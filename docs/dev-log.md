# Dev log

## 2026-10-06 — restructure-by-direction

**Goal:** Replace the `domain` / `application` / `framework` layout with one grouped by direction (`domain`, `core`, `inbound`, `outbound`), so inbound and outbound code can't be mixed by accident. The dependency rules are enforced by the build rather than by convention.

**Steps**
1. Moved modules: `application/core` → `core/`, `application/port-inbound` → `inbound/port/`, `framework/adapter-inbound` → `inbound/adapter-rest/`, `application/port-outbound` → `outbound/port/`, `framework/adapter-outbound` → `outbound/adapter-persistence/`. Removed the `application` and `framework` aggregators.
2. Rewrote the POMs. Each side has its own groupId (`com.wixia.inbound`, `com.wixia.outbound`), and every module declares its own dependencies with `${project.version}`. The parent POMs now carry no dependencies.
3. Added `maven-enforcer-plugin` 3.6.2. Each side's parent bans the other side's groupId and `com.wixia:core`. Each port bans its own side's groupId, so a port can't depend on its own side's adapters.
4. Verified that `mvn validate` passes. Also checked in a scratch copy that it fails for: inbound adapter → outbound port, outbound adapter → core, inbound port → inbound adapter, and inbound adapter → core (which would bring in outbound-port transitively).

**Decisions / deviations**
- Ports are flat siblings of the adapters (`inbound/port`), not nested under them. A `jar` module can't have children, and a nested port would inherit the adapter's parent configuration.
- The port artifactIds are `inbound-port` and `outbound-port`, not just `port`. Separate groupIds already keep the coordinates unique, but two jars both named `port-1.0-SNAPSHOT.jar` would clash in a shared `lib/` directory or a fat jar.
- The adapter names `adapter-rest` and `adapter-persistence` are placeholders for the eventual technology.
- Follow-up the same day: added `core-isolation`, which bans both sides' groupIds but allows `inbound-port` and `outbound-port` back in through `<includes>`. Also added `domain-isolation`, which bans `com.wixia`, `com.wixia.inbound` and `com.wixia.outbound`. Verified that both fail on a violation. For any module that already depends on `domain`, a domain → module dependency is caught first by Maven's cycle detection. The domain rule matters for future modules that don't depend on `domain` (e.g. a shared `common`).
- The archetype's `AppTest` placeholders used JUnit 3 while the root POM provides JUnit 5, so `test-compile` failed. They were ported to JUnit 5 afterwards, and `mvn clean verify` now passes.

## 2026-10-06 — library-lending-model

**Goal:** Replace the archetype placeholders with a sample domain that shows why the hexagonal structure is worth it: real rules in the domain, use cases that do more than load and save, and more than one outbound port. The chosen domain is library lending: members borrow, return and renew book copies. Each Maven module also gets its own root package and a JPMS `module-info.java`.

**Steps**
1. Removed the `App` and `AppTest` placeholders from every module.
2. Domain (`com.wixia.library.domain`): `Member`, `BookCopy`, `Loan` and `LendingPolicy`. The rules: at most 5 active loans, no borrowing while a loan is overdue, a copy is lent to one member at a time, a loan lasts 3 weeks and can be renewed once while not overdue. A violated rule throws `LendingRefusedException` with a `Reason`. 14 tests.
3. Ports: `inbound.port` has `BorrowBookUseCase`, `ReturnBookUseCase`, `RenewLoanUseCase` and `NotFoundException`. `outbound.port` has `MemberRepository`, `BookCopyRepository`, `LoanRepository` and `MemberNotifier`.
4. Core: one service per use case. The tests use `FakeLibrary`, a single fake for all outbound ports, because the enforcer bans the real adapters from `core` (test scope included). 7 tests.
5. Outbound adapters: in-memory repositories in `adapter-persistence`, where loans are stored as `LoanRecord` snapshots (3 tests). A new `adapter-notification` module provides a `LoggingMemberNotifier`.

