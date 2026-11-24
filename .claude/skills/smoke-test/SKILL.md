---
name: smoke-test
description: Runs an end-to-end smoke test of the running API — starts the local dependencies if needed, gets real JWTs from Keycloak for a PATIENT and an ADMIN user, calls the main /api/v1 endpoints with curl and reports what works and what fails, including role checks. Use it when the user asks to smoke test, try the API, check endpoints end to end, or verify a change in the real running app.
arguments: [scope, host]
argument-hint: [chat | agent | analysis | documents | security | all] [host (default http://localhost:8080)]
---

# Smoke test — mediaassistant

Scope: `$scope` (default `all`) · API host: `$host` (default `http://localhost:8080`).

Exercise the real running application. This calls real LLMs (Gemini / Ollama), so keep prompts short and don't loop.

## 1. Make sure the app is reachable

```sh
curl -s -o /dev/null -w "%{http_code}\n" "$HOST/api/v1/chat" -X POST
```

- `401` → app up, security on. Continue.
- Connection refused on localhost → offer to start it (ask first, it is long-running):
  1. `docker compose up -d postgres-app keycloak chroma`
  2. Required env: `GOOGLE_GENAI_API_KEY` (all profiles), `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://localhost:8090/realms/medassistant`, plus `OLLAMA_API_KEY` for `remote`. If a variable is missing, ask the user to export it — never invent or print keys.
  3. `SPRING_PROFILES_ACTIVE=<remote|local> ./mvnw spring-boot:run` in the background (needs JDK 25; otherwise run the `medassistant:aws`-style image or the Maven container). Note: the MCP client STDIO commands in `application.yaml` point to Codespaces paths and can break startup outside Codespaces — if startup fails there, report it.
  4. Wait until the log shows `Started MediaassistantApplication`.
- Remote host (e.g. the EC2 IP) → never start anything; only test.

## 2. Get tokens

Keycloak realm `medassistant`, confidential client `medassistant-client` (direct access grants enabled). Keycloak host: `http://localhost:8090` locally, or `http://<host>:8090` for EC2.

Users in `keycloak_realm/medassistant-realm.json`:

| User | Role | `userId` claim |
| --- | --- | --- |
| `gabriel` | PATIENT | 1 |
| `maria` | PATIENT | 2 |
| `admin-reception` | ADMIN | 99 |

The client secret and user passwords are not in this skill. Take them from env vars `KC_CLIENT_SECRET`, `KC_PATIENT_PASSWORD`, `KC_ADMIN_PASSWORD`; if unset, ask the user. Never echo them or write them to files.

```sh
token() { curl -s "$KC/realms/medassistant/protocol/openid-connect/token" \
  -d grant_type=password -d client_id=medassistant-client \
  --data-urlencode "client_secret=$KC_CLIENT_SECRET" \
  --data-urlencode "username=$1" --data-urlencode "password=$2" \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["access_token"])'; }
PATIENT_TOKEN=$(token gabriel "$KC_PATIENT_PASSWORD")
ADMIN_TOKEN=$(token admin-reception "$KC_ADMIN_PASSWORD")
```

Decode the payload (`cut -d. -f2 | base64 -d`) and confirm `iss` matches the issuer the app is configured with, `userId` is present and the role is in `resource_access.medassistant-client.roles`. An `iss` mismatch is the #1 cause of 401s — report it clearly.

## 3. Run the checks for the scope

Body for every call: `{"prompt":"<short prompt>","model":"gemini"}` with `-H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json"`. Record HTTP status, time (`-w "%{http_code} %{time_total}s"`) and the first ~200 chars of the body.

| Scope | Endpoints |
| --- | --- |
| `chat` | `POST /api/v1/chat`, `/stream` (use `curl -N`, stop after a few events), `/explain`, `/symptoms`, `/diagnose`, `/consult` |
| `analysis` | `POST /api/v1/analysis/condition`, `/conditions`, `/classify` |
| `agent` | `POST /api/v1/agent/routing`, `/orchestrator`, `/orchestrator/progress` (SSE), `/open`, `/chain/appointment` (only propose — **don't call `/confirm`** unless the user says so: it books) |
| `documents` | `POST /api/v1/documents/ingest` — **ask first**: it re-indexes the vector store |
| `security` | no token → 401; garbage token → 401; PATIENT asking via `/chat` to book an appointment → the booking tool must refuse; ADMIN same request → allowed to proceed (stop before final confirmation); `/mcp` public routes reachable without a token |
| `all` | `chat` + `analysis` + `agent` + `security` (not `documents`) |

Use `gemini` by default; also try `"model":"ollama"` once on `/chat` in non-aws runs (in `aws` it must be rejected — that is a pass).

## 4. Report

A table: endpoint · role · status · time · verdict (✅/❌) · note. Then failures with the exact response body or the relevant app log lines (`docker logs` or the spring-boot:run output). Separate "app bug" from "environment issue" (missing key, issuer mismatch, LLM quota). Don't claim a pass you didn't observe.

If you started the app or containers, ask whether to stop them.
