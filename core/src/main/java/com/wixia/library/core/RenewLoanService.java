package com.wixia.library.core;

import com.wixia.library.domain.catalog.BookCopy;
import com.wixia.library.domain.loan.LendingPolicy;
import com.wixia.library.domain.loan.Loan;
import com.wixia.library.domain.loan.LoanId;
import com.wixia.library.domain.member.Member;
import com.wixia.library.inbound.port.NotFoundException;
import com.wixia.library.inbound.port.RenewLoanUseCase;
import com.wixia.library.outbound.port.BookCopyRepository;
import com.wixia.library.outbound.port.LoanRepository;
import com.wixia.library.outbound.port.MemberNotifier;
import com.wixia.library.outbound.port.MemberRepository;

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
