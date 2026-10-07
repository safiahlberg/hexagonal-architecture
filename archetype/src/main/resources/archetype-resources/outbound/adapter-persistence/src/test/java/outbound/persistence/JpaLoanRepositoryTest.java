package ${package}.outbound.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ${package}.domain.catalog.BookCopy;
import ${package}.domain.catalog.BookCopyId;
import ${package}.domain.loan.LendingRefusedException;
import ${package}.domain.loan.LendingRefusedException.Reason;
import ${package}.domain.loan.Loan;
import ${package}.domain.loan.LoanId;
import ${package}.domain.member.Member;
import ${package}.domain.member.MemberId;

import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Each test runs in a transaction that is rolled back afterwards. The entity manager is cleared before reading, so
 * reads come from the database rather than from its cache.
 */
class JpaLoanRepositoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 6);

    private final MemberId member = MemberId.newId();
    private EntityManager entityManager;
    private JpaLoanRepository repository;

    @BeforeEach
    void beginTransaction() {
        entityManager = TestDatabase.entityManagerFactory().createEntityManager();
        entityManager.getTransaction().begin();
        repository = new JpaLoanRepository(entityManager);
        new JpaMemberRepository(entityManager).add(new Member(member, "Ada", "ada@example.com"));
    }

    @AfterEach
    void rollBack() {
        entityManager.getTransaction().rollback();
        entityManager.close();
    }

    @Test
    void savedLoanCanBeFoundWithAllItsState() {
        Loan loan = Loan.restore(LoanId.newId(), member, newCopy(), TODAY, TODAY.plusWeeks(6), true, TODAY.plusDays(2));

        repository.save(loan);
        entityManager.clear();
        Loan found = repository.findById(loan.id()).orElseThrow();

        assertEquals(state(loan), state(found));
    }

    @Test
    void changesAreKeptOnlyOnceSaved() {
        Loan loan = activeLoan(newCopy());
        repository.save(loan);
        entityManager.clear();

        Loan found = repository.findById(loan.id()).orElseThrow();
        found.giveBack(TODAY);

        assertTrue(repository.findById(loan.id()).orElseThrow().isActive());
        repository.save(found);
        entityManager.clear();
        assertEquals(Optional.of(TODAY), repository.findById(loan.id()).orElseThrow().returnedOn());
    }

    @Test
    void returnedLoansAreNotActive() {
        BookCopyId copy = newCopy();
        Loan returned = activeLoan(copy);
        returned.giveBack(TODAY);
        Loan current = activeLoan(newCopy());
        repository.save(returned);
        repository.save(current);
        entityManager.clear();

        assertEquals(List.of(current), repository.findActiveByMember(member));
        assertEquals(Optional.empty(), repository.findActiveByCopy(copy));
        assertEquals(Optional.of(current), repository.findActiveByCopy(current.copyId()));
    }

    @Test
    void secondActiveLoanOfACopyIsRefusedByTheDatabase() {
        BookCopyId copy = newCopy();
        repository.save(activeLoan(copy));

        // As if a concurrent request had passed LendingPolicy's check before the first loan was stored.
        LendingRefusedException refused = assertThrows(LendingRefusedException.class,
                () -> repository.save(activeLoan(copy)));

        assertEquals(Reason.COPY_ON_LOAN, refused.reason());
    }

    @Test
    void returnedLoanDoesNotBlockTheCopy() {
        BookCopyId copy = newCopy();
        Loan returned = activeLoan(copy);
        returned.giveBack(TODAY);
        repository.save(returned);

        Loan next = activeLoan(copy);
        repository.save(next);
        entityManager.clear();

        assertEquals(Optional.of(next), repository.findActiveByCopy(copy));
    }

    private BookCopyId newCopy() {
        BookCopyId id = BookCopyId.newId();
        new JpaBookCopyRepository(entityManager).add(new BookCopy(id, "978-0-441-17271-9", "Dune"));
        return id;
    }

    private Loan activeLoan(BookCopyId copy) {
        return Loan.restore(LoanId.newId(), member, copy, TODAY, TODAY.plusWeeks(3), false, null);
    }

    private static List<Object> state(Loan loan) {
        return List.of(loan.id(), loan.memberId(), loan.copyId(), loan.borrowedOn(), loan.dueOn(), loan.isRenewed(),
                loan.returnedOn());
    }
}
