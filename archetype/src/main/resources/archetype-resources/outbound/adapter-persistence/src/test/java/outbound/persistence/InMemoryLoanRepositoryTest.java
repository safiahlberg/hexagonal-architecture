package ${package}.outbound.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ${package}.domain.catalog.BookCopyId;
import ${package}.domain.loan.Loan;
import ${package}.domain.loan.LoanId;
import ${package}.domain.member.MemberId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class InMemoryLoanRepositoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 6);

    private final InMemoryLoanRepository repository = new InMemoryLoanRepository();
    private final MemberId member = MemberId.newId();

    @Test
    void savedLoanCanBeFoundWithAllItsState() {
        Loan loan = Loan.restore(LoanId.newId(), member, BookCopyId.newId(), TODAY, TODAY.plusWeeks(6), true, TODAY.plusDays(2));

        repository.save(loan);
        Loan found = repository.findById(loan.id()).orElseThrow();

        assertEquals(LoanRecord.from(loan), LoanRecord.from(found));
    }

    @Test
    void changesAreKeptOnlyOnceSaved() {
        Loan loan = activeLoan(BookCopyId.newId());
        repository.save(loan);

        Loan found = repository.findById(loan.id()).orElseThrow();
        found.giveBack(TODAY);

        assertTrue(repository.findById(loan.id()).orElseThrow().isActive());
        repository.save(found);
        assertEquals(Optional.of(TODAY), repository.findById(loan.id()).orElseThrow().returnedOn());
    }

    @Test
    void returnedLoansAreNotActive() {
        BookCopyId copy = BookCopyId.newId();
        Loan returned = activeLoan(copy);
        returned.giveBack(TODAY);
        Loan current = activeLoan(BookCopyId.newId());
        repository.save(returned);
        repository.save(current);

        assertEquals(List.of(current), repository.findActiveByMember(member));
        assertEquals(Optional.empty(), repository.findActiveByCopy(copy));
        assertEquals(Optional.of(current), repository.findActiveByCopy(current.copyId()));
    }

    private Loan activeLoan(BookCopyId copy) {
        return Loan.restore(LoanId.newId(), member, copy, TODAY, TODAY.plusWeeks(3), false, null);
    }
}
