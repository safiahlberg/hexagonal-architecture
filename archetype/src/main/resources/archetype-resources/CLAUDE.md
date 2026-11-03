#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
${symbol_pound} CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

A sample hexagonal-architecture (ports and adapters) service in Java 27 and Maven, using a library-lending domain: members borrow, renew and return book copies. It runs as a Spring Boot 4 app, but Spring is confined to the outer modules.

${symbol_pound}${symbol_pound} Commands

No Maven wrapper; use the system `mvn`.

```bash
mvn clean verify                                   ${symbol_pound} build, enforcer rules, all tests
mvn validate                                       ${symbol_pound} enforcer dependency rules only (fast)
mvn test -pl domain -Dtest=LoanTest                ${symbol_pound} one test class in one module
mvn test -pl inbound/adapter-rest -am \
    -Dtest='LendingControllerTest${symbol_pound}brokenJsonIsBadRequest' \
    -Dsurefire.failIfNoSpecifiedTests=false        ${symbol_pound} one method; -am rebuilds the modules it depends on
```

Without `-am`, `-pl` resolves sibling modules from `~/.m2`, which may hold stale snapshots. Run `mvn install -DskipTests` first, or add `-am`.

The `adapter-persistence` and `bootstrap` tests start PostgreSQL with Testcontainers, so they need Docker, or Podman with its socket running (`systemctl --user start podman.socket`; `DOCKER_HOST` points at it).

Run the app against PostgreSQL from `compose.yaml`: `docker compose up -d`, then `mvn package -DskipTests && java -jar bootstrap/target/bootstrap-${version}.jar`. Or, after `mvn install -DskipTests`, `mvn spring-boot:test-run -pl bootstrap` runs it against a throwaway container (`TestLibraryApplication`). At every start, `SampleData` writes sample members and copies with fixed ids and logs them.

${symbol_pound}${symbol_pound} Module structure and dependency rules

```
domain/                        ${groupId}:domain                 pkg ${package}.domain.*
core/                          ${groupId}:core                   pkg ${package}.core
inbound/inbound-port/          ${groupId}.inbound:inbound-port   pkg ${package}.inbound.port
inbound/adapter-rest/          ${groupId}.inbound:adapter-rest   pkg ${package}.inbound.rest
outbound/outbound-port/        ${groupId}.outbound:outbound-port pkg ${package}.outbound.port
outbound/adapter-persistence/  ...:adapter-persistence          pkg ${package}.outbound.persistence
outbound/adapter-notification/ ...:adapter-notification         pkg ${package}.outbound.notification
bootstrap/                     ${groupId}:bootstrap              pkg ${package}.bootstrap
```

Dependency direction is enforced by `maven-enforcer-plugin` `bannedDependencies` rules, which run in `validate` and include transitive and test-scope dependencies. A forbidden dependency fails the build, so don't work around a failure:
- `inbound/pom.xml` and `outbound/pom.xml` ban the other side's groupId and `${groupId}:core`, and allow only the port from their own side, so adapters can't depend on each other. Every module under a side inherits the rule; this is why each side has its own groupId.
- `domain`, both ports and `core` use allow-lists: they ban `*` and include only what they need, so a new framework is refused by default. `domain` allows no internal module, the ports allow `domain`, and `core` allows `domain` and the two ports. All of them also allow the test libraries (`org.junit*`, `org.opentest4j`, `org.apiguardian`, `org.jspecify`).
- Spring belongs only in adapters and `bootstrap`. Outbound adapters may technically use it; that they currently don't is a convention, because a future adapter may have a reason to.
- `bootstrap` is the composition root and the only module without a rule; it is meant to see everything.

The enforcer sees only dependencies between modules, not which methods are called. `ArchitectureTest` in `bootstrap` uses ArchUnit for what it can't see: only `core` may call `Loan.giveBack`, and only outbound adapters may call `Loan.restore`. It uses the plain `archunit` library in a normal JUnit test, not ArchUnit's JUnit engine.

Conventions that keep these rules effective:
- Parent POMs (`inbound/`, `outbound/`) carry rules only. Every module declares its own dependencies explicitly, with `${symbol_dollar}{project.version}` for internal ones, rather than inheriting them.
- A new adapter goes under `inbound/` or `outbound/` so it inherits that side's rules.
- Each module has its own root package under `${package}`, so packages are never split across modules.
- There is deliberately no JPMS (`module-info.java`). It was tried and removed: Spring Boot runs on the classpath, and the enforcer already covers module boundaries.

${symbol_pound}${symbol_pound} Domain and use cases

