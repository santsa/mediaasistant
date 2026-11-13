# Media Assistant

Media Assistant is a Spring Boot REST API for a medical-assistance prototype. It uses Spring AI with Google Gemini or Ollama for conversational and structured medical-information responses. The assistant can also look up doctors, appointments, patient information, and drug information, and an administrator can book appointments through the chat tools.
It also provides specialized agent workflows for query routing, appointment booking, and multi-worker symptom analysis, answers questions using medical documents indexed in a vector store (RAG), and exposes its own MCP server with resources and prompts.

The application runs in two modes:

- **Development** (`local` / `remote` profiles): Gemini and Ollama, Chroma vector store, and MCP client tools (filesystem and GitHub).
- **Production on AWS** (`aws` profile): a Docker image running on EC2, Gemini only, PostgreSQL on Amazon RDS with the pgvector extension as the vector store, and Keycloak running next to the app.

> **Important:** This is a prototype, not a medical device or a substitute for professional medical advice, diagnosis, or treatment. AI-generated information may be inaccurate. Do not use it for emergencies or make clinical decisions based on its responses.

## Technology

- Java 25 (LTS) and Spring Boot 4
- Spring AI 2 with Google Gemini and Ollama chat clients
- PostgreSQL 17 with Spring Data JPA
- Vector stores: Chroma (development) and pgvector (AWS)
- Apache Tika for reading PDF and other documents
- MCP client (STDIO) and MCP server (WebMVC, stateless)
- Keycloak JWT bearer-token authentication
- OpenFDA drug information lookup
- Docker (multi-stage image), Amazon ECR, EC2 and RDS, provisioned with Terraform

## Spring profiles

| Profile | Use | Chat models | Embeddings / vector store |
| --- | --- | --- | --- |
| `remote` | Codespaces or remote development | Gemini, Ollama Cloud | Gemini embeddings for both chat clients, Chroma |
| `local` | Local machine | Gemini, local Ollama (`http://localhost:11434`) | Gemini embeddings, plus a separate collection with local Ollama embeddings, Chroma |
| `aws` | Production Docker image | Gemini only | Gemini embeddings (`gemini-embedding-001`, 3072 dimensions), pgvector table `medassistant_google` |

`gemini` and `ollama` contain the model settings for each provider. Profiles are activated with `SPRING_PROFILES_ACTIVE` or under `spring.profiles.include` in `application.yaml`. The Docker image always sets `SPRING_PROFILES_ACTIVE=aws`.

## Prerequisites

- JDK 25
- Docker and Docker Compose
- A Google Gemini API key; an Ollama API key is also required when using the remote Ollama endpoint
- A Keycloak realm and client that issue tokens accepted by this API (the realm can be imported from `keycloak_realm/medassistant-realm.json`)
- For AWS deployment: the AWS CLI, an ECR repository, and Terraform

Both AI providers are configured in the development profiles. The Google Gemini API key is used for embeddings in the `remote` profile, so it is required in either development profile. Remote Ollama chat also requires an Ollama API key. Set the request's `model` to `ollama` to use Ollama chat. The `local` profile does not require an Ollama API key.

## Run locally

1. Start PostgreSQL, Keycloak, and Chroma:

   ```sh
   docker compose up -d postgres-app keycloak chroma
   ```

   Compose exposes PostgreSQL on port `5432`, Keycloak on port `8090`, and Chroma on port `8000`. The compose file uses development credentials (`postgres`/`postgres` and `admin`/`admin`); do not use these defaults outside local development.

2. Configure a Keycloak realm named `medassistant` and a client named `medassistant-client`, or import `keycloak_realm/medassistant-realm.json`. Configure the API's JWT issuer URI to exactly match the `iss` claim in the tokens Keycloak issues. The default issuer in `src/main/resources/application.yaml` points to a Codespaces URL; for a local Keycloak issuer, set:

   ```sh
   export SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://localhost:8090/realms/medassistant
   ```

   Tokens must contain a numeric `userId` claim for chat operations and a `PATIENT` or `ADMIN` role, either as a realm role or a role from `medassistant-client`. All `/api/v1/**` routes require one of those roles.

3. Set the AI provider credentials:

   ```sh
   export GOOGLE_GENAI_API_KEY=your-google-genai-api-key
   ```

   For the `remote` profile, also set `OLLAMA_API_KEY` for Ollama Cloud. For local mode, make sure Ollama is running and has the configured chat and embedding models available.

4. Run the application with a development profile:

   ```sh
   SPRING_PROFILES_ACTIVE=remote ./mvnw spring-boot:run
   # or
   SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
   ```

   The API listens on port `8080` by default. To build and run the tests:

   ```sh
   ./mvnw verify
   ```

## MCP client tools — Filesystem and GitHub

> These servers are intended for local development over STDIO and are disabled in the `aws` profile. They require **Node.js ≥ 18** (run `node --version` to check).

### 1. Filesystem MCP Server

Allows the model to list and read files in a local directory.

```sh
npm install -g @modelcontextprotocol/server-filesystem@2025.3.28
which mcp-server-filesystem      # Windows: where mcp-server-filesystem
```