**Decisions / deviations**
- Package and module names follow the module layout: `com.wixia.library.{domain, inbound.port, outbound.port, core, outbound.persistence, outbound.notification}`. Each JPMS module is named after its root package, so packages are never split across modules. The ports and `core` use `requires transitive`, because their signatures expose the types of the modules they depend on.
- "Today" comes from an injected `java.time.Clock` rather than a separate time port. The JDK already provides the abstraction, and tests use `Clock.fixed`.
- Copy availability is derived from the active loans, not stored on `BookCopy`. That keeps a single source of truth and keeps each use case to writing one aggregate.
- New loans can only come from `LendingPolicy.lend` (`Loan.start` is package-private). Persistence adapters rebuild loans through `Loan.restore`, which applies no rules.
- Added `outbound/adapter-notification`. It wasn't in the original layout, but it shows a second adapter on one side, and it picked up the outbound enforcer rules without any configuration of its own.
- Not done: the REST adapter (`inbound/adapter-rest` has no sources yet) and a composition root that wires services to adapters, so there's nothing runnable yet.
- Follow-up the same day: removed all `module-info.java` files again, to keep the door open for running as a Spring Boot service. Spring Boot runs the fat jar on the classpath, where `module-info` is ignored. Spring, Jackson and Hibernate would each need `opens` lines, and the enforcer rules already guard module dependencies. The one-root-package-per-module layout stays: it avoids split packages, gives Spring a clean `com.wixia.library` base to scan, and keeps JPMS possible later. If internal packages need hiding, use ArchUnit rather than JPMS.

## 2026-10-06 — rest-adapter-spring-boot

**Goal:** Expose the lending use cases over REST with Spring Boot and make the service runnable, without letting Spring into `domain`, the ports or `core`.

**Steps**
1. The root POM imports `spring-boot-dependencies` 4.1.1 (versions only) and moves `junit-bom` to 6.0.3, the version Boot is built against. The domain, port and core enforcer rules now also ban `org.springframework*`. Verified in a scratch copy with `spring-context`, `spring-boot` and `spring-data-commons`.
2. `adapter-rest` (`com.wixia.library.inbound.rest`): `LendingController` serves `POST /loans`, `POST /loans/{loanId}/renewal` and `POST /copies/{copyId}/return`. `LendingExceptionHandler` maps `NotFoundException` to 404 and `LendingRefusedException` to 409 with a `reason` property, as RFC 9457 problem details. 6 `@WebMvcTest` tests with mocked use cases. The root POM now compiles with `-parameters`, which Spring needs to bind `@PathVariable` by name.
3. New top-level `bootstrap` module (`com.wixia:bootstrap`), the composition root. `LendingConfiguration` wires the services to the adapters with plain `@Bean` methods. `SampleData` adds 2 members and 3 copies with fixed ids. `spring-boot-maven-plugin` builds the runnable jar. A `@SpringBootTest` runs a full borrow → conflict → renew → return → re-borrow round trip. Also checked by running the jar and calling it with curl.

