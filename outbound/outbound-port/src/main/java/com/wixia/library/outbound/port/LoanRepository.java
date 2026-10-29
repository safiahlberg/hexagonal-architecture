package com.wixia.library.outbound.port;

import com.wixia.library.domain.catalog.BookCopyId;
import com.wixia.library.domain.loan.LendingRefusedException;
import com.wixia.library.domain.loan.Loan;
import com.wixia.library.domain.loan.LoanId;
import com.wixia.library.domain.member.MemberId;

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
     * @throws LendingRefusedException with {@link LendingRefusedException.Reason#COPY_ON_LOAN} if the loan is active
     *                                 and another active loan of the same copy is already stored. The use cases
     *                                 check this first, but only the store can catch two concurrent requests.
     */
    void save(Loan loan);
}
