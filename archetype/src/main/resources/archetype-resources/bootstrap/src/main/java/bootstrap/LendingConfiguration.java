package ${package}.bootstrap;

import ${package}.core.BorrowBookService;
import ${package}.core.RenewLoanService;
import ${package}.core.ReturnBookService;
import ${package}.domain.loan.LendingPolicy;
import ${package}.inbound.port.BorrowBookUseCase;
import ${package}.inbound.port.RenewLoanUseCase;
import ${package}.inbound.port.ReturnBookUseCase;
import ${package}.outbound.notification.LoggingMemberNotifier;
import ${package}.outbound.persistence.JpaBookCopyRepository;
import ${package}.outbound.persistence.JpaLoanRepository;
import ${package}.outbound.persistence.JpaMemberRepository;
import ${package}.outbound.port.BookCopyRepository;
import ${package}.outbound.port.LoanRepository;
import ${package}.outbound.port.MemberNotifier;
import ${package}.outbound.port.MemberRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.time.Clock;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Plugs the outbound adapters into the use cases. Swapping an adapter means changing a bean here and nothing in the
 * core.
 * <p>
 * Each use case runs in one transaction. The core knows nothing about transactions, so the services are wrapped
 * here rather than annotated with {@code @Transactional}.
 */
@Configuration(proxyBeanMethods = false)
// The JPA entities live in the persistence adapter, outside the packages Boot scans by default.
@EntityScan(basePackageClasses = JpaLoanRepository.class)
class LendingConfiguration {

    /** Delegates to the entity manager of the current transaction, like an injected {@code @PersistenceContext}. */
    @Bean
    EntityManager entityManager(EntityManagerFactory entityManagerFactory) {
        return SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory);
    }

    @Bean
    JpaMemberRepository memberRepository(EntityManager entityManager) {
        return new JpaMemberRepository(entityManager);
    }

    @Bean
    JpaBookCopyRepository bookCopyRepository(EntityManager entityManager) {
        return new JpaBookCopyRepository(entityManager);
    }

    @Bean
    JpaLoanRepository loanRepository(EntityManager entityManager) {
        return new JpaLoanRepository(entityManager);
    }

    /** Notifies only once the use case's transaction has committed. */
    @Bean
    MemberNotifier memberNotifier() {
        return new AfterCommitMemberNotifier(new LoggingMemberNotifier());
    }

    @Bean
    LendingPolicy lendingPolicy() {
        return LendingPolicy.standard();
    }

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    BorrowBookUseCase borrowBook(MemberRepository members, BookCopyRepository copies, LoanRepository loans,
                                 MemberNotifier notifier, LendingPolicy policy, Clock clock,
                                 TransactionTemplate transaction) {
        BorrowBookService service = new BorrowBookService(members, copies, loans, notifier, policy, clock);
        return (memberId, copyId) -> transaction.execute(status -> service.borrow(memberId, copyId));
    }

    @Bean
    ReturnBookUseCase returnBook(LoanRepository loans, Clock clock, TransactionTemplate transaction) {
        ReturnBookService service = new ReturnBookService(loans, clock);
        return copyId -> transaction.execute(status -> service.returnCopy(copyId));
    }

    @Bean
    RenewLoanUseCase renewLoan(MemberRepository members, BookCopyRepository copies, LoanRepository loans,
                               MemberNotifier notifier, LendingPolicy policy, Clock clock,
                               TransactionTemplate transaction) {
        RenewLoanService service = new RenewLoanService(members, copies, loans, notifier, policy, clock);
        return loanId -> transaction.execute(status -> service.renew(loanId));
    }
}
