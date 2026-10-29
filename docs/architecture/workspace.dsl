workspace "Library Lending" "C4 model of the hexagonal-architecture sample: a library-lending service built with ports and adapters." {

    model {
        client = person "Library client" "Front-desk application or staff tool that lends, renews and takes back book copies on behalf of members."
        member = person "Member" "Library member who borrows book copies and is told about their loans."

        lending = softwareSystem "Library Lending Service" "Lets members borrow, renew and return book copies, enforcing the library's lending rules." {

            app = container "Lending Application" "Spring Boot 4 application built from the Maven reactor and run as bootstrap-1.0-SNAPSHOT.jar. Holds its store in memory." "Java 27, Spring Boot 4" {

                group "Inbound adapters" {
                    restAdapter = component "REST Adapter" "LendingController maps HTTP requests onto use cases; LendingExceptionHandler turns exceptions into RFC 9457 problem details (404, 409 with reason, 400)." "Spring Web MVC, Jackson 3" "Adapter" {
                        properties {
                            "Maven module" "com.wixia.inbound:adapter-rest"
                            "Package" "com.wixia.library.inbound.rest"
                        }
                    }
                }

                group "Hexagon" {
                    inboundPort = component "Inbound Port" "Use-case interfaces BorrowBookUseCase, RenewLoanUseCase and ReturnBookUseCase, plus NotFoundException." "Java interfaces" "Port" {
                        properties {
                            "Maven module" "com.wixia.inbound:inbound-port"
                            "Package" "com.wixia.library.inbound.port"
                        }
                    }
                    core = component "Core" "One service per use case (BorrowBookService, RenewLoanService, ReturnBookService). Loads aggregates, applies the lending policy, saves and notifies. Gets today from an injected Clock." "Plain Java" "Application" {
                        properties {
                            "Maven module" "com.wixia:core"
                            "Package" "com.wixia.library.core"
                        }
                    }
                    domain = component "Domain" "Member, BookCopy and Loan. All lending rules live in LendingPolicy, which refuses with LendingRefusedException and a Reason." "Plain Java" "Domain" {
                        properties {
                            "Maven module" "com.wixia:domain"
                            "Package" "com.wixia.library.domain"
                        }
                    }
                    outboundPort = component "Outbound Port" "Interfaces the core needs: MemberRepository, BookCopyRepository, LoanRepository and MemberNotifier." "Java interfaces" "Port" {
                        properties {
                            "Maven module" "com.wixia.outbound:outbound-port"
                            "Package" "com.wixia.library.outbound.port"
                        }
                    }
                }

                group "Outbound adapters" {
                    persistenceAdapter = component "Persistence Adapter" "In-memory repositories. Loans are stored as LoanRecord snapshots and rebuilt with Loan.restore, like a database would." "Plain Java, in-memory maps" "Adapter" {
                        properties {
                            "Maven module" "com.wixia.outbound:adapter-persistence"
                            "Package" "com.wixia.library.outbound.persistence"
                        }
                    }
                    notificationAdapter = component "Notification Adapter" "LoggingMemberNotifier writes the member's loan confirmations and renewals to the application log." "java.lang.System.Logger" "Adapter" {
                        properties {
                            "Maven module" "com.wixia.outbound:adapter-notification"
                            "Package" "com.wixia.library.outbound.notification"
                        }
                    }
                }

                bootstrap = component "Bootstrap" "Composition root. LibraryApplication starts Spring Boot; LendingConfiguration wires core and outbound adapters with explicit @Bean methods; SampleData seeds the store." "Spring Boot" "Composition root" {
                    properties {
                        "Maven module" "com.wixia:bootstrap"
                        "Package" "com.wixia.library.bootstrap"
                    }
                }
            }
        }

        # Runtime use, and at the same time the allowed compile-time dependency direction: every arrow points inwards.
        client -> restAdapter "Borrows, renews and returns copies using" "JSON/HTTP: POST /loans, /loans/{id}/renewal, /copies/{id}/return"
        restAdapter -> inboundPort "Invokes use cases through"
        restAdapter -> domain "Reads loans and ids from"
        core -> inboundPort "Implements"
        core -> outboundPort "Loads, saves and notifies through"
        core -> domain "Applies lending rules with"
        inboundPort -> domain "Is expressed in terms of"
        outboundPort -> domain "Is expressed in terms of"
        persistenceAdapter -> outboundPort "Implements the repositories of"
        notificationAdapter -> outboundPort "Implements MemberNotifier of"
        notificationAdapter -> member "Tells about confirmed and renewed loans" "Application log (placeholder for e-mail or SMS)"
        bootstrap -> core "Instantiates and wires"
        bootstrap -> persistenceAdapter "Instantiates and seeds"
        bootstrap -> notificationAdapter "Instantiates"
        bootstrap -> restAdapter "Component-scans"

        local = deploymentEnvironment "Local" {
            deploymentNode "Developer machine" "" "Linux, macOS or Windows" {
                deploymentNode "JVM" "" "Java 27" {
                    containerInstance app
                }
            }
        }
    }

    views {
        systemContext lending "SystemContext" {
            include *
            autolayout lr
            description "Who uses the Library Lending Service."
        }

        container lending "Containers" {
            include *
            autolayout lr
            description "The service is a single Spring Boot application with an in-memory store."
        }

        component app "Components" {
            include *
            autolayout lr
            description "Ports and adapters: one component per Maven module. Adapters depend on ports, ports and core on the domain, never the other way round."
        }

        deployment lending local "LocalDeployment" {
            include *
            autolayout lr
            description "Run with java -jar bootstrap/target/bootstrap-1.0-SNAPSHOT.jar or mvn spring-boot:run -pl bootstrap."
        }

        styles {
            element "Person" {
                shape Person
                background #08427b
                color #ffffff
            }
            element "Software System" {
                background #1168bd
                color #ffffff
            }
            element "Container" {
                background #438dd5
                color #ffffff
            }
            element "Component" {
                background #85bbf0
                color #000000
            }
            element "Domain" {
                shape Hexagon
                background #f5c242
            }
            element "Application" {
                shape Hexagon
                background #f8d77a
            }
            element "Port" {
                shape RoundedBox
                background #fbe9b7
            }
            element "Adapter" {
                shape Component
            }
            element "Composition root" {
                background #dddddd
            }
        }
    }

}
