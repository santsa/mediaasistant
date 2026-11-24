---
name: prompt-tune
description: Improves one of the StringTemplate prompts in src/main/resources/prompts/*.st (system prompt, routing, orchestrator, analysis, appointment chain…) while keeping its template variables and the code contract that uses it, and compares model output before and after with the same inputs. Use it when the user asks to tune, improve, rewrite, shorten or fix the behavior of a prompt or of the assistant's answers.
arguments: [prompt]
argument-hint: <prompt-name (e.g. routing-symptom)> ["desired change"]
---

# Prompt tune — mediaassistant

Prompt: `$prompt` (full arguments: `$ARGUMENTS`).

Prompts live in `src/main/resources/prompts/<name>.st`. The first argument is the file name with or without `.st`; the rest is the desired change. If no prompt is given, list the available files and ask. If the change is vague ("make it better"), ask what is wrong today (examples of bad answers help most).

## 1. Understand the contract

- Read the prompt and find where it is used: `grep -rn "<name>.st" src/main/java`.
- From the Java code, note:
  - **Variables** passed to `PromptTemplate.render(Map.of(...))` / `.create(...)`. Every `{var}` in the file must still be provided, and no new `{var}` may be added without changing the code (StringTemplate fails at runtime on missing vars). Literal braces must stay escaped.
  - Exception: `system-prompt.st` is **not** rendered by StringTemplate — `AssistantConfig` / `AssistantConfigAws` read it as a string and do `.replace("{currentDate}", ...)`. Keep `{currentDate}` exactly as written; any other `{...}` would be sent to the model literally.
  - **Output contract**: if the caller uses `.entity(SomeRecord.class)` (e.g. classification into `QueryType`, appointment extraction, structured analysis), the prompt must keep asking for exactly those fields/enum values. Check the record/enum.
  - **Role in the flow**: system vs user message, which tools the client has (the prompt may reference tool usage), whether its output feeds another prompt (orchestrator workers → synthesis, extraction → proposal).
- `system-prompt.st` is shared by **both** profiles (`AssistantConfig` and `AssistantConfigAws`): changes affect every chat and tool call.

## 2. Baseline

Pick 3–5 representative inputs (from the user, or typical ones for this prompt, including one edge case — e.g. an emergency symptom for routing, a vague request for extraction). If the app is running (see `/smoke-test` for tokens and startup), call the endpoint that uses the prompt with each input and save the responses to the scratchpad. If the app can't be run, say so and do a review-only change (skip step 4's comparison and state that it is untested).

## 3. Edit

- Make the minimal change that addresses the goal. Keep the existing style: English, clear sections, explicit output format.
- Keep the medical safety framing: no definitive diagnosis, recommend a professional, clear emergency guidance (call 112 / emergency services), no dosage advice beyond what the drug data supports, respond in the user's language if the current prompt does so.
- Prefer concrete instructions and short examples over adjectives ("be concise" → "answer in at most 5 bullet points").
- Don't put user data or secrets in prompts.

## 4. Compare

Restart the app (prompts are loaded at startup in `@PostConstruct`) and run the same inputs. Present a before/after table per input with a short verdict. Note that LLM output is non-deterministic: call out differences that are clearly due to the change vs. noise, and run an input twice if in doubt.

## 5. Report

Show the diff of the `.st` file, the comparison, and any risk (e.g. "classification now leans to EMERGENCY for chest pain — intended"). If the code contract had to change (new variable, new record field), list the Java changes and ask before making them — that turns this into a code change, which should get a unit test.
