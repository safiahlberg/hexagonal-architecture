# Hexagonal architecture sample

A small library-lending service built with the hexagonal (ports and adapters) architecture in Java 27 and Maven. Members borrow, renew and return book copies over a REST API.

It runs as a Spring Boot 4 app, but Spring stays in the outer modules. The domain, the ports and the use cases are plain Java, and the build fails if any module depends on something it shouldn't.

## Requirements

- JDK 27
- Maven (there is no wrapper; use the system `mvn`)
- Docker, or Podman with its socket running: the tests start PostgreSQL in a container with Testcontainers, and `compose.yaml` runs it for the app

## Build and run

```bash
mvn clean verify                                    # build, check dependency rules, run all tests
docker compose up -d                                # PostgreSQL on localhost:5432
mvn package -DskipTests
java -jar bootstrap/target/bootstrap-1.0-SNAPSHOT.jar
```

The app listens on port 8080 and keeps its data in PostgreSQL. Flyway creates the schema at startup. To use another database, set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`. `docker compose down -v` deletes the data.

To try the app without setting up a database, run it against a throwaway container that is removed when the app stops:

```bash
mvn install -DskipTests
mvn spring-boot:test-run -pl bootstrap
```

## Trying the API

At every start the app writes sample data with fixed ids, which are also logged:

| Kind   | Name                      | Id                                     |
|--------|---------------------------|----------------------------------------|
| Member | Ada                       | `11111111-1111-1111-1111-111111111111` |
| Member | Bob                       | `22222222-2222-2222-2222-222222222222` |
| Copy   | Dune                      | `c0000000-0000-0000-0000-000000000001` |
| Copy   | Neuromancer               | `c0000000-0000-0000-0000-000000000002` |
| Copy   | The Left Hand of Darkness | `c0000000-0000-0000-0000-000000000003` |

| Method and path                  | What it does                                   |
|----------------------------------|------------------------------------------------|
| `POST /loans`                    | Borrow a copy. Body: `{"memberId": …, "copyId": …}`. Returns 201 with the loan. |
| `POST /loans/{loanId}/renewal`   | Renew a loan by one loan period.              |
| `POST /copies/{copyId}/return`   | Return a copy and close its active loan.       |

```bash
# Ada borrows Dune
curl -i -X POST localhost:8080/loans \
     -H 'Content-Type: application/json' \
     -d '{"memberId":"11111111-1111-1111-1111-111111111111","copyId":"c0000000-0000-0000-0000-000000000001"}'

# Renew it, using the id from the response above
curl -i -X POST localhost:8080/loans/<loanId>/renewal

# Return it
curl -i -X POST localhost:8080/copies/c0000000-0000-0000-0000-000000000001/return
```

Errors are returned as [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) problem details:

- **400** — the request is malformed or incomplete. The `detail` names the missing or invalid field.
- **404** — the member, copy or loan doesn't exist.
- **409** — a lending rule refused the request. The `reason` property says which one.

## Lending rules

All rules live in `LendingPolicy` in the domain module:

- A copy can be lent to only one member at a time (`COPY_ON_LOAN`).
- A member can have at most 5 active loans (`LOAN_LIMIT_REACHED`).
- A member with an overdue loan can't borrow (`MEMBER_HAS_OVERDUE_LOANS`).
- A loan lasts 3 weeks and can be renewed once (`ALREADY_RENEWED`), and only while it isn't overdue (`LOAN_OVERDUE`).
- A returned loan can't be renewed (`ALREADY_RETURNED`). Returning a copy that isn't on loan gives a 404.

## Modules

```
domain/                          entities and lending rules, no dependencies
core/                            one service per use case
inbound/
  inbound-port/                  use-case interfaces (BorrowBookUseCase, ...)
  adapter-rest/                  Spring MVC controller
outbound/
  outbound-port/                 repository and notifier interfaces
  adapter-persistence/           JPA repositories on PostgreSQL, Flyway migrations
  adapter-notification/          notifier that logs
bootstrap/                       Spring Boot app, wires everything together
```

Dependencies point inward: adapters depend on ports, `core` depends only on the ports, and `domain` depends on nothing internal. `bootstrap` is the composition root and the only module that sees everything.

`maven-enforcer-plugin` checks these rules in the `validate` phase, including transitive and test-scope dependencies:

- The inbound and outbound sides can't depend on each other or on `core`.
- A port can't depend on an adapter.
- `core` can use only the two port modules.
- `domain`, the ports and `core` can't use Spring.

Run `mvn validate` to check the rules on their own. A new adapter placed under `inbound/` or `outbound/` inherits its side's rules automatically.

`core` and the outbound adapters have no Spring annotations; `bootstrap` wires them with explicit `@Bean` methods in `LendingConfiguration`, and runs each use case in a transaction there. Replacing an adapter means writing a new module under `outbound/` and changing its beans.

## Starting a new project from this sample

[`archetype/`](archetype/README.md) is a Maven archetype that copies this project under your own coordinates and package. Install it into your local repository once:

```bash
cd archetype
mvn install
```

Then generate a project from the directory where it should go:

```bash
mvn archetype:generate \
    -DarchetypeGroupId=com.wixia \
    -DarchetypeArtifactId=hexagonal-architecture-archetype \
    -DarchetypeVersion=1.0-SNAPSHOT \
    -DgroupId=org.example.shop \
    -DartifactId=lending-service \
    -Dversion=0.1-SNAPSHOT \
    -Dpackage=org.example.shop.lending \
    -B
cd lending-service
mvn clean verify
```

- `groupId` is used for the root, `domain`, `core` and `bootstrap`. The inbound and outbound sides get `<groupId>.inbound` and `<groupId>.outbound`, and the enforcer rules are adjusted to match.
- `artifactId` names the root project and its directory. `package` is the root package and defaults to `groupId`.
- Module names and class names are the same as here. The lending code is a worked example: replace it with your own domain, keeping the modules and their rules.
- Without `-B`, Maven asks you to confirm the properties, including one called `gitignore`. Accept its default, `.gitignore`.

The generated project builds and runs as is. If you change this sample, regenerate the archetype first, as described in [`archetype/README.md`](archetype/README.md).

## Further reading

- [`docs/architecture/`](docs/architecture/README.md) describes the architecture as a [C4 model](https://c4model.com), with system context, container, component (one per module) and deployment diagrams generated from a [Structurizr DSL](https://docs.structurizr.com/dsl) model.
- [`docs/dev-log.md`](docs/dev-log.md) records how the project was restructured and why the design decisions were made.
- [`CLAUDE.md`](CLAUDE.md) has more detail on conventions, testing and the Spring setup.
- [`archetype/`](archetype/README.md) is a Maven archetype that generates a new project from this sample, under your own groupId and package.
