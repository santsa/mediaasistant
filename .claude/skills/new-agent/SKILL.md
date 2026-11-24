---
name: new-agent
description: Adds a new LLM workflow / agent endpoint to this project following its existing pattern — StringTemplate prompt in prompts/, service interface + Impl that loads the PromptTemplate in @PostConstruct and calls ClientResolver, a JWT-secured controller endpoint under /api/v1, a unit test and README docs. Use it when the user asks for a new agent, workflow, analysis endpoint or AI feature exposed through the API.
arguments: [name]
argument-hint: <agentName> ["what it should do"]
---

# New agent — mediaassistant

Agent: `$name` (full arguments: `$ARGUMENTS`).

Create a new AI workflow exposed through the REST API. The first argument is the agent name (e.g. `triage`); the rest describes what it does. If the input/output shape or the pattern to use is unclear, ask before writing code.

## 1. Pick the pattern

Look at the closest existing implementation and copy its structure:

| Need | Reference |
| --- | --- |
| Single prompt → text answer | `service/AssistantServiceImpl` (`explain`, `symptoms`, `consult`) |
| Structured output (record) | `service/AnalysisServiceImpl` (`.entity(...)`) |
| Classify then dispatch to a specific prompt | `agent/RoutingWorkflowServiceImpl` + `QueryType` |
| Multi-step chain with state | `agent/AppointmentChainServiceImpl` + `PendingBookingStore` |
| Parallel workers + synthesis (optionally SSE progress) | `agent/orchestrator/OrchestratorServiceImpl` |
| Free-form tool-using agent | `agent/OpenAgentServiceImpl` |

Tell the user which pattern you chose and why.

## 2. Files to create / change

- **Prompt**: `src/main/resources/prompts/<name>.st` (StringTemplate, `{variable}` placeholders, English). One file per distinct prompt. Medical prompts must keep the safety framing of the existing ones (not a diagnosis, recommend a professional, emergency guidance).
- **Service**: `agent/<Name>Service.java` (interface) + `agent/<Name>ServiceImpl.java`:
  - `@Service @RequiredArgsConstructor @Slf4j`
  - `@Value("classpath:prompts/<name>.st") private Resource xxxResource;` → `new PromptTemplate(resource)` in `@PostConstruct`. Never load templates per request.
  - Get the client with `clientResolver.resolve(model)` (never inject a specific ChatClient — that breaks the `aws` profile, which only has Gemini).
  - Pass `.toolContext(Map.of("userId", userId, "role", role))` whenever tools can be reached; use `userId` as the chat memory conversation id if the agent needs memory.
  - Add `MedicalAuditAdvisor` like the other services do.
  - Structured output → a `record` in `dto/` (or `dto/analysis/`).
- **DTOs**: reuse `ChatRequest` (`prompt`, `model`) unless the endpoint really needs more fields; validate with `jakarta.validation`.
- **Controller**: add the endpoint to `controller/AgentController` (`/api/v1/agent/**`) or `AnalysisController` (`/api/v1/analysis/**`) — create a new controller only for a new area. Extract `userId` with `jwt.getClaim("userId")` and the role with `securityUtils.extractRole(authentication)`. Streaming → `Flux<String>` with `produces = "text/event-stream; charset=UTF-8"` and no blocking calls.
- **Security**: everything under `/api/v1/**` already requires `PATIENT` or `ADMIN`. If the agent must be ADMIN-only, enforce it (tool-level check or `@PreAuthorize` if method security is enabled — check `SecurityConfig` first) and tell the user.
- **Profiles**: the service must work in both `aws` and `!aws`. Only `gemini` exists in `aws`.

## 3. Verify

1. Unit test for the service (`unit-test` skill conventions, deep-stub `ChatClient`): prompt rendered with the right variables, toolContext contents, structured output mapping.
2. Build and run the tests (JDK 25; Maven container if the local JDK is older). Report the real result.
3. Offer to run `/smoke-test` against the new endpoint for a real end-to-end check.

## 4. Document

Add the endpoint to the endpoints section of `README.md` with a `curl` example in the same style as the existing ones.

## Don't

- Don't hardcode the model name or bypass `ClientResolver`.
- Don't inline long prompts in Java.
- Don't add write-tools to a new agent without the user's approval.
