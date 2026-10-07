package com.wixia.library.bootstrap;

import com.wixia.library.core.BorrowBookService;
import com.wixia.library.core.RenewLoanService;
import com.wixia.library.core.ReturnBookService;
import com.wixia.library.domain.loan.LendingPolicy;
import com.wixia.library.outbound.notification.LoggingMemberNotifier;
import com.wixia.library.outbound.persistence.InMemoryBookCopyRepository;
import com.wixia.library.outbound.persistence.InMemoryLoanRepository;
import com.wixia.library.outbound.persistence.InMemoryMemberRepository;
import com.wixia.library.outbound.port.BookCopyRepository;
import com.wixia.library.outbound.port.LoanRepository;
import com.wixia.library.outbound.port.MemberNotifier;
import com.wixia.library.outbound.port.MemberRepository;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Plugs the outbound adapters into the use cases. Swapping an adapter, e.g. for a database, means changing a bean
 * here and nothing in the core.
 */
@Configuration(proxyBeanMethods = false)
class LendingConfiguration {

    @Bean
    InMemoryMemberRepository memberRepository() {
        return new InMemoryMemberRepository();
    }

    @Bean
    InMemoryBookCopyRepository bookCopyRepository() {
        return new InMemoryBookCopyRepository();
    }

    @Bean
    InMemoryLoanRepository loanRepository() {
        return new InMemoryLoanRepository();
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
    BorrowBookService borrowBookService(MemberRepository members, BookCopyRepository copies, LoanRepository loans,
                                        MemberNotifier notifier, LendingPolicy policy, Clock clock) {
        return new BorrowBookService(members, copies, loans, notifier, policy, clock);
    }

    @Bean
    ReturnBookService returnBookService(LoanRepository loans, Clock clock) {
        return new ReturnBookService(loans, clock);
    }

    @Bean
    RenewLoanService renewLoanService(MemberRepository members, BookCopyRepository copies, LoanRepository loans,
                                      MemberNotifier notifier, LendingPolicy policy, Clock clock) {
        return new RenewLoanService(members, copies, loans, notifier, policy, clock);
    }
}
