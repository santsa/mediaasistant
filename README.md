# Media Assistant

Media Assistant is a Spring Boot REST API for a medical-assistance prototype. It uses Spring AI with Google Gemini or Ollama for conversational and structured medical-information responses. The assistant can also look up doctors, appointments, patient information, and drug information, and an administrator can book appointments through the chat tools.
It also provides specialized agent workflows for query routing, appointment booking, and multi-worker symptom analysis.

> **Important:** This is a prototype, not a medical device or a substitute for professional medical advice, diagnosis, or treatment. AI-generated information may be inaccurate. Do not use it for emergencies or make clinical decisions based on its responses.

## Technology

- Java 25 (LTS) and Spring Boot 4
- Spring AI 2 with Google Gemini and Ollama chat clients
- PostgreSQL 17 with Spring Data JPA
- Keycloak JWT bearer-token authentication
- OpenFDA drug information lookup

## Prerequisites

- JDK 25
- Docker and Docker Compose
- A Google Gemini API key; an Ollama API key is also required when using the remote Ollama endpoint
- A configured Keycloak realm and client that issue tokens accepted by this API

Both AI providers are configured by default. The Google Gemini API key is used for embeddings in the default `remote` profile, so it is required in either profile. Remote Ollama chat also requires an Ollama API key. The default `remote` profile uses Gemini embeddings for both chat clients, avoiding a dependency on a reachable Ollama embedding endpoint in remote environments. Set the request's `model` to `ollama` to use Ollama chat. The `local` profile points Ollama to `http://localhost:11434`, uses local Ollama embeddings for Ollama chat, and does not require an Ollama API key.

## Installing MCP Servers — Filesystem and GitHub

> These servers are intended for local development over STDIO and are not deployed to production. They require **Node.js ≥ 18** (run `node --version` to check).

### 1. Filesystem MCP Server

Allows the model to list and read files in a local directory.

**macOS / Linux:**

```sh
npm install -g @modelcontextprotocol/server-filesystem@2025.3.28
which mcp-server-filesystem
```

**Windows (PowerShell):**

```powershell
npm install -g @modelcontextprotocol/server-filesystem@2025.3.28
where mcp-server-filesystem
```

Copy the returned path to use as `command` in the MCP configuration in `application.yaml`.

### 2. GitHub MCP Server

Allows the model to search repositories, read files, and list GitHub issues.

**macOS / Linux:**

```sh
npm install -g @modelcontextprotocol/server-github@2025.3.28
which mcp-server-github
```

**Windows (PowerShell):**

```powershell
npm install -g @modelcontextprotocol/server-github@2025.3.28
where mcp-server-github
```

> npm may display a warning about version `2025.3.28`: the package no longer publishes date-based versions and recommends installing the latest semantic version. The warning does not necessarily mean the installation failed; check that `which` or `where` can find the executable.
Set `MCP_GITHUB_TOKEN` in the environment before starting the application; `application.yaml` reads this variable for the GitHub MCP server. Do not commit the token to `application.yaml`.

The MCP server commands and arguments are configured under `spring.ai.mcp.client.stdio.connections` in `src/main/resources/application.yaml`. Replace the example absolute executable paths with the paths on your machine. The filesystem server's argument controls which directory it can access; keep it limited to files you intend to expose to the model.

## Run locally

1. Start PostgreSQL and Keycloak:

   ```sh
   docker compose up -d postgres-app keycloak
   ```

   Compose exposes PostgreSQL on port `5432` and Keycloak on port `8090`. The compose file uses development credentials (`postgres`/`postgres` and `admin`/`admin`); do not use these defaults outside local development.

2. Configure a Keycloak realm named `medassistant` and a client named `medassistant-client`. Configure the API's JWT issuer URI to exactly match the `iss` claim in the tokens Keycloak issues. The default issuer in `src/main/resources/application.yaml` points to a Codespaces URL; for a local Keycloak issuer, set the environment variable below if it matches your realm's issuer:

   ```sh
   export SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://localhost:8090/realms/medassistant
   ```

   Tokens must contain a numeric `userId` claim for chat operations and a `PATIENT` or `ADMIN` role, either as a realm role or a role from `medassistant-client`. All `/api/v1/**` routes require one of those roles.

3. Set the AI provider credentials:

   ```sh
   export GOOGLE_GENAI_API_KEY=your-google-genai-api-key
   ```

   For the default remote profile, also set `OLLAMA_API_KEY` for Ollama Cloud. For local mode, make sure Ollama is running and has the configured chat and embedding models available.

4. Run the application:

   ```sh
   ./mvnw spring-boot:run
   ```

   The default profile is `remote`. To use local Ollama embeddings, activate the `local` profile:

   ```sh
   SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
   ```

   The API listens on port `8080` by default. To build and run the tests:

   ```sh
   ./mvnw verify
   ```

