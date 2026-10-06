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
- Rough edge: a borrow request missing an id gets a 400 with the generic detail "Failed to read request". The message from `BorrowRequest` is lost when Jackson wraps the exception.
- Running: `mvn package`, then `java -jar bootstrap/target/bootstrap-1.0-SNAPSHOT.jar`. The sample ids are logged at startup.
