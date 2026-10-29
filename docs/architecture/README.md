# Architecture

The Library Lending Service lets members borrow, renew and return book copies over a REST API. It is built with the hexagonal (ports and adapters) architecture: the lending rules and use cases are plain Java in the middle, and everything that talks to the outside world (HTTP, PostgreSQL, notifications) is an adapter around them.

This page follows the [C4 model](https://c4model.com), from the system's surroundings down to its components. The diagrams are generated from [`workspace.dsl`](workspace.dsl), a [Structurizr DSL](https://docs.structurizr.com/dsl) model that is the source of truth; see [Keeping this page up to date](#keeping-this-page-up-to-date).

## 1. System context

<!-- diagram:SystemContext -->
```mermaid
graph LR

    1["<b>Library client</b><br/><small>[Person]</small><br/>Front-desk application or<br />staff tool that lends, renews<br />and takes back book copies on<br />behalf of members."]
    style 1 fill:#08427b,stroke:#052e56,color:#ffffff
    2["<b>Member</b><br/><small>[Person]</small><br/>Library member who borrows<br />book copies and is told about<br />their loans."]
    style 2 fill:#08427b,stroke:#052e56,color:#ffffff
    3["<b>Library Lending Service</b><br/><small>[Software System]</small><br/>Lets members borrow, renew<br />and return book copies,<br />enforcing the library's<br />lending rules."]
    style 3 fill:#1168bd,stroke:#0b4884,color:#ffffff

    1-. "Borrows, renews and returns<br />copies using<br/><small>[JSON/HTTP: POST /loans, /loans/{id}/renewal, /copies/{id}/return]</small>" .->3
    3-. "Tells about confirmed and<br />renewed loans<br/><small>[Application log (placeholder for e-mail or SMS)]</small>" .->2

```
<!-- /diagram:SystemContext -->

- The **library client** is whatever calls the REST API on members' behalf, such as a front-desk application. The service has no user interface of its own.
- **Members** are told when a loan is confirmed or renewed. For now the notification is written to the application log, which stands in for e-mail or SMS.

## 2. Containers

<!-- diagram:Containers -->
```mermaid
graph LR

    1["<b>Library client</b><br/><small>[Person]</small><br/>Front-desk application or<br />staff tool that lends, renews<br />and takes back book copies on<br />behalf of members."]
    style 1 fill:#08427b,stroke:#052e56,color:#ffffff
    2["<b>Member</b><br/><small>[Person]</small><br/>Library member who borrows<br />book copies and is told about<br />their loans."]
    style 2 fill:#08427b,stroke:#052e56,color:#ffffff

    subgraph 3 ["Library Lending Service"]
      style 3 fill:none,stroke:#0b4884,color:#0b4884

      13[("<b>Lending Database</b><br/><small>[Container: PostgreSQL 18]</small><br/>Members, book copies and<br />loans. The schema comes from<br />Flyway migrations; a partial<br />unique index allows only one<br />active loan per copy.")]
      style 13 fill:#438dd5,stroke:#2e6295,color:#ffffff
      4["<b>Lending Application</b><br/><small>[Container: Java 27, Spring Boot 4]</small><br/>Spring Boot 4 application<br />built from the Maven reactor<br />and run as<br />bootstrap-1.0-SNAPSHOT.jar."]
      style 4 fill:#438dd5,stroke:#2e6295,color:#ffffff
    end

    1-. "Borrows, renews and returns<br />copies using<br/><small>[JSON/HTTP: POST /loans, /loans/{id}/renewal, /copies/{id}/return]</small>" .->4
    4-. "Reads from and writes to<br/><small>[JPA/JDBC]</small>" .->13
    4-. "Tells about confirmed and<br />renewed loans<br/><small>[Application log (placeholder for e-mail or SMS)]</small>" .->2

```
<!-- /diagram:Containers -->

The service is deployed as one Spring Boot application with its own PostgreSQL database.

| Container | Technology | Responsibility |
|---|---|---|
| Lending Application | Java 27, Spring Boot 4 | Serves the REST API and runs the use cases. Built as `bootstrap/target/bootstrap-1.0-SNAPSHOT.jar`. |
| Lending Database | PostgreSQL 18 | Stores members, book copies and loans. The schema comes from Flyway migrations; a partial unique index allows only one active loan per copy. |

## 3. Components

<!-- diagram:Components -->
```mermaid
graph LR

    1["<b>Library client</b><br/><small>[Person]</small><br/>Front-desk application or<br />staff tool that lends, renews<br />and takes back book copies on<br />behalf of members."]
    style 1 fill:#08427b,stroke:#052e56,color:#ffffff
    2["<b>Member</b><br/><small>[Person]</small><br/>Library member who borrows<br />book copies and is told about<br />their loans."]
    style 2 fill:#08427b,stroke:#052e56,color:#ffffff

    subgraph 3 ["Library Lending Service"]
      style 3 fill:none,stroke:#0b4884,color:#0b4884

      subgraph 4 ["Lending Application"]
        style 4 fill:none,stroke:#2e6295,color:#2e6295

        subgraph group1 ["Hexagon"]
          style group1 fill:none,stroke:#cccccc,color:#cccccc,stroke-dasharray:5

          6("<b>Inbound Port</b><br/><small>[Component: Java interfaces]</small><br/>Use-case interfaces<br />BorrowBookUseCase,<br />RenewLoanUseCase and<br />ReturnBookUseCase, plus<br />NotFoundException.")
          style 6 fill:#fbe9b7,stroke:#afa380,color:#000000
          7["<b>Core</b><br/><small>[Component: Plain Java]</small><br/>One service per use case<br />(BorrowBookService,<br />RenewLoanService,<br />ReturnBookService). Loads<br />aggregates, applies the<br />lending policy, saves and<br />notifies. Gets today from an<br />injected Clock."]
          style 7 fill:#f8d77a,stroke:#ad9655,color:#000000
          8["<b>Domain</b><br/><small>[Component: Plain Java]</small><br/>Member, BookCopy and Loan.<br />All lending rules live in<br />LendingPolicy, which refuses<br />with LendingRefusedException<br />and a Reason."]
          style 8 fill:#f5c242,stroke:#ab872e,color:#000000
          9("<b>Outbound Port</b><br/><small>[Component: Java interfaces]</small><br/>Interfaces the core needs:<br />MemberRepository,<br />BookCopyRepository,<br />LoanRepository and<br />MemberNotifier.")
          style 9 fill:#fbe9b7,stroke:#afa380,color:#000000
        end

        subgraph group2 ["Inbound adapters"]
          style group2 fill:none,stroke:#cccccc,color:#cccccc,stroke-dasharray:5

          5["<b>REST Adapter</b><br/><small>[Component: Spring Web MVC, Jackson 3]</small><br/>LendingController maps HTTP<br />requests onto use cases;<br />LendingExceptionHandler turns<br />exceptions into RFC 9457<br />problem details (404, 409<br />with reason, 400)."]
          style 5 fill:#85bbf0,stroke:#5d82a8,color:#000000
        end

        subgraph group3 ["Outbound adapters"]
          style group3 fill:none,stroke:#cccccc,color:#cccccc,stroke-dasharray:5

          10["<b>Persistence Adapter</b><br/><small>[Component: Jakarta Persistence, Hibernate (no Spring)]</small><br/>JPA repositories that map<br />domain objects to and from<br />separate *Entity classes and<br />rebuild loans with<br />Loan.restore.<br />JpaLoanRepository turns a<br />violation of the<br />one-active-loan-per-copy<br />index into<br />LendingRefusedException;<br />JpaMemberRepository locks the<br />member row for borrows. Holds<br />the Flyway migrations."]
          style 10 fill:#85bbf0,stroke:#5d82a8,color:#000000
          11["<b>Notification Adapter</b><br/><small>[Component: java.lang.System.Logger]</small><br/>LoggingMemberNotifier writes<br />the member's loan<br />confirmations and renewals to<br />the application log."]
          style 11 fill:#85bbf0,stroke:#5d82a8,color:#000000
        end

        12["<b>Bootstrap</b><br/><small>[Component: Spring Boot, Spring ORM, Flyway]</small><br/>Composition root.<br />LibraryApplication starts<br />Spring Boot;<br />LendingConfiguration wires<br />core and outbound adapters<br />with explicit @Bean methods,<br />runs each use case in a<br />TransactionTemplate and<br />defers notifications until<br />commit<br />(AfterCommitMemberNotifier);<br />SampleData seeds the<br />database."]
        style 12 fill:#dddddd,stroke:#9a9a9a,color:#000000
      end

      13[("<b>Lending Database</b><br/><small>[Container: PostgreSQL 18]</small><br/>Members, book copies and<br />loans. The schema comes from<br />Flyway migrations; a partial<br />unique index allows only one<br />active loan per copy.")]
      style 13 fill:#438dd5,stroke:#2e6295,color:#ffffff
    end

    1-. "Borrows, renews and returns<br />copies using<br/><small>[JSON/HTTP: POST /loans, /loans/{id}/renewal, /copies/{id}/return]</small>" .->5
    5-. "Invokes use cases through" .->6
    5-. "Reads loans and ids from" .->8
    7-. "Implements" .->6
    7-. "Loads, saves and notifies<br />through" .->9
    7-. "Applies lending rules with" .->8
    6-. "Is expressed in terms of" .->8
    9-. "Is expressed in terms of" .->8
    10-. "Implements the repositories<br />of" .->9
    10-. "Reads from and writes to<br/><small>[JPA/JDBC]</small>" .->13
    11-. "Implements MemberNotifier of" .->9
    11-. "Tells about confirmed and<br />renewed loans<br/><small>[Application log (placeholder for e-mail or SMS)]</small>" .->2
    12-. "Instantiates and wires" .->7
    12-. "Instantiates and seeds" .->10
    12-. "Instantiates, deferring its<br />calls until commit" .->11
    12-. "Migrates the schema of at<br />startup<br/><small>[Flyway over JDBC]</small>" .->13
    12-. "Component-scans" .->5

```
<!-- /diagram:Components -->

Each component is one Maven module, and each module has its own root package under `com.wixia.library`.

| Component | Maven module | Responsibility |
|---|---|---|
| REST Adapter | `com.wixia.inbound:adapter-rest` | `LendingController` maps HTTP requests onto use cases. `LendingExceptionHandler` turns exceptions into [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) problem details: 400, 404, and 409 with a `reason`. |
| Inbound Port | `com.wixia.inbound:inbound-port` | Use-case interfaces: `BorrowBookUseCase`, `RenewLoanUseCase`, `ReturnBookUseCase`. |
| Core | `com.wixia:core` | One service per use case. Loads aggregates, applies the lending policy, saves and notifies. |
| Domain | `com.wixia:domain` | `Member`, `BookCopy` and `Loan`. All lending rules are in `LendingPolicy`. |
| Outbound Port | `com.wixia.outbound:outbound-port` | What the core needs from outside: `MemberRepository`, `BookCopyRepository`, `LoanRepository`, `MemberNotifier`. |
| Persistence Adapter | `com.wixia.outbound:adapter-persistence` | JPA repositories over separate entity classes, and the Flyway migrations. |
| Notification Adapter | `com.wixia.outbound:adapter-notification` | `LoggingMemberNotifier`, which writes notifications to the log. |
| Bootstrap | `com.wixia:bootstrap` | Composition root. Starts Spring Boot, wires everything together and owns the transactions. |

### Dependency rules

The arrows between components show both what calls what and which way the compile-time dependencies may go. They all point inwards:

- Adapters depend on ports, never the other way round, and the inbound and outbound sides never depend on each other.
- `core` uses only the two ports. It *implements* the inbound port, so the REST adapter reaches it only through the use-case interfaces.
- `domain` depends on no other module.
- `domain`, the ports and `core` can't use Spring. Spring is used only in the adapters and `bootstrap`.
- `bootstrap` is the only module that sees everything.

`maven-enforcer-plugin` checks these rules in Maven's `validate` phase, including transitive and test-scope dependencies, so a forbidden dependency fails the build. Run `mvn validate` to check them on their own.

### How a borrow goes through the components

1. The **REST adapter** receives `POST /loans`, checks that `memberId` and `copyId` are present, and calls `BorrowBookUseCase`.
2. **Bootstrap** has wired that interface to `BorrowBookService` inside a transaction (`TransactionTemplate`). The core itself knows nothing about transactions.
3. **Core** loads the member with a row lock, so concurrent borrows by one member are checked one after the other. It then loads the copy and the member's active loans through the **outbound port**.
4. The **domain**'s `LendingPolicy` decides whether the loan may start. If not, it throws `LendingRefusedException` with a reason, such as `LOAN_LIMIT_REACHED`.
5. The **persistence adapter** saves the loan. If another request lent the same copy in the meantime, the database's unique index rejects the loan, and the adapter turns that into `COPY_ON_LOAN`.
6. The **notification adapter** is called through `MemberNotifier`, but bootstrap holds the notification back until the transaction commits. Nothing is sent for a borrow that is rolled back.
7. The REST adapter answers `201 Created` with the loan, or with a problem detail if any step refused.

## 4. Deployment

<!-- diagram:LocalDeployment -->
```mermaid
graph LR

    subgraph 36 ["Developer machine"]
      style 36 fill:none,stroke:#444444,color:#444444

      subgraph 37 ["JVM"]
        style 37 fill:none,stroke:#444444,color:#444444

        38["<b>Lending Application</b><br/><small>[Container: Java 27, Spring Boot 4]</small><br/>Spring Boot 4 application<br />built from the Maven reactor<br />and run as<br />bootstrap-1.0-SNAPSHOT.jar."]
        style 38 fill:#438dd5,stroke:#2e6295,color:#ffffff
      end

      subgraph 39 ["Docker or Podman"]
        style 39 fill:none,stroke:#444444,color:#444444

        subgraph 40 ["postgres"]
          style 40 fill:none,stroke:#444444,color:#444444

          41[("<b>Lending Database</b><br/><small>[Container: PostgreSQL 18]</small><br/>Members, book copies and<br />loans. The schema comes from<br />Flyway migrations; a partial<br />unique index allows only one<br />active loan per copy.")]
          style 41 fill:#438dd5,stroke:#2e6295,color:#ffffff
        end

      end

    end

    38-. "Reads from and writes to<br/><small>[JPA/JDBC]</small>" .->41

```
<!-- /diagram:LocalDeployment -->

The only deployment environment is a developer machine:

```bash
docker compose up -d                                 # PostgreSQL on localhost:5432
mvn package -DskipTests
java -jar bootstrap/target/bootstrap-1.0-SNAPSHOT.jar
```

Flyway creates the schema when the application starts. The tests and `mvn spring-boot:test-run -pl bootstrap` use a throwaway PostgreSQL container from Testcontainers instead, which isn't shown here.

## Keeping this page up to date

[`workspace.dsl`](workspace.dsl) is the source of truth. After changing the model, regenerate the diagrams on this page:

```bash
docs/architecture/export-diagrams.sh
```

The script exports each view as Mermaid, using the `structurizr/structurizr` image under Docker or Podman. It writes each diagram between the `<!-- diagram:… -->` markers above, so don't edit the diagrams by hand.

To browse and arrange the views interactively, serve the folder with Structurizr and open http://localhost:8080:

```bash
docker run -it --rm -p 8080:8080 -v "$PWD/docs/architecture:/usr/local/structurizr" structurizr/structurizr local
```

Under rootless Podman, add `--userns=keep-id:uid=65532,gid=65532` so the container can write to the folder. Structurizr saves the layout to `workspace.json`, which is committed. Every view currently uses `autolayout`, so remove that line from a view first if its arranged layout should be kept.

For the reasons behind these design decisions, see [`../dev-log.md`](../dev-log.md).
