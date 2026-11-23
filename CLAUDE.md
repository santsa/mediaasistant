# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Spring Boot 4 / Spring AI 2 REST API (Java 25) for a medical-assistant prototype. Chat with Gemini or Ollama, tool-calling over a PostgreSQL domain (doctors, patients, appointments) plus OpenFDA drug lookup, RAG over PDFs, agent workflows, an MCP client (dev only) and an MCP server. `README.md` has the full endpoint list, env vars and AWS deployment steps.

## Commands

```sh
# Dev dependencies (PostgreSQL :5432, Keycloak :8090, Chroma :8000)
docker compose up -d postgres-app keycloak chroma

# Run (a profile is required: remote = Codespaces/Ollama Cloud, local = local Ollama)
export GOOGLE_GENAI_API_KEY=...            # needed in every profile (embeddings)
export SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://localhost:8090/realms/medassistant
SPRING_PROFILES_ACTIVE=remote ./mvnw spring-boot:run   # remote also needs OLLAMA_API_KEY

./mvnw verify                 # build (+ tests)
./mvnw -Dtest=ClassName test  # single test class (no src/test exists yet)

# AWS image and local simulation of RDS (pgvector on :5434)
docker build -t medassistant:aws .
docker compose -f docker-compose.aws-test.yml up -d
```

The default issuer URI in `application.yaml` points to a Codespaces URL, so it must be overridden for local runs. MCP client STDIO commands in `application.yaml` use hard-coded Codespaces paths (`/home/codespace/nvm/...`); adjust them or startup of the MCP client fails in non-aws profiles.

## Architecture

### Profile split (the most important thing to understand)

There are two parallel bean graphs selected by `@Profile("aws")` vs `@Profile("!aws")`. When changing chat client, vector store or ingestion behavior, update **both** sides:

| Concern | dev (`!aws`) | `aws` |
| --- | --- | --- |
| Chat clients + vector stores | `config/AssistantConfig` (Gemini + Ollama clients, Chroma) | `config/AssistantConfigAws` (Gemini only, PgVectorStore `medassistant_google`, 3072 dims) |
| Model selection | `config/ClientResolverDefault` | `config/ClientResolverAws` (rejects anything but `gemini`) |
| Document ingestion | `service/DocumentIngestionServiceDefaultImpl` | `service/DocumentIngestionServiceAwsImpl` |

`AssistantConfig` and `AssistantConfigAws` duplicate the ChatClient construction (system prompt from `prompts/system-prompt.st` with `{currentDate}` substituted, the five `@Tool` beans, `MedicalAuditAdvisor`, and a `QuestionAnswerAdvisor` topK 3 / threshold 0.7). Keep them in sync.

Within dev: `local` gives Ollama its own Chroma collection (`medassistant_ollama`, Ollama embeddings); otherwise `ollamaVectorStore` is an alias of the Google store. `ollama & !local` adds a Bearer-token RestClient/WebClient customizer for Ollama Cloud. Chroma autoconfiguration is excluded globally; Chroma beans are built manually. Provider model settings live in `application-gemini.yaml` / `application-ollama.yaml`; `assistant.ingestion.*-enabled` flags in the profile yamls control startup indexing.

### Request flow

Controllers take a `ChatRequest` (`prompt`, optional `model`, defaults to `gemini`) and extract `userId` (numeric JWT claim) and role (`SecurityUtils.extractRole`) from the JWT. Services call `ClientResolver.resolve(model)` and pass `toolContext(Map.of("userId", ..., "role", ...))`. Tools in `tools/` read that context for authorization — e.g. `AppointmentBookingTool` refuses unless `role == ADMIN`. Chat memory is JDBC-backed (`MessageWindowChatMemory`, 10 messages) keyed by `userId` as the conversation id.

Prompts are StringTemplate files in `src/main/resources/prompts/*.st`, loaded via `@Value("classpath:...")` into `PromptTemplate` in `@PostConstruct`.

### Agents (`agent/`)

- `RoutingWorkflowServiceImpl`: classifies into `QueryType` (structured output) and dispatches to a `routing-*.st` prompt.
- `AppointmentChainServiceImpl`: extract preferences → search slots → propose; `/confirm` re-checks availability and books. Pending selections live in the in-memory `PendingBookingStore` (lost on restart).
- `orchestrator/`: `OrchestratorServiceImpl` runs `ClinicalWorker`, `BibliographyWorker` (vector store search) and `HistoryWorker` (patient data) and synthesizes with `orchestrator-synthesis.st`; a `/progress` variant streams SSE.
- `OpenAgentServiceImpl`: free-form tool-using agent.

### Security

`SecurityConfig`: stateless JWT resource server; roles come from both `realm_access.roles` and `resource_access.medassistant-client.roles`, prefixed `ROLE_`. `/api/v1/**` requires `PATIENT` or `ADMIN`; `/mcp/**` (MCP server: `mcp/DoctorResourceProvider`, `mcp/SymptomConsultationPrompProvider`) is public. Keycloak realm export: `keycloak_realm/medassistant-realm.json`.

### Data

JPA with `ddl-auto: update`; `config/DataLoader` seeds sample doctors/patients/appointments per empty table and indexes `src/main/resources/docs/*.pdf` (Tika) if the vector store is empty. In `aws`, `schema-aws.sql` creates the `vector` extension at startup.

## Terraform (`terraform/`)

Resources (EC2 + EIP, RDS PostgreSQL 17, SGs, IAM role for ECR read) were created in the AWS console and imported. `*_generated.tf` files are produced by `generate.sh` (runs `terraform plan -generate-config-out` and splits output by resource type). Flow per `import.tf`: add an `import` block → `./generate.sh` → `terraform apply` → remove the import block. Copy `terraform.tfvars.example` for variables.

## Misc

- `TestChatClient` / `TestLlmCall` in the main package are disabled scratch runners (no `@Component`), not tests.
- `.env_local` / `.env_aws` hold environment values for compose; `docker-compose-aws.yml` is what runs on EC2 (Keycloak + app image from ECR).