**Decisions / deviations**
- The composition root has to be its own module. `adapter-rest` may not depend on `core` or the outbound adapters, so it can't do the wiring itself. `bootstrap` has no enforcer rule, being the one module that is meant to see everything.
- Component scanning covers only `bootstrap` and `inbound.rest`. `core` and the outbound adapters have no Spring annotations and are wired explicitly, so replacing an adapter means changing one `@Bean`.
- Both `-parameters` and the explicit `repackage` execution would normally come from `spring-boot-starter-parent`. That parent isn't used, since the project has its own root POM. Its BOM is imported instead.
- Follow-up: a borrow request missing an id used to get a 400 with the generic detail "Failed to read request", because the check in `BorrowRequest`'s constructor ran inside Jackson, which wrapped the exception. The check now runs in the controller after deserialisation, and `InvalidRequestException` maps to a 400 that names the missing fields. `LendingExceptionHandler` also handles `HttpMessageNotReadableException`: when Jackson reports which field had the wrong type (a `MismatchedInputException` with a path), the detail names it, e.g. "Invalid value for memberId, expected UUID". Otherwise the detail is "Request body is missing or is not valid JSON". A malformed UUID in the URL path (`MethodArgumentTypeMismatchException`) gets the same wording. The handler is `@Order(Ordered.HIGHEST_PRECEDENCE)`: without that, Spring Boot's own problem-details handler (switched on by `spring.mvc.problemdetails.enabled` in `bootstrap`) runs first and answers with its generic messages. `@WebMvcTest` doesn't load that setting and missed this, so `LibraryApplicationTest` covers it end to end.
- Running: `mvn package`, then `java -jar bootstrap/target/bootstrap-1.0-SNAPSHOT.jar`. The sample ids are logged at startup.

## 2026-10-07 — maven-archetype

**Goal:** Make a Maven archetype from the project, so a new service can start with the same module layout, enforcer rules and Spring Boot wiring under its own coordinates. It generates the full lending sample, not an empty skeleton, so the generated project builds, runs and shows how the parts fit together.

**Steps**
1. `archetype/` is a standalone `maven-archetype` project (`com.wixia:hexagonal-architecture-archetype`), outside the root reactor. `archetype-metadata.xml` declares every module. `sync.sh` generates `archetype-resources` from the sample's files and turns groupId, root artifactId, version and package into parameters. The inbound and outbound groupIds become `${groupId}.inbound` / `${groupId}.outbound`, enforcer rules included.
2. Integration test `projects/basic` generates a project as `org.example.shop:lending-service:0.1-SNAPSHOT` with package `org.example.shop.lending` and runs `mvn verify` on it: enforcer rules pass and all 37 tests pass. Also checked by generating with the sample's own coordinates and diffing against the repo. Apart from the removed pointers to `docs/` and `archetype/`, the result is byte-identical.
3. `archetype/README.md` documents usage and syncing. The root `README.md` and `CLAUDE.md` point to it, and `sync.sh` strips those pointers from the generated copies.

**Decisions / deviations**
- `mvn archetype:create-from-project` was tried first and discarded. Because module artifactIds (`domain`, `core`, `bootstrap`) match package segments, it replaced them with `__artifactId__` / `__parentArtifactId__` in source paths and package names. Its output would also have been a one-off copy to maintain by hand. `sync.sh` is deterministic and can be re-run after every change.
- The generated resources are committed rather than produced during the archetype build. That keeps `archetype/` a plain Maven project that needs no script to build, and `git status` after `sync.sh` shows any drift.
- Outside the reactor, so the root `mvn clean verify` stays as fast as before. The cost is that drift is only caught when someone runs `sync.sh`.
- Velocity pitfalls: `#` and `$` in Java and Markdown are escaped (`${symbol_pound}`, `${symbol_dollar}`), otherwise `##` headings would become Velocity comments. `${org.mockito:mockito-core:jar}` in POMs is a Velocity parse error and is wrapped in `#[[...]]#`. Other POM properties like `${project.version}` pass through, because Velocity leaves undefined references alone.
- The archetype jar drops `.gitignore`, so it is stored as `__gitignore__` with a `gitignore` property that defaults to `.gitignore`. Interactive generation therefore also asks to confirm that property.
- Module names and class names are fixed, not parameters.

## 2026-10-07 — postgres-persistence

**Goal:** Replace the in-memory store with a real database, so loans, members and copies survive a restart. PostgreSQL through plain JPA (Hibernate) in `adapter-persistence`, schema managed by Flyway, without letting persistence or transaction concerns into `domain`, the ports or `core`.