### 2. GitHub MCP Server

Allows the model to search repositories, read files, and list GitHub issues.

```sh
npm install -g @modelcontextprotocol/server-github@2025.3.28
which mcp-server-github          # Windows: where mcp-server-github
```

> npm may display a warning about version `2025.3.28`: the package no longer publishes date-based versions and recommends installing the latest semantic version. The warning does not necessarily mean the installation failed; check that `which` or `where` can find the executable.

Set `MCP_GITHUB_TOKEN` in the environment before starting the application; `application.yaml` reads this variable for the GitHub MCP server. Do not commit the token.

The MCP server commands and arguments are configured under `spring.ai.mcp.client.stdio.connections` in `src/main/resources/application.yaml`. Replace the example absolute executable paths with the paths on your machine. The filesystem server's argument controls which directory it can access; keep it limited to files you intend to expose to the model.

## MCP server

The application also acts as an MCP server (`medassistant-mcp`, stateless WebMVC transport) in every profile, including `aws`. MCP routes under `/mcp/**` do not require authentication. It exposes:

| Type | Name | Description |
| --- | --- | --- |
| Resource | `doctor://{speciality}` | List of available doctors for a medical speciality. |
| Prompt | `symptom-consultation` | Builds a structured message to ask for educational guidance on a symptom. Argument: `symptom` (required). |

Because the MCP endpoints are public and the doctor resource reads from the database, review this before exposing the server to the internet.

## Medical documents (RAG)

On startup, the application indexes the PDFs in `src/main/resources/docs/` (ibuprofen pharmacology, cardiovascular care protocol, hypertension guide) into the vector store, unless documents are already present. The chat client adds a `QuestionAnswerAdvisor` that retrieves the 3 most relevant chunks (similarity ≥ 0.7) and adds them to the prompt. Administrators can index new documents with `/api/v1/documents/ingest`.

In the `aws` profile, the vector store is the `medassistant_google` table in PostgreSQL. `schema-aws.sql` runs on startup and creates the `vector` extension, so the database user must be allowed to create it (on RDS, the master user can).

## Docker and AWS deployment

### Build the image

The `Dockerfile` uses a multi-stage build: Maven with JDK 25 compiles the JAR, and the runtime image contains only JRE 25. The container runs as a non-root user, uses the `Europe/Madrid` timezone, sets `SPRING_PROFILES_ACTIVE=aws`, and limits the heap to 75 % of the container's memory.

```sh
docker build -t medassistant:aws .
```

### Environment variables for the `aws` profile

| Variable | Description |
| --- | --- |
| `RDS_HOST`, `RDS_PORT`, `RDS_DB` | PostgreSQL / RDS connection (defaults: `localhost`, `5432`, `medassistant`). |
| `RDS_USERNAME`, `RDS_PASSWORD` | Database credentials. |
| `GOOGLE_GENAI_API_KEY` | Gemini API key for chat and embeddings. |
| `KEYCLOAK_ISSUER_URI` | Issuer of the JWT tokens, for example `http://<host>:8090/realms/medassistant`. |

In the `aws` profile only the `gemini` model is supported; a request with `"model": "ollama"` returns an error. The MCP client and the `/api/v1/mcp/demo` endpoint are disabled.

### Test the AWS image locally

`docker-compose.aws-test.yml` starts PostgreSQL, Keycloak, and a pgvector database on port `5434` to simulate RDS:

```sh
docker compose -f docker-compose.aws-test.yml up -d
docker build -t medassistant:aws .

docker run -d --name medassistant \
  --add-host=host.docker.internal:host-gateway \
  -p 8080:8080 \
  -e RDS_HOST=host.docker.internal \
  -e RDS_PORT=5434 \
  -e RDS_DB=medassistant_prod \
  -e RDS_USERNAME=postgres \
  -e RDS_PASSWORD=postgres \
  -e KEYCLOAK_ISSUER_URI=http://host.docker.internal:8090/realms/medassistant \
  -e GOOGLE_GENAI_API_KEY \
  medassistant:aws

docker logs -f medassistant
```

### Publish to Amazon ECR

```sh
aws configure
aws sts get-caller-identity

aws ecr get-login-password --region <region> \
  | docker login --username AWS --password-stdin <account-id>.dkr.ecr.<region>.amazonaws.com

docker tag medassistant:aws <account-id>.dkr.ecr.<region>.amazonaws.com/medassistant:latest
docker push <account-id>.dkr.ecr.<region>.amazonaws.com/medassistant:latest
```

### Infrastructure

The infrastructure is defined with Terraform in the sibling `terraform` project:

- **EC2** (`t3.small`) with an Elastic IP, an IAM role with read-only ECR access, and a user-data script that installs Docker, the Docker Compose plugin, and 2 GB of swap.
- **RDS PostgreSQL 17** for application data, chat memory, and pgvector embeddings.
- **Security groups** for the EC2 instance and RDS.

Terraform outputs the RDS endpoint and JDBC URL, the EC2 public IP and DNS, and the SSH command.