## API

All endpoints are under `/api/v1` and require a valid access token in the `Authorization` header. Request bodies use JSON with a required `prompt` and optional `model` (`gemini` or `ollama`):

```json
{
  "prompt": "Find a cardiologist",
  "model": "gemini"
}
```

### Chat and consultation

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/chat` | Chat with the assistant. Chat tools can search doctors, appointments, patient information, and drug information; appointment booking is restricted to administrators. |
| `POST` | `/api/v1/stream` | Stream chat responses as server-sent events. |
| `POST` | `/api/v1/explain` | Explain a medical condition. |
| `POST` | `/api/v1/symptoms` | Analyze symptoms in a conversational response. |
| `POST` | `/api/v1/diagnose` | Request a symptom assessment with reasoning. |
| `POST` | `/api/v1/consult` | Get a general consultation-style response. |

### Structured analysis

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/analysis/condition` | Return a structured summary of a condition. |
| `POST` | `/api/v1/analysis/conditions` | List conditions related to a prompt. |
| `POST` | `/api/v1/analysis/symptoms` | Return structured symptom analysis. |
| `POST` | `/api/v1/analysis/classify` | Classify a query. |

### Agent workflows

Agent endpoints require the same authenticated request body as chat: a non-empty `prompt` and an optional `model` (`gemini` or `ollama`). The user's numeric `userId` is read from the access token.

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/agent/open` | Answer a query using tools to look up doctors, appointments, patient information, and drug information. |
| `POST` | `/api/v1/agent/routing` | Classify a query and route it to a workflow for symptoms, emergencies, general health questions, prescription requests, or off-topic requests. |
| `POST` | `/api/v1/agent/chain/appointment` | Extract appointment preferences, find available appointments, and return a suggested appointment for confirmation. |
| `POST` | `/api/v1/agent/chain/appointment/confirm` | Confirm and book the appointment pending for the authenticated user, after checking availability again. This endpoint does not require a request body. |
| `POST` | `/api/v1/agent/chain/appointment/cancel` | Cancel the authenticated user's pending appointment selection. This endpoint does not require a request body. |
| `POST` | `/api/v1/agent/orchestrator` | Analyze symptoms by combining clinical analysis, relevant medical-document search, and the patient's history. |
| `POST` | `/api/v1/agent/orchestrator/progress` | Run the same analysis and stream progress updates followed by the final response as server-sent events. |

The appointment chain does not book the suggested slot until `/confirm` is called. Pending selections are held in application memory and are cleared if the application restarts.

### MCP tools demo

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/mcp/demo` | Send a prompt to the selected chat model with the configured MCP tools available. |

`McpDemoController` resolves the requested model, passes the prompt and the injected `ToolCallbackProvider` to Spring AI's `ChatClient`, and returns the model's response. The provider supplies the tool callbacks discovered from the configured MCP servers (for example, filesystem and GitHub). The model may call those tools while producing its response. `ToolCallbackProvider` is a Spring AI interface; its `from(...)` factory methods create a provider from callbacks directly, while this controller uses the provider supplied by Spring's dependency injection.

The request uses the standard chat body and authentication described above. For example:

```sh
curl http://localhost:8080/api/v1/mcp/demo \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"prompt":"List the available files","model":"gemini"}'
```

The available tools depend on which MCP servers are configured and running. Treat the filesystem directory and GitHub token as sensitive: the model can use the configured tools to access the resources they grant.

### Document ingestion

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/documents/ingest` | Upload a document as multipart form data using the `file` field and index its contents in the configured vector store. Requires the `ADMIN` role. |

Example:

```sh
curl http://localhost:8080/api/v1/documents/ingest \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -F "file=@./medical-guideline.pdf"
```

Example:

```sh
curl http://localhost:8080/api/v1/analysis/condition \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"prompt":"asthma","model":"gemini"}'
```

## Development notes

- The application uses `jdbc:postgresql://localhost:5432/mediaassistant` by default; configure the datasource properties if your database differs.
- Hibernate is configured with `ddl-auto: update`, so application restarts preserve the existing tables and their data. Docker Compose stores PostgreSQL and Chroma data in `./postgres_data` and `./chroma_data`; these directories survive container recreation and `docker compose down`. Keep them to retain the data.
- On an empty database, the application seeds sample doctors, patients, and appointments. Each sample group is seeded only when its table is empty. Do not treat this sample data as real patient information.
- Gemini and Ollama model settings are in `src/main/resources/application-gemini.yaml` and `src/main/resources/application-ollama.yaml`. `application-remote.yaml` uses Gemini embeddings for both vector stores and indexes documents once; `application-local.yaml` additionally indexes an Ollama-specific collection using local Ollama embeddings. Chat requests use Gemini unless `model` is set to `ollama`.
