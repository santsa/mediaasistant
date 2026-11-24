---
name: refactor
description: Refactors and optimizes code in this Spring Boot 4 / Spring AI 2 project without changing its behavior — removes duplication, improves structure and naming, and fixes performance issues (JPA queries, prompt loading, HTTP clients, streaming). Use it when the user asks to refactor, clean up, restructure, simplify or optimize a class, package or feature.
arguments: [target]
argument-hint: <ClassName | path | package> [focus: refactor | performance | all]
---

# Refactor — mediaassistant

Target: `$target` (full arguments: `$ARGUMENTS`).

Refactor and/or optimize the target (class name, path, package or feature). An optional focus word may follow: `refactor` (structure only), `performance` (optimization only) or `all` (default). If the target is empty, take it from the user's request, or ask what to work on.

**Golden rule: behavior is preserved.** Same endpoints, same responses, same authorization rules, same profile behavior. Anything that changes behavior is a proposal for the user, not part of the refactor.

## Workflow

1. **Read** the target fully, plus its callers and collaborators (`grep` for usages) — a refactor that breaks a caller is not done.
2. **Diagnose.** List concrete findings, each with `file:line`, the problem and the proposed change. Classify them:
   - *Safe & local* (rename private member, extract method, remove dead code inside the class).
   - *Structural* (changes a public signature, moves/merges classes, touches both profile sides, adds a dependency).
   - *Behavior-changing* (anything that alters output, errors, auth or config) → only report, never apply without approval.
3. **Present the plan** to the user and wait for approval before applying *structural* changes. *Safe & local* changes can be applied directly when the user asked for a refactor.
4. **Safety net first.** `src/test` may have little or no coverage. Before changing logic, make sure the affected behavior is covered by unit tests; if it isn't, write them first with the `unit-test` skill conventions and see them pass on the *original* code.
5. **Apply in small steps**, one finding at a time, keeping the code compiling between steps.
6. **Verify** (see **Build & verify**): compile, run the tests, and report the real result.
7. **Summarize**: what changed and why, what was deliberately left out, and any behavior-changing proposals still pending.

## Project-specific rules

### Profile split — keep both sides in sync
Beans exist twice, selected by `@Profile("aws")` vs `@Profile("!aws")`:

| Concern | dev (`!aws`) | `aws` |
| --- | --- | --- |
| Chat clients + vector stores | `config/AssistantConfig` | `config/AssistantConfigAws` |
| Model selection | `config/ClientResolverDefault` | `config/ClientResolverAws` |
| Document ingestion | `service/DocumentIngestionServiceDefaultImpl` | `service/DocumentIngestionServiceAwsImpl` |

- Any change to one side must be mirrored on the other, or explicitly justified.
- The `ChatClient` construction (system prompt from `prompts/system-prompt.st` with `{currentDate}`, the five `@Tool` beans, `MedicalAuditAdvisor`, `QuestionAnswerAdvisor` topK 3 / threshold 0.7) is duplicated between `AssistantConfig` and `AssistantConfigAws`. Extracting it into a shared, profile-neutral helper is a good candidate — but the result must produce identical clients in both profiles.
- Don't collapse the profile split into `if (profile == ...)` logic; keep bean selection declarative.

### Spring AI
- Load prompt templates once (`@PostConstruct` / constructor), never per request. Prompts stay in `src/main/resources/prompts/*.st`; don't inline them into Java.
- Keep `toolContext(Map.of("userId", ..., "role", ...))` on every call that can reach a tool — tools rely on it for authorization (`AppointmentBookingTool` requires `ADMIN`).
- Don't change tool names, `@Tool` descriptions or `@ToolParam` descriptions casually: the model selects tools by them. Treat such edits as behavior-changing.
- Preserve the chat memory conversation id (`userId`) and the advisor order.
- Streaming endpoints (`Flux`) must stay non-blocking: no `.block()`, no blocking JPA/HTTP calls inside the reactive chain without `subscribeOn(Schedulers.boundedElastic())`.

### Security
- Never weaken `SecurityConfig` rules, role mapping (`realm_access` + `resource_access.medassistant-client`) or tool authorization checks as part of a refactor.
- Repeated `jwt.getClaim("userId")` + `securityUtils.extractRole(authentication)` in controllers is a fine extraction candidate (e.g. a small resolver/helper), as long as the values passed downstream are identical.

### JPA / data
- Look for N+1 queries (lazy associations accessed in loops) → `@EntityGraph`, `JOIN FETCH` or projections.
- Prefer derived queries / `@Query` filtering in the DB over `findAll()` + Java stream filtering.
- Read-only service methods: `@Transactional(readOnly = true)`.
- Don't change entity mappings or column names: `ddl-auto: update` would alter the real schema (RDS in `aws`). That is behavior-changing.

### HTTP clients (OpenFDA, Ollama Cloud)
- Reuse `RestClient` / `WebClient` beans built from the injected builders; don't create clients per call.
- Caching OpenFDA results (`@Cacheable`) is a valid optimization proposal, but it adds config — classify as *structural*.

### General Java
- Java 25 / Lombok are available: records for DTOs, `@RequiredArgsConstructor` for injection, pattern matching, `switch` expressions.
- Remove dead code, unused imports, commented-out blocks and duplicated logic; name things after what they do (watch typos in names like `DoctorServiceImp`, `explainCondtion`, `SymptomConsultationPrompProvider` — renaming public types/methods is *structural*).
- Don't add abstractions "for the future": an interface with a single implementation that isn't profile-switched or mocked doesn't need to be created.
- `TestChatClient` / `TestLlmCall` are scratch runners; leave them alone unless asked.

## Build & verify

The project requires **JDK 25**. Check `java -version`; if the local JDK is < 25, run Maven in a container:

```sh
docker run --rm -u "$(id -u):$(id -g)" -e HOME=/tmp -e MAVEN_CONFIG=/tmp/.m2 \
  -v "$PWD":/build -v ~/.m2:/tmp/.m2 -w /build maven:3.9-eclipse-temurin-25 \
  ./mvnw -B -Dmaven.repo.local=/tmp/.m2/repository verify
```

With a local JDK 25: `./mvnw verify`.

If the change touched profile-specific beans, also mention that runtime startup of the other profile was not exercised (full startup needs Keycloak, Gemini and PostgreSQL/Chroma) unless you actually ran it.

## Don't

- Don't mix refactoring with new features or bug fixes. If you find a bug, report it separately.
- Don't reformat whole files or reorder members just for style — it buries the real diff.
- Don't commit; leave the changes in the working tree for the user to review.
