package com.wixia.library.domain.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wixia.library.domain.catalog.BookCopyId;
import com.wixia.library.domain.loan.LendingRefusedException.Reason;
import com.wixia.library.domain.member.MemberId;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class LoanTest {

    private static final LocalDate BORROWED = LocalDate.of(2026, 10, 6);
    private static final LocalDate DUE = BORROWED.plusWeeks(3);

    @Test
    void isOverdueOnlyAfterDueDate() {
        Loan loan = activeLoan();

        assertFalse(loan.isOverdue(DUE));
        assertTrue(loan.isOverdue(DUE.plusDays(1)));
    }

    @Test
    void returnedLoanIsNeitherActiveNorOverdue() {
        Loan loan = activeLoan();

        loan.giveBack(DUE.plusDays(10));

        assertFalse(loan.isActive());
        assertFalse(loan.isOverdue(DUE.plusDays(11)));
        assertEquals(Optional.of(DUE.plusDays(10)), loan.returnedOn());
    }

    @Test
    void cannotBeReturnedTwice() {
        Loan loan = activeLoan();
        loan.giveBack(BORROWED);

        LendingRefusedException refused = assertThrows(LendingRefusedException.class, () -> loan.giveBack(BORROWED));
        assertEquals(Reason.ALREADY_RETURNED, refused.reason());
    }

    @Test
    void equalityIsByIdentity() {
        LoanId id = LoanId.newId();
        Loan loan = Loan.restore(id, MemberId.newId(), BookCopyId.newId(), BORROWED, DUE, false, null);
        Loan sameLoanLater = Loan.restore(id, loan.memberId(), loan.copyId(), BORROWED, DUE.plusWeeks(3), true, null);

        assertEquals(loan, sameLoanLater);
    }

    private static Loan activeLoan() {
        return Loan.restore(LoanId.newId(), MemberId.newId(), BookCopyId.newId(), BORROWED, DUE, false, null);
    }
}