### Run on EC2

`docker-compose-aws.yml` starts Keycloak (with its own PostgreSQL database and the realm imported from `keycloak_realm/medassistant-realm.json`) and the application image from ECR. Copy the compose file, the `keycloak_realm/` folder, and an `.env` file to the instance with these variables:

```sh
MEDASSISTANT_IMAGE=<account-id>.dkr.ecr.<region>.amazonaws.com/medassistant:latest
RDS_HOST=...
RDS_PORT=5432
RDS_DB=...
RDS_USERNAME=...
RDS_PASSWORD=...
GOOGLE_GENAI_API_KEY=...
KEYCLOAK_ADMIN=...
KEYCLOAK_ADMIN_PASSWORD=...
KEYCLOAK_ISSUER_URI=http://<ec2-public-ip>:8090/realms/medassistant
```

Then, on the instance:

```sh
aws ecr get-login-password --region <region> \
  | docker login --username AWS --password-stdin <account-id>.dkr.ecr.<region>.amazonaws.com
docker compose -f docker-compose-aws.yml --env-file .env up -d
```

The application listens on port `8080` and Keycloak on port `8090`. Keycloak runs in `start-dev` mode over HTTP; use HTTPS and a production Keycloak setup before handling real data. Never commit `.env` files with credentials.

## API

All endpoints are under `/api/v1` and require a valid access token in the `Authorization` header. Request bodies use JSON with a required `prompt` and optional `model` (`gemini` or `ollama`; only `gemini` in the `aws` profile):

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

Example:

```sh
curl http://localhost:8080/api/v1/analysis/condition \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"prompt":"asthma","model":"gemini"}'
```

### Agent workflows

Agent endpoints require the same authenticated request body as chat. The user's numeric `userId` is read from the access token.

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/agent/open` | Answer a query using tools to look up doctors, appointments, patient information, and drug information. |
| `POST` | `/api/v1/agent/routing` | Classify a query and route it to a workflow for symptoms, emergencies, general health questions, prescription requests, or off-topic requests. |
| `POST` | `/api/v1/agent/chain/appointment` | Extract appointment preferences, find available appointments, and return a suggested appointment for confirmation. |
| `POST` | `/api/v1/agent/chain/appointment/confirm` | Confirm and book the appointment pending for the authenticated user, after checking availability again. No request body. |
| `POST` | `/api/v1/agent/chain/appointment/cancel` | Cancel the authenticated user's pending appointment selection. No request body. |
| `POST` | `/api/v1/agent/orchestrator` | Analyze symptoms by combining clinical analysis, relevant medical-document search, and the patient's history. |
| `POST` | `/api/v1/agent/orchestrator/progress` | Run the same analysis and stream progress updates followed by the final response as server-sent events. |

The appointment chain does not book the suggested slot until `/confirm` is called. Pending selections are held in application memory and are cleared if the application restarts.

### Document ingestion

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/documents/ingest` | Upload a document as multipart form data using the `file` field and index its contents in the configured vector store. Requires the `ADMIN` role. Returns the file name and the number of indexed fragments. |

```sh
curl http://localhost:8080/api/v1/documents/ingest \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -F "file=@./medical-guideline.pdf"
```

### MCP tools demo (development only)

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/mcp/demo` | Send a prompt to the selected chat model with the configured MCP client tools available. Not available in the `aws` profile. |

`McpDemoController` resolves the requested model, passes the prompt and the injected `ToolCallbackProvider` to Spring AI's `ChatClient`, and returns the model's response. The provider supplies the tool callbacks discovered from the configured MCP servers (filesystem and GitHub), and the model may call those tools while producing its response.

```sh
curl http://localhost:8080/api/v1/mcp/demo \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"prompt":"List the available files","model":"gemini"}'
```

Treat the filesystem directory and GitHub token as sensitive: the model can use the configured tools to access the resources they grant.

## Development notes

- The application uses `jdbc:postgresql://localhost:5432/mediaassistant` by default in development; in the `aws` profile the URL is built from the `RDS_*` variables.
- Hibernate is configured with `ddl-auto: update`, so application restarts preserve the existing tables and their data. Docker Compose stores PostgreSQL and Chroma data in `./postgres_data` and `./chroma_data`; these directories survive container recreation and `docker compose down`.
- On an empty database, the application seeds sample doctors, patients, and appointments. Each sample group is seeded only when its table is empty. Do not treat this sample data as real patient information.
- Gemini and Ollama model settings are in `application-gemini.yaml` and `application-ollama.yaml`. `application-remote.yaml` uses Gemini embeddings for both vector stores and indexes documents once; `application-local.yaml` additionally indexes an Ollama-specific collection using local Ollama embeddings. `application-aws.yaml` disables Chroma, Ollama, and the MCP client, and uses `gemini-3.1-flash-lite` for chat.
- Configuration is split by profile with `ClientResolverDefault` / `ClientResolverAws`, `AssistantConfig` / `AssistantConfigAws`, and `DocumentIngestionServiceDefaultImpl` / `DocumentIngestionServiceAwsImpl`.