**Steps**
1. `adapter-persistence`: removed the `InMemory*` repositories and `LoanRecord`. Added `MemberEntity`, `BookCopyEntity` and `LoanEntity`, mapped to and from the domain types, and `Jpa*Repository` classes that take an `EntityManager`. The schema is `V1__create_lending_tables.sql`, with foreign keys and a partial unique index allowing one active loan per copy. `JpaLoanRepository.save` flushes and turns a violation of that index into `LendingRefusedException(COPY_ON_LOAN)`; the port's javadoc now says so. 8 tests against PostgreSQL 18 in a Testcontainers container, including the database refusing a second active loan for a copy.
2. `bootstrap`: `spring-boot-starter-jdbc`, `spring-boot-hibernate`, `spring-boot-starter-flyway` and the PostgreSQL driver. `LendingConfiguration` creates the repositories with a shared `EntityManager` proxy, and runs each use case in a `TransactionTemplate`. `SampleData` writes its rows in a transaction at every start. `compose.yaml` at the root provides the database that `application.properties` points at. Tests import `TestDatabaseConfiguration` (container with `@ServiceConnection`), and `TestLibraryApplication` lets `mvn spring-boot:test-run` run the app against a throwaway container. Checked by hand: a loan made before a restart of the jar was still there afterwards.
3. Docs and archetype: README and CLAUDE.md describe the database, how to run it and the container requirement for tests. `archetype-metadata.xml` now includes `compose.yaml` and the adapter's `src/main/resources`. The archetype's integration test generates and builds a project with all 42 tests passing.

**Decisions / deviations**
- Plain JPA rather than Spring Data JPA. The adapter keeps to the convention that outbound adapters have no Spring and are wired with explicit `@Bean`s, and its tests need no Spring context: `TestDatabase` runs Flyway and builds an `EntityManagerFactory` with Jakarta Persistence 3.2's `PersistenceConfiguration`. Hibernate is a compile dependency only to read the violated constraint's name from its `ConstraintViolationException`.
- Separate entity classes rather than annotating the domain types, so `domain` stays free of persistence annotations and `Loan.restore` remains the only way back in. Columns are named explicitly in `@Column`, because outside Boot Hibernate does not apply the snake_case naming strategy.
- Transactions are wrapped around the use cases in `bootstrap`, not declared with `@Transactional` in `core`, which may not see Spring. The use-case beans are therefore exposed by their port interface, not by the service class. `open-in-view` is off, so nothing touches the database outside a use case.
- The database, not just `LendingPolicy`, enforces one active loan per copy, because two concurrent borrows can both pass the policy check.
- The member is notified inside the transaction, so a notification could go out for a borrow whose commit then fails. That is harmless for the logging notifier; a real one would need an outbox or an after-commit hook.
- Flyway owns the schema and Hibernate only validates it (`ddl-auto=validate`, in the adapter tests too), so a mismatch between entities and migrations fails at startup and in the tests.
- Follow-up the same day: the limit of active loans per member had no database guard. A new `ConcurrentBorrowingTest` in `bootstrap`, which starts 8 borrows by one member at once, showed all 8 being lent against a limit of 5. `MemberRepository` now has `findByIdForUpdate`, which `JpaMemberRepository` implements with a `PESSIMISTIC_WRITE` lock (`for no key update` in PostgreSQL), and `BorrowBookService` uses it. Concurrent borrows by one member now wait for each other, and each one counts the loans the others committed. The lock is explicit in the port and the use case, rather than hidden in `findActiveByMember`, because serialising a member's borrows is a decision of the use case. A count can't be expressed as a constraint, and a trigger would duplicate the limit that `LendingPolicy` owns. The same test checks that 8 members borrowing one copy at once get exactly 1 loan. The adapter test checks that a second lock on a locked member fails with NOWAIT; it fails without the lock.
- `mvn clean verify` now needs Docker or Podman. On this machine Testcontainers uses Podman through `DOCKER_HOST`, which only works while `podman.socket` is running.
