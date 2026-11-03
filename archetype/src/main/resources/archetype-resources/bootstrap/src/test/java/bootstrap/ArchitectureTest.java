#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
package ${package}.bootstrap;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import ${package}.domain.catalog.BookCopyId;
import ${package}.domain.loan.Loan;
import ${package}.domain.loan.LoanId;
import ${package}.domain.member.MemberId;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * Rules the enforcer can't check. It sees only which modules depend on each other, and the inbound adapters and the
 * outbound adapters both need the domain, so these rules say which of its operations they may call.
 */
class ArchitectureTest {

    private static final String ROOT = "${package}";

    /**
     * This module's test classes, the only ones on its class path. ArchUnit's own {@code DoNotIncludeTests} won't do:
     * it matches {@code target/test-classes} anywhere in a path, and the archetype's integration test builds its
     * generated project inside such a directory, so it would exclude every class.
     */
    private static final String TEST_CLASSES =
            ArchitectureTest.class.getProtectionDomain().getCodeSource().getLocation().toString();

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(location -> !location.asURI().toString().startsWith(TEST_CLASSES))
            .importPackages(ROOT);

    /** A loan changes only inside a use case. Inbound adapters get a {@link Loan} back from one, but only read it. */
    @Test
    void onlyTheCoreReturnsLoans() {
        noClasses().that().resideOutsideOfPackage(ROOT + ".core..")
                .should().callMethod(Loan.class, "giveBack", LocalDate.class)
                .check(PRODUCTION_CLASSES);
    }

    /** {@link Loan${symbol_pound}restore} applies no lending rules, so only an outbound adapter rebuilding a stored loan may use it. */
    @Test
    void onlyOutboundAdaptersRestoreLoans() {
        noClasses().that().resideOutsideOfPackage(ROOT + ".outbound..")
                .should().callMethod(Loan.class, "restore", LoanId.class, MemberId.class, BookCopyId.class,
                        LocalDate.class, LocalDate.class, boolean.class, LocalDate.class)
                .check(PRODUCTION_CLASSES);
    }
}
