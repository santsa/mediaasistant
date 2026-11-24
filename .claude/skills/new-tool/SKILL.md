---
name: new-tool
description: Adds a new Spring AI @Tool to this project and wires it into every ChatClient that should expose it, in both profiles (AssistantConfig and AssistantConfigAws), with ToolContext-based authorization and a unit test. Use it when the user asks to add, create or expose a new tool, function or capability the model can call.
arguments: [name]
argument-hint: <toolName> ["what it does / who may use it"]
---

# New tool — mediaassistant

Tool: `$name` (full arguments: `$ARGUMENTS`).

Create a new tool the model can call. The first argument is the tool method name (e.g. `cancelAppointment`); the rest describes what it does and who may use it. If anything essential is missing (what data it touches, which roles may call it, read-only or write), ask before writing code.

## Workflow

1. **Study the existing tools** in `src/main/java/com/santsa/mediaassistant/tools/` (`AppointmentBookingTool` is the reference for role checks, `DrugInfoTool` for external calls) and the service layer they delegate to.
2. **Decide the home**: a new method on an existing tool class if it belongs to the same domain, otherwise a new `<Domain>Tool` class. Tools stay thin: logic goes in a service in `service/` (interface + `Impl`), the tool only validates, authorizes and delegates.
3. **Write the tool** following the conventions below.
4. **Wire it** everywhere it must be available (see **Registration**). This is the step that most often goes wrong.
5. **Test it** with the `unit-test` skill conventions: authorized role, denied role, invalid input, delegation to the service.
6. **Build & run the tests** (JDK 25; use the Maven container from the `unit-test` skill if the local JDK is older) and report the real result.
7. **Document**: if it adds user-visible capability, add a line to `README.md` where tools are described.

## Tool conventions

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class XxxTool {

    private final XxxService xxxService;

    @Tool(description = "Clear sentence of WHAT it does and WHEN to use it. State restrictions, e.g. 'Only administrative staff can execute this action. Ask for confirmation before executing.'")
    public String doSomething(
            @ToolParam(description = "Meaning and exact format, e.g. 'Date in format yyyy-MM-dd'") String date,
            ToolContext context) {
        log.info("Invoked doSomething: date={}", date);
        String role = String.valueOf(context.getContext().get("role"));
        Long userId = (Long) context.getContext().get("userId");
        ...
    }
}
```

- **Descriptions are the API for the model**: precise, in English, with formats for every parameter. Write-actions must say "Ask for confirmation before ..." in the description.
- **Authorization lives in the tool**, read from `ToolContext` (`role`, `userId`), never from parameters the model fills in. A `PATIENT` may only access their own data: compare against `userId` from the context, not a `patientId` argument.
- Return a short human-readable `String` (or a small record/DTO) — on denial or not-found, return a clear message instead of throwing; the model relays it.
- Parse dates/times with `LocalDate.parse` / `LocalTime.parse` and handle `DateTimeParseException` with a helpful message.
- Don't log sensitive patient data beyond IDs.

## Registration

The tool must be added in **every** place that should expose it:

| Where | Profile | What to change |
| --- | --- | --- |
| `config/AssistantConfig` | `!aws` | inject the tool and add it to `defaultTools(...)` in the shared `createChatClient(...)` helper (used by both the Gemini and Ollama clients) |
| `config/AssistantConfigAws` | `aws` | inject the tool and add it to the Gemini ChatClient's `defaultTools(...)` |
| `agent/OpenAgentServiceImpl` | both | add it to `.tools(...)` only if the free-form agent should use it (note: it deliberately excludes `AppointmentBookingTool`; write-tools usually stay out) |
| other agents (`agent/**`) | both | only if that workflow needs it |

After editing, `grep -rn "<ToolClass>" src/main/java` and check that `AssistantConfig` and `AssistantConfigAws` both reference it. Update the "five `@Tool` beans" mention in `CLAUDE.md` if the count changes.

## Don't

- Don't put business logic or repository calls directly in the tool.
- Don't trust IDs or roles passed as tool parameters for authorization.
- Don't register a write-tool (booking, cancelling, modifying data) in the open agent without the user's explicit approval.
