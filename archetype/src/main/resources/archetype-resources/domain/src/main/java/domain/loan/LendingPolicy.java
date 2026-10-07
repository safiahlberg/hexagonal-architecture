package ${package}.domain.loan;

import ${package}.domain.catalog.BookCopyId;
import ${package}.domain.loan.LendingRefusedException.Reason;
import ${package}.domain.member.MemberId;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.Objects;

/**
 * The library's lending rules: who may borrow a copy, for how long, and when a loan may be renewed.
 */
public final class LendingPolicy {

    public static final int DEFAULT_MAX_ACTIVE_LOANS = 5;
    public static final Period DEFAULT_LOAN_PERIOD = Period.ofWeeks(3);

    private final int maxActiveLoans;
    private final Period loanPeriod;

    public LendingPolicy(int maxActiveLoans, Period loanPeriod) {
        if (maxActiveLoans < 1) {
            throw new IllegalArgumentException("maxActiveLoans must be at least 1");
        }
        Objects.requireNonNull(loanPeriod, "loanPeriod");
        if (loanPeriod.isZero() || loanPeriod.isNegative()) {
            throw new IllegalArgumentException("loanPeriod must be positive");
        }
        this.maxActiveLoans = maxActiveLoans;
        this.loanPeriod = loanPeriod;
    }

    public static LendingPolicy standard() {
        return new LendingPolicy(DEFAULT_MAX_ACTIVE_LOANS, DEFAULT_LOAN_PERIOD);
    }

    /**
     * Lends a copy to a member, starting today.
     *
     * @param activeLoansOfMember the member's loans that have not been returned
     * @param copyOnLoan          whether the copy is currently lent to anyone
     * @throws LendingRefusedException if a lending rule forbids the loan
     */
    public Loan lend(LoanId loanId, MemberId memberId, BookCopyId copyId,
                     Collection<Loan> activeLoansOfMember, boolean copyOnLoan, LocalDate today) {
        if (copyOnLoan) {
            throw new LendingRefusedException(Reason.COPY_ON_LOAN);
        }
        if (activeLoansOfMember.stream().anyMatch(loan -> loan.isOverdue(today))) {
            throw new LendingRefusedException(Reason.MEMBER_HAS_OVERDUE_LOANS);
        }
        if (activeLoansOfMember.size() >= maxActiveLoans) {
            throw new LendingRefusedException(Reason.LOAN_LIMIT_REACHED);
        }
        return Loan.start(loanId, memberId, copyId, today, loanPeriod);
    }

    /**
     * Extends a loan by one loan period. A loan can be renewed once, and only while it is not overdue.
     *
     * @throws LendingRefusedException if a lending rule forbids the renewal
     */
    public void renew(Loan loan, LocalDate today) {
        loan.renew(today, loanPeriod);
    }
}
