package ${package}.bootstrap;

import org.springframework.boot.SpringApplication;

/**
 * Runs the app against a throwaway database in a container, with no database to set up:
 * {@code mvn spring-boot:test-run -pl bootstrap}. The data is gone when the app stops.
 */
class TestLibraryApplication {

    public static void main(String[] args) {
        SpringApplication.from(LibraryApplication::main).with(TestDatabaseConfiguration.class).run(args);
    }
}
