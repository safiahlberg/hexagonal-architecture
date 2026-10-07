package ${package}.inbound.port;

import ${package}.domain.catalog.BookCopyId;
import ${package}.domain.loan.Loan;

public interface ReturnBookUseCase {

    /**
     * Ends the current loan of a copy. Identified by copy, since that is what is handed in at the desk.
     *
     * @throws NotFoundException if the copy is not on loan
     */
    Loan returnCopy(BookCopyId copyId);
}
