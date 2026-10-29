#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
package ${package}.outbound.port;

import ${package}.domain.catalog.BookCopyId;
import ${package}.domain.loan.LendingRefusedException;
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

    /**
     * Stores a new loan, or replaces the stored state of an existing one.
     *
     * @throws LendingRefusedException with {@link LendingRefusedException.Reason${symbol_pound}COPY_ON_LOAN} if the loan is active
     *                                 and another active loan of the same copy is already stored. The use cases
     *                                 check this first, but only the store can catch two concurrent requests.
     */
    void save(Loan loan);
}
