package ${package}.bootstrap;

import ${package}.core.BorrowBookService;
import ${package}.core.RenewLoanService;
import ${package}.core.ReturnBookService;
import ${package}.domain.loan.LendingPolicy;
import ${package}.outbound.notification.LoggingMemberNotifier;
import ${package}.outbound.persistence.InMemoryBookCopyRepository;
import ${package}.outbound.persistence.InMemoryLoanRepository;
import ${package}.outbound.persistence.InMemoryMemberRepository;
import ${package}.outbound.port.BookCopyRepository;
import ${package}.outbound.port.LoanRepository;
import ${package}.outbound.port.MemberNotifier;
import ${package}.outbound.port.MemberRepository;

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
