package com.wixia.library.inbound.port;

import com.wixia.library.domain.catalog.BookCopyId;
import com.wixia.library.domain.loan.LendingRefusedException;
import com.wixia.library.domain.loan.Loan;
import com.wixia.library.domain.member.MemberId;

public interface BorrowBookUseCase {

    /**
     * Lends a copy to a member, starting today.
     *
     * @throws NotFoundException       if the member or the copy does not exist
     * @throws LendingRefusedException if a lending rule forbids the loan
     */
    Loan borrow(MemberId memberId, BookCopyId copyId);
}
