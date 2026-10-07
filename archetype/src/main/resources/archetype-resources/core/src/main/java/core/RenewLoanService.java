package ${package}.core;

import ${package}.domain.catalog.BookCopy;
import ${package}.domain.loan.LendingPolicy;
import ${package}.domain.loan.Loan;
import ${package}.domain.loan.LoanId;
import ${package}.domain.member.Member;
import ${package}.inbound.port.NotFoundException;
import ${package}.inbound.port.RenewLoanUseCase;
import ${package}.outbound.port.BookCopyRepository;
import ${package}.outbound.port.LoanRepository;
import ${package}.outbound.port.MemberNotifier;
import ${package}.outbound.port.MemberRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

public final class RenewLoanService implements RenewLoanUseCase {

    private final MemberRepository members;
    private final BookCopyRepository copies;
    private final LoanRepository loans;
    private final MemberNotifier notifier;
    private final LendingPolicy policy;
    private final Clock clock;

    public RenewLoanService(MemberRepository members, BookCopyRepository copies, LoanRepository loans,
                            MemberNotifier notifier, LendingPolicy policy, Clock clock) {
        this.members = Objects.requireNonNull(members, "members");
        this.copies = Objects.requireNonNull(copies, "copies");
        this.loans = Objects.requireNonNull(loans, "loans");
        this.notifier = Objects.requireNonNull(notifier, "notifier");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public Loan renew(LoanId loanId) {
        Loan loan = loans.findById(loanId)
                .orElseThrow(() -> new NotFoundException("No loan with id " + loanId.value()));

        policy.renew(loan, LocalDate.now(clock));
        loans.save(loan);

        // A loan always refers to an existing member and copy, so a miss here is a data error, not a bad request.
        Member member = members.findById(loan.memberId()).orElseThrow();
        BookCopy copy = copies.findById(loan.copyId()).orElseThrow();
        notifier.loanRenewed(member, copy, loan);
        return loan;
    }
}
