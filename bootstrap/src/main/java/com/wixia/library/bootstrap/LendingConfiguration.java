package com.wixia.library.bootstrap;

import com.wixia.library.core.BorrowBookService;
import com.wixia.library.core.RenewLoanService;
import com.wixia.library.core.ReturnBookService;
import com.wixia.library.domain.loan.LendingPolicy;
import com.wixia.library.inbound.port.BorrowBookUseCase;
import com.wixia.library.inbound.port.RenewLoanUseCase;
import com.wixia.library.inbound.port.ReturnBookUseCase;
import com.wixia.library.outbound.notification.LoggingMemberNotifier;
import com.wixia.library.outbound.persistence.JpaBookCopyRepository;
import com.wixia.library.outbound.persistence.JpaLoanRepository;
import com.wixia.library.outbound.persistence.JpaMemberRepository;
import com.wixia.library.outbound.port.BookCopyRepository;
import com.wixia.library.outbound.port.LoanRepository;
import com.wixia.library.outbound.port.MemberNotifier;
import com.wixia.library.outbound.port.MemberRepository;

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

    @Bean
    LoggingMemberNotifier memberNotifier() {
        return new LoggingMemberNotifier();
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
