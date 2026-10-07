package ${package}.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Component scanning covers only the modules written for Spring. The core and the outbound adapters are
 * framework-free and are wired explicitly in {@link LendingConfiguration}.
 */
@SpringBootApplication(scanBasePackages = {
        "${package}.bootstrap",
        "${package}.inbound.rest"
})
public class LibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
    }
}