- All lending rules are in `LendingPolicy`. `Loan.start` is package-private, so new loans can only come from `LendingPolicy.lend`. Rule violations throw `LendingRefusedException` with a `Reason`. Persistence adapters rebuild loans with `Loan.restore`, which applies no rules.
- Whether a copy is available is derived from active loans (`LoanRepository.findActiveByCopy`); `BookCopy` has no status field.
- Core has one service per inbound port (`BorrowBookService`, ...). It gets "today" from an injected `java.time.Clock`; tests use `Clock.fixed`.
- `core` tests can't use the real adapters, which the enforcer bans even in test scope. They use `FakeLibrary`, a single fake for all outbound ports.
- `adapter-persistence` uses plain JPA (Hibernate, no Spring Data) on PostgreSQL. Domain objects are mapped to and from separate `*Entity` classes, so the domain has no persistence annotations and changes to a `Loan` only persist after `save`. Column names are explicit in `@Column`, because the adapter tests run Hibernate without Boot's naming strategy.
- The schema is the Flyway migrations in `adapter-persistence/src/main/resources/db/migration`; Hibernate only validates it (`ddl-auto=validate`, also in the adapter tests). Change the schema with a new migration, never by editing an applied one.
- A partial unique index allows one active loan per copy. `JpaLoanRepository.save` flushes and turns a violation into `LendingRefusedException(COPY_ON_LOAN)`, which covers concurrent borrows that both passed `LendingPolicy`. For the loan limit, `BorrowBookService` loads the member with `MemberRepository.findByIdForUpdate`, a row lock held until commit, so borrows by one member run one after the other. `ConcurrentBorrowingTest` in `bootstrap` covers both races.
- The adapter tests are Spring-free: `TestDatabase` starts one container per test JVM, runs Flyway and builds an `EntityManagerFactory`. Each test rolls back its transaction; clear the entity manager before reading, or reads come from its cache.

${symbol_pound}${symbol_pound} Spring wiring

- The root POM imports `spring-boot-dependencies` as a BOM; it does not use `spring-boot-starter-parent`. So `-parameters` (`maven.compiler.parameters`) and the `repackage` execution in `bootstrap` are configured explicitly. `junit.version` is kept in line with the JUnit version Spring Boot manages.
- `core` and the outbound adapters have no Spring annotations. They are wired with explicit `@Bean` methods in `bootstrap/.../LendingConfiguration`.
- Transactions are per use case: `LendingConfiguration` exposes each use case as a lambda that runs the core service in a `TransactionTemplate`, so `core` needs no `@Transactional`. The repositories get a shared `EntityManager` proxy bound to the current transaction, and `@EntityScan` points at the adapter's package. `spring.jpa.open-in-view` is off.
- The `MemberNotifier` bean is `AfterCommitMemberNotifier` wrapping the real notifier: notifications go out only after the use case's transaction commits, are dropped on rollback, and a failing notifier is logged rather than failing the committed request. Delivery is at most once; an outbox would be needed for guaranteed delivery.
- `LibraryApplication` scans only `${package}.bootstrap` and `${package}.inbound.rest`. A new Spring-annotated adapter package must be added to `scanBasePackages`.
- `bootstrap` tests import `TestDatabaseConfiguration`, a PostgreSQL container with `@ServiceConnection`, which overrides the datasource in `application.properties`. Boot 4 splits persistence into modules: `bootstrap` uses `spring-boot-hibernate`, `spring-boot-starter-jdbc` and `spring-boot-starter-flyway` rather than `spring-boot-starter-data-jpa`.
- `LendingExceptionHandler` turns the exceptions into RFC 9457 problem details: `NotFoundException` → 404, `LendingRefusedException` → 409 with `reason`, malformed or incomplete requests → 400 with a specific `detail`. It is `@Order(HIGHEST_PRECEDENCE)` because `bootstrap` enables `spring.mvc.problemdetails.enabled`, and Boot's handler would otherwise answer first with generic messages. `@WebMvcTest` in `adapter-rest` does not load that setting, so behaviour that depends on it is covered by `LibraryApplicationTest` in `bootstrap`.
- Modules whose tests use Mockito (`adapter-rest`, `bootstrap`, via the `-webmvc-test` starter) declare `maven-dependency-plugin` (its `properties` execution is in the root `pluginManagement`) and give surefire `-javaagent:${symbol_dollar}{org.mockito:mockito-core:jar} -Xshare:off`. This loads Mockito as an agent instead of letting it self-attach, which the JDK warns about; `-Xshare:off` silences the class-data-sharing warning caused by Mockito appending to the boot class path. It is per module because the jar-path property only exists where Mockito is a dependency; a new module using Mockito needs both plugin entries.
- Spring Boot 4 uses Jackson 3 (`tools.jackson.*` packages, not `com.fasterxml.jackson.*`) and the `spring-boot-starter-webmvc` / `-webmvc-test` starters.
