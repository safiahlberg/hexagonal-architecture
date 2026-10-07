# Hexagonal architecture sample

A small library-lending service built with the hexagonal (ports and adapters) architecture in Java 27 and Maven. Members borrow, renew and return book copies over a REST API.

It runs as a Spring Boot 4 app, but Spring stays in the outer modules. The domain, the ports and the use cases are plain Java, and the build fails if any module depends on something it shouldn't.

## Requirements

- JDK 27
- Maven (there is no wrapper; use the system `mvn`)

## Build and run

```bash
mvn clean verify                                    # build, check dependency rules, run all tests
mvn package -DskipTests
java -jar bootstrap/target/bootstrap-1.0-SNAPSHOT.jar
```

The app listens on port 8080. Data is kept in memory and is lost on restart.

## Trying the API

The app starts with sample data that has fixed ids, which are also logged at startup:

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
  adapter-persistence/           in-memory repositories
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

`core` and the outbound adapters have no Spring annotations; `bootstrap` wires them with explicit `@Bean` methods in `LendingConfiguration`. Replacing an adapter, such as the in-memory store with a database, means writing a new module under `outbound/` and changing one bean.

## Further reading

- [`docs/dev-log.md`](docs/dev-log.md) records how the project was restructured and why the design decisions were made.
- [`CLAUDE.md`](CLAUDE.md) has more detail on conventions, testing and the Spring setup.
- [`archetype/`](archetype/README.md) is a Maven archetype that generates a new project from this sample, under your own groupId and package.
