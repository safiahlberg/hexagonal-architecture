package ${package}.inbound.port;

import ${package}.domain.catalog.BookCopyId;
import ${package}.domain.loan.LendingRefusedException;
import ${package}.domain.loan.Loan;
import ${package}.domain.member.MemberId;

public interface BorrowBookUseCase {

    /**
     * Lends a copy to a member, starting today.
     *
     * @throws NotFoundException       if the member or the copy does not exist
     * @throws LendingRefusedException if a lending rule forbids the loan
     */
    Loan borrow(MemberId memberId, BookCopyId copyId);
}
