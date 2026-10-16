# Media Assistant

Media Assistant is a Spring Boot REST API for a medical-assistance prototype. It uses Spring AI with Google Gemini or Ollama for conversational and structured medical-information responses. The assistant can also look up doctors, appointments, patient information, and drug information, and an administrator can book appointments through the chat tools.

> **Important:** This is a prototype, not a medical device or a substitute for professional medical advice, diagnosis, or treatment. AI-generated information may be inaccurate. Do not use it for emergencies or make clinical decisions based on its responses.

## Technology

- Java 21 and Spring Boot 4
- Spring AI 2 with Google Gemini and Ollama chat clients
- PostgreSQL 17 with Spring Data JPA
- Keycloak JWT bearer-token authentication
- OpenFDA drug information lookup

## Prerequisites

- JDK 21
- Docker and Docker Compose
- API keys for both Google Gemini and Ollama
- A configured Keycloak realm and client that issue tokens accepted by this API

Both AI profiles are included by default, so both provider credentials must be set for the application to start. By default, requests use Gemini; set the request's `model` to `ollama` to use Ollama.

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
   export OLLAMA_API_KEY=your-ollama-api-key
   ```

4. Run the application:

   ```sh
   ./mvnw spring-boot:run
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

Example:

```sh
curl http://localhost:8080/api/v1/analysis/condition \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"prompt":"asthma","model":"gemini"}'
```

## Development notes

- The application uses `jdbc:postgresql://localhost:5432/mediaassistant` by default; configure the datasource properties if your database differs.
- Hibernate is configured with `ddl-auto: create-drop`. Database tables are recreated when the application starts and dropped when it stops, so this configuration is for disposable development data only.
- On startup, the application seeds sample doctors, patients, and appointments. Do not treat this sample data as real patient information.
- Gemini and Ollama model settings are in `src/main/resources/application-gemini.yaml` and `src/main/resources/application-ollama.yaml`. Chat requests use Gemini unless `model` is set to `ollama`.
