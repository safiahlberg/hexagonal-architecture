package com.wixia.library.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wixia.library.domain.catalog.BookCopy;
import com.wixia.library.domain.catalog.BookCopyId;
import com.wixia.library.domain.loan.LendingPolicy;
import com.wixia.library.domain.loan.LendingRefusedException;
import com.wixia.library.domain.loan.LendingRefusedException.Reason;
import com.wixia.library.domain.loan.Loan;
import com.wixia.library.domain.loan.LoanId;
import com.wixia.library.domain.member.Member;
import com.wixia.library.domain.member.MemberId;
import com.wixia.library.inbound.port.NotFoundException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

class LendingServicesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 6);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-06T10:00:00Z"), ZoneOffset.UTC);

    private final FakeLibrary library = new FakeLibrary();
    private final LendingPolicy policy = LendingPolicy.standard();

    private final BorrowBookService borrow = new BorrowBookService(library, library, library, library, policy, CLOCK);
    private final ReturnBookService giveBack = new ReturnBookService(library, CLOCK);
    private final RenewLoanService renew = new RenewLoanService(library, library, library, library, policy, CLOCK);

    private final Member ada = library.addMember("Ada");
    private final Member bob = library.addMember("Bob");
    private final BookCopy dune = library.addCopy("Dune");

    @Test
    void borrowingStoresLoanAndNotifiesMember() {
        Loan loan = borrow.borrow(ada.id(), dune.id());

        assertEquals(TODAY, loan.borrowedOn());
        assertEquals(loan, library.loans.get(loan.id()));
        assertEquals(List.of("confirmed: Ada / Dune"), library.notifications);
    }

    @Test
    void borrowingCopyOnLoanIsRefusedAndHasNoEffect() {
        borrow.borrow(ada.id(), dune.id());

        LendingRefusedException refused = assertThrows(LendingRefusedException.class, () -> borrow.borrow(bob.id(), dune.id()));

        assertEquals(Reason.COPY_ON_LOAN, refused.reason());
        assertEquals(1, library.loans.size());
        assertEquals(1, library.notifications.size());
    }

    @Test
    void borrowingNeedsKnownMemberAndCopy() {
        assertThrows(NotFoundException.class, () -> borrow.borrow(MemberId.newId(), dune.id()));
        assertThrows(NotFoundException.class, () -> borrow.borrow(ada.id(), BookCopyId.newId()));
    }

    @Test
    void returningEndsLoanAndFreesCopy() {
        Loan loan = borrow.borrow(ada.id(), dune.id());

        Loan returned = giveBack.returnCopy(dune.id());

        assertEquals(loan.id(), returned.id());
        assertFalse(returned.isActive());
        assertEquals(dune.id(), borrow.borrow(bob.id(), dune.id()).copyId());
    }

    @Test
    void returningCopyNotOnLoanIsNotFound() {
        assertThrows(NotFoundException.class, () -> giveBack.returnCopy(dune.id()));
    }

    @Test
    void renewingExtendsLoanAndNotifiesMember() {
        Loan loan = borrow.borrow(ada.id(), dune.id());

        Loan renewed = renew.renew(loan.id());

        assertTrue(renewed.isRenewed());
        assertEquals(TODAY.plus(LendingPolicy.DEFAULT_LOAN_PERIOD).plus(LendingPolicy.DEFAULT_LOAN_PERIOD), renewed.dueOn());
        assertEquals("renewed: Ada / Dune", library.notifications.getLast());
    }

    @Test
    void renewingUnknownLoanIsNotFound() {
        assertThrows(NotFoundException.class, () -> renew.renew(LoanId.newId()));
    }
}
