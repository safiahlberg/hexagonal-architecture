package com.wixia.library.inbound.port;

import com.wixia.library.domain.loan.LendingRefusedException;
import com.wixia.library.domain.loan.Loan;
import com.wixia.library.domain.loan.LoanId;

public interface RenewLoanUseCase {

    /**
     * Extends a loan by one loan period.
     *
     * @throws NotFoundException       if the loan does not exist
     * @throws LendingRefusedException if a lending rule forbids the renewal
     */
    Loan renew(LoanId loanId);
}
