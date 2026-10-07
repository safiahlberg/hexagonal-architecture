#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
package ${package}.outbound.persistence;

import ${package}.domain.catalog.BookCopyId;
import ${package}.domain.loan.LendingRefusedException;
import ${package}.domain.loan.LendingRefusedException.Reason;
import ${package}.domain.loan.Loan;
import ${package}.domain.loan.LoanId;
import ${package}.domain.member.MemberId;
import ${package}.outbound.port.LoanRepository;

import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;

/**
 * Must be called inside a transaction, which the caller manages.
 */
public final class JpaLoanRepository implements LoanRepository {

    /** The unique index that allows only one active loan per copy, see V1__create_lending_tables.sql. */
    private static final String ONE_ACTIVE_LOAN_PER_COPY = "loans_one_active_per_copy";

    private final EntityManager entityManager;

    public JpaLoanRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public Optional<Loan> findById(LoanId id) {
        return Optional.ofNullable(entityManager.find(LoanEntity.class, id.value())).map(LoanEntity::toLoan);
    }

    @Override
    public List<Loan> findActiveByMember(MemberId memberId) {
        return entityManager.createQuery("""
                        select l from LoanEntity l
                        where l.memberId = :memberId and l.returnedOn is null
                        order by l.borrowedOn""", LoanEntity.class)
                .setParameter("memberId", memberId.value())
                .getResultStream()
                .map(LoanEntity::toLoan)
                .toList();
    }

    @Override
    public Optional<Loan> findActiveByCopy(BookCopyId copyId) {
        return entityManager.createQuery("""
                        select l from LoanEntity l
                        where l.copyId = :copyId and l.returnedOn is null""", LoanEntity.class)
                .setParameter("copyId", copyId.value())
                .getResultStream()
                .findFirst()
                .map(LoanEntity::toLoan);
    }

    /**
     * Flushes right away, so that a copy lent twice by concurrent requests is refused here rather than failing
     * at commit with a database error.
     *
     * @throws LendingRefusedException with {@link Reason${symbol_pound}COPY_ON_LOAN} if another active loan of the copy is stored
     */
    @Override
    public void save(Loan loan) {
        entityManager.merge(LoanEntity.from(loan));
        try {
            entityManager.flush();
        } catch (ConstraintViolationException e) {
            if (ONE_ACTIVE_LOAN_PER_COPY.equals(e.getConstraintName())) {
                throw new LendingRefusedException(Reason.COPY_ON_LOAN);
            }
            throw e;
        }
    }
}
