---
name: unit-test
description: Creates unit tests (JUnit 5 + Mockito + AssertJ) for the classes in this Spring Boot 4 / Spring AI 2 project — services, tools, agents, controllers, utils and repositories. Use it when the user asks to create, add or generate unit tests, "unit test", or test coverage for a class or package.
arguments: [target]
argument-hint: <ClassName | path | package> [more targets...]
---

# Unit test — mediaassistant

Target: `$target` (full arguments: `$ARGUMENTS`).

Generate unit tests for the target: a class name (e.g. `AppointmentBookingTool`), a path or a package. If several targets were given, cover each one. If the target is empty (the skill was triggered without arguments), take it from the user's request, or ask which class to test.

## Workflow

1. Read the entire target class and its direct dependencies (interfaces, DTOs, entities) before writing anything.
2. List the behaviors to cover: happy path, branches (`if`/`orElse`), role-based authorization, null/empty values, exceptions.
3. Write the test in `src/test/java/<same package>/<Class>Test.java`, mirroring `src/main/java`.
4. Run only that test (see **Running**) and fix it until it passes. Do not consider a test done until you have seen it green; if it cannot be run, say so.
5. Summarize for the user which cases each test covers and the result of the run.

## Conventions

- JUnit 5 (`org.junit.jupiter.api`), Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`), AssertJ (`assertThat`). All of it comes with `spring-boot-starter-test`, already pulled in transitively by the `*-test` starters in `pom.xml`.
- Method names: `method_condition_expectedResult` (e.g. `bookAppointment_whenRoleIsPatient_returnsDeniedMessage`). Use `@DisplayName` when it adds clarity.
- Given / When / Then structure with a blank line between blocks.
- One behavior per test. Verify interactions (`verify`, `verifyNoInteractions`) only when they are part of the contract (e.g. nothing is booked when the role is not ADMIN).
- **Prefer plain tests without a Spring context** (`new` or `@InjectMocks`). Classes use `@RequiredArgsConstructor`, so Mockito constructor injection works out of the box.
- Never call a real LLM, OpenFDA, Chroma or PostgreSQL from a unit test.

## Recipes by class type

### Tools (`tools/`)
Build the `ToolContext` by hand and mock the service:

```java
ToolContext ctx = new ToolContext(Map.of("userId", 1L, "role", "PATIENT"));
String result = tool.bookAppointment("Cardiology", "2026-12-01", "10:00", 5L, ctx);
assertThat(result).contains("Denied access");
verifyNoInteractions(appointmentService);
```
For tools with authorization, always cover both the ADMIN and the non-ADMIN branch, plus parsing of invalid dates/times (`DateTimeParseException`).

### Services / agents using `ChatClient`
Mock the fluent `ChatClient` API with deep stubs:

```java
@Mock(answer = Answers.RETURNS_DEEP_STUBS) ChatClient chatClient;
when(clientResolver.resolve("gemini")).thenReturn(chatClient);
when(chatClient.prompt().user(anyString()).toolContext(anyMap()).call().content()).thenReturn("ok");
```
- Match the chain exactly to what the code uses (`.system(...)`, `.advisors(...)`, `.entity(SomeClass.class)` for structured output, `.stream().content()` → return `Flux.just(...)`).
- To verify the `toolContext`, use an `ArgumentCaptor<Map<String,Object>>` and assert on `userId` and `role`.
- Classes that load a `PromptTemplate` in `@PostConstruct` from `@Value("classpath:prompts/...")`: set the `Resource` with `ReflectionTestUtils.setField(obj, "field", new ClassPathResource("prompts/x.st"))` and call the `@PostConstruct` method manually.

### ClientResolver
`ClientResolverAws` must reject anything other than `gemini`; `ClientResolverDefault` must resolve `gemini`, `ollama` and the default value. Pass `ChatClient` mocks through the constructor.

### Utils (`SecurityUtils`, etc.)
Plain tests. For `Authentication` use `new TestingAuthenticationToken("u", null, "ROLE_ADMIN")` (from `spring-security-core`, already available) — cover ADMIN, PATIENT, both, and neither → `"UNKNOWN"`.

### Controllers (`@WebMvcTest`)
Only if the user asks for web-layer tests. In Spring Boot 4:
- `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`
- `org.springframework.test.context.bean.override.mockito.MockitoBean` (replaces `@MockBean`, which was removed)
- JWT support requires `spring-security-test` (`SecurityMockMvcRequestPostProcessors.jwt()`), which is **not in `pom.xml`**. Ask the user for confirmation before adding:
  ```xml
  <dependency>
      <groupId>org.springframework.security</groupId>
      <artifactId>spring-security-test</artifactId>
      <scope>test</scope>
  </dependency>
  ```
- Example: `mockMvc.perform(post("/api/v1/chat").with(jwt().jwt(j -> j.claim("userId", 1L)).authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))).contentType(APPLICATION_JSON).content("{\"prompt\":\"hello\"}"))`.
- `@WebMvcTest` only loads `SecurityConfig` if it is imported (`@Import(SecurityConfig.class)`); mock `SecurityUtils` or import it.

### Repositories (`@DataJpaTest`)
Only for derived queries or custom `@Query` methods. Boot 4 annotation: `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`. Requires an embedded database or Testcontainers, which are **not in `pom.xml`**: ask the user before adding dependencies. Make sure `DataLoader` and the AI beans are not loaded in the slice.

## Running

The project requires **JDK 25**. Check `java -version`; if the local JDK is < 25, run Maven in a container (results land in the user's `target/`):

```sh
docker run --rm -u "$(id -u):$(id -g)" -e HOME=/tmp -e MAVEN_CONFIG=/tmp/.m2 \
  -v "$PWD":/build -v ~/.m2:/tmp/.m2 -w /build maven:3.9-eclipse-temurin-25 \
  ./mvnw -B -Dmaven.repo.local=/tmp/.m2/repository -Dtest=TestName -Dsurefire.failIfNoSpecifiedTests=false test
```

With a local JDK 25: `./mvnw -Dtest=TestName test`.

Finally, run the whole suite (`test` without `-Dtest`) to check nothing else broke, and report the actual result (tests run / failed).

## Don't

- Don't create full-context `@SpringBootTest` tests: they require Keycloak, Gemini and PostgreSQL.
- Don't change production code to make it testable without telling the user.
- Don't treat `TestChatClient` / `TestLlmCall` as tests (they are scratch runners).
