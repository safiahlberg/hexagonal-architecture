package com.wixia.library.core;

import com.wixia.library.domain.catalog.BookCopy;
import com.wixia.library.domain.catalog.BookCopyId;
import com.wixia.library.domain.loan.LendingPolicy;
import com.wixia.library.domain.loan.Loan;
import com.wixia.library.domain.loan.LoanId;
import com.wixia.library.domain.member.Member;
import com.wixia.library.domain.member.MemberId;
import com.wixia.library.inbound.port.BorrowBookUseCase;
import com.wixia.library.inbound.port.NotFoundException;
import com.wixia.library.outbound.port.BookCopyRepository;
import com.wixia.library.outbound.port.LoanRepository;
import com.wixia.library.outbound.port.MemberNotifier;
import com.wixia.library.outbound.port.MemberRepository;

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
