#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
package ${package}.domain.loan;

import ${package}.domain.catalog.BookCopyId;
import ${package}.domain.loan.LendingRefusedException.Reason;
import ${package}.domain.member.MemberId;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;
import java.util.Optional;

/**
 * A copy lent to a member. New loans are created only through {@link LendingPolicy${symbol_pound}lend}.
 */
public final class Loan {

    private final LoanId id;
    private final MemberId memberId;
    private final BookCopyId copyId;
    private final LocalDate borrowedOn;
    private LocalDate dueOn;
    private boolean renewed;
    private LocalDate returnedOn;

    private Loan(LoanId id, MemberId memberId, BookCopyId copyId, LocalDate borrowedOn, LocalDate dueOn,
                 boolean renewed, LocalDate returnedOn) {
        this.id = Objects.requireNonNull(id, "id");
        this.memberId = Objects.requireNonNull(memberId, "memberId");
        this.copyId = Objects.requireNonNull(copyId, "copyId");
        this.borrowedOn = Objects.requireNonNull(borrowedOn, "borrowedOn");
        this.dueOn = Objects.requireNonNull(dueOn, "dueOn");
        this.renewed = renewed;
        this.returnedOn = returnedOn;
    }

    static Loan start(LoanId id, MemberId memberId, BookCopyId copyId, LocalDate today, Period loanPeriod) {
        return new Loan(id, memberId, copyId, today, today.plus(loanPeriod), false, null);
    }

    /**
     * Rebuilds a loan from stored state. Meant for persistence adapters: it applies no lending rules.
     */
    public static Loan restore(LoanId id, MemberId memberId, BookCopyId copyId, LocalDate borrowedOn,
                               LocalDate dueOn, boolean renewed, LocalDate returnedOn) {
        return new Loan(id, memberId, copyId, borrowedOn, dueOn, renewed, returnedOn);
    }

    void renew(LocalDate today, Period loanPeriod) {
        if (!isActive()) {
            throw new LendingRefusedException(Reason.ALREADY_RETURNED);
        }
        if (renewed) {
            throw new LendingRefusedException(Reason.ALREADY_RENEWED);
        }
        if (isOverdue(today)) {
            throw new LendingRefusedException(Reason.LOAN_OVERDUE);
        }
        dueOn = dueOn.plus(loanPeriod);
        renewed = true;
    }

    public void giveBack(LocalDate today) {
        if (!isActive()) {
            throw new LendingRefusedException(Reason.ALREADY_RETURNED);
        }
        returnedOn = Objects.requireNonNull(today, "today");
    }

    public boolean isActive() {
        return returnedOn == null;
    }

    public boolean isOverdue(LocalDate today) {
        return isActive() && today.isAfter(dueOn);
    }

    public LoanId id() {
        return id;
    }

    public MemberId memberId() {
        return memberId;
    }

    public BookCopyId copyId() {
        return copyId;
    }

    public LocalDate borrowedOn() {
        return borrowedOn;
    }

    public LocalDate dueOn() {
        return dueOn;
    }

    public boolean isRenewed() {
        return renewed;
    }

    public Optional<LocalDate> returnedOn() {
        return Optional.ofNullable(returnedOn);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Loan loan && id.equals(loan.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Loan[id=%s, memberId=%s, copyId=%s, dueOn=%s, returnedOn=%s]"
                .formatted(id.value(), memberId.value(), copyId.value(), dueOn, returnedOn);
    }
}
