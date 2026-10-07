package com.wixia.library.domain.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wixia.library.domain.catalog.BookCopyId;
import com.wixia.library.domain.loan.LendingRefusedException.Reason;
import com.wixia.library.domain.member.MemberId;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.junit.jupiter.api.Test;

class LendingPolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 6);

    private final LendingPolicy policy = new LendingPolicy(2, Period.ofWeeks(3));
    private final MemberId member = MemberId.newId();

    @Test
    void lendsAvailableCopyForOneLoanPeriod() {
        BookCopyId copy = BookCopyId.newId();

        Loan loan = policy.lend(LoanId.newId(), member, copy, List.of(), false, TODAY);

        assertEquals(member, loan.memberId());
        assertEquals(copy, loan.copyId());
        assertEquals(TODAY, loan.borrowedOn());
        assertEquals(TODAY.plusWeeks(3), loan.dueOn());
        assertTrue(loan.isActive());
        assertFalse(loan.isRenewed());
    }

    @Test
    void refusesCopyThatIsOnLoan() {
        assertRefused(Reason.COPY_ON_LOAN, () -> policy.lend(LoanId.newId(), member, BookCopyId.newId(), List.of(), true, TODAY));
    }

    @Test
    void refusesMemberAtLoanLimit() {
        List<Loan> current = List.of(loanBorrowedOn(TODAY), loanBorrowedOn(TODAY));

        assertRefused(Reason.LOAN_LIMIT_REACHED, () -> policy.lend(LoanId.newId(), member, BookCopyId.newId(), current, false, TODAY));
    }

    @Test
    void refusesMemberWithOverdueLoan() {
        List<Loan> current = List.of(loanBorrowedOn(TODAY.minusWeeks(4)));

        assertRefused(Reason.MEMBER_HAS_OVERDUE_LOANS, () -> policy.lend(LoanId.newId(), member, BookCopyId.newId(), current, false, TODAY));
    }

    @Test
    void renewalExtendsDueDateByOneLoanPeriod() {
        Loan loan = loanBorrowedOn(TODAY);

        policy.renew(loan, TODAY.plusWeeks(1));

        assertEquals(TODAY.plusWeeks(6), loan.dueOn());
        assertTrue(loan.isRenewed());
    }

    @Test
    void loanCanBeRenewedOnItsDueDate() {
        Loan loan = loanBorrowedOn(TODAY);

        policy.renew(loan, loan.dueOn());

        assertTrue(loan.isRenewed());
    }

    @Test
    void refusesSecondRenewal() {
        Loan loan = loanBorrowedOn(TODAY);
        policy.renew(loan, TODAY);

        assertRefused(Reason.ALREADY_RENEWED, () -> policy.renew(loan, TODAY));
    }

    @Test
    void refusesRenewalOfOverdueLoan() {
        Loan loan = loanBorrowedOn(TODAY);

        assertRefused(Reason.LOAN_OVERDUE, () -> policy.renew(loan, loan.dueOn().plusDays(1)));
    }

    @Test
    void refusesRenewalOfReturnedLoan() {
        Loan loan = loanBorrowedOn(TODAY);
        loan.giveBack(TODAY);

        assertRefused(Reason.ALREADY_RETURNED, () -> policy.renew(loan, TODAY));
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new LendingPolicy(0, Period.ofWeeks(3)));
        assertThrows(IllegalArgumentException.class, () -> new LendingPolicy(1, Period.ZERO));
    }

    private Loan loanBorrowedOn(LocalDate day) {
        return policy.lend(LoanId.newId(), member, BookCopyId.newId(), List.of(), false, day);
    }

    private static void assertRefused(Reason expected, Runnable action) {
        LendingRefusedException refused = assertThrows(LendingRefusedException.class, action::run);
        assertEquals(expected, refused.reason());
    }
}
