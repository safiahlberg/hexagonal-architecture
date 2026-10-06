package com.wixia.library.inbound.port;

import com.wixia.library.domain.catalog.BookCopyId;
import com.wixia.library.domain.loan.Loan;

public interface ReturnBookUseCase {

    /**
     * Ends the current loan of a copy. Identified by copy, since that is what is handed in at the desk.
     *
     * @throws NotFoundException if the copy is not on loan
     */
    Loan returnCopy(BookCopyId copyId);
}
