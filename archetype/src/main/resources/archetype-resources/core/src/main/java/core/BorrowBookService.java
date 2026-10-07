package ${package}.core;

import ${package}.domain.catalog.BookCopy;
import ${package}.domain.catalog.BookCopyId;
import ${package}.domain.loan.LendingPolicy;
import ${package}.domain.loan.Loan;
import ${package}.domain.loan.LoanId;
import ${package}.domain.member.Member;
import ${package}.domain.member.MemberId;
import ${package}.inbound.port.BorrowBookUseCase;
import ${package}.inbound.port.NotFoundException;
import ${package}.outbound.port.BookCopyRepository;
import ${package}.outbound.port.LoanRepository;
import ${package}.outbound.port.MemberNotifier;
import ${package}.outbound.port.MemberRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

public final class BorrowBookService implements BorrowBookUseCase {

    private final MemberRepository members;
    private final BookCopyRepository copies;
    private final LoanRepository loans;
    private final MemberNotifier notifier;
    private final LendingPolicy policy;
    private final Clock clock;

    public BorrowBookService(MemberRepository members, BookCopyRepository copies, LoanRepository loans,
                             MemberNotifier notifier, LendingPolicy policy, Clock clock) {
        this.members = Objects.requireNonNull(members, "members");
        this.copies = Objects.requireNonNull(copies, "copies");
        this.loans = Objects.requireNonNull(loans, "loans");
        this.notifier = Objects.requireNonNull(notifier, "notifier");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public Loan borrow(MemberId memberId, BookCopyId copyId) {
        Member member = members.findById(memberId)
                .orElseThrow(() -> new NotFoundException("No member with id " + memberId.value()));
        BookCopy copy = copies.findById(copyId)
                .orElseThrow(() -> new NotFoundException("No copy with id " + copyId.value()));

        Loan loan = policy.lend(LoanId.newId(), memberId, copyId,
                loans.findActiveByMember(memberId),
                loans.findActiveByCopy(copyId).isPresent(),
                LocalDate.now(clock));

        loans.save(loan);
        notifier.loanConfirmed(member, copy, loan);
        return loan;
    }
}
