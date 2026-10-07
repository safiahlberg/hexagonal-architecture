package ${package}.outbound.port;

import ${package}.domain.catalog.BookCopyId;
import ${package}.domain.loan.Loan;
import ${package}.domain.loan.LoanId;
import ${package}.domain.member.MemberId;

import java.util.List;
import java.util.Optional;

public interface LoanRepository {

    Optional<Loan> findById(LoanId id);

    /** The member's loans that have not been returned. */
    List<Loan> findActiveByMember(MemberId memberId);

    /** The copy's loan that has not been returned, if it is on loan. */
    Optional<Loan> findActiveByCopy(BookCopyId copyId);

    /** Stores a new loan, or replaces the stored state of an existing one. */
    void save(Loan loan);
}
