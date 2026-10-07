package ${package}.inbound.port;

import ${package}.domain.loan.LendingRefusedException;
import ${package}.domain.loan.Loan;
import ${package}.domain.loan.LoanId;

public interface RenewLoanUseCase {

    /**
     * Extends a loan by one loan period.
     *
     * @throws NotFoundException       if the loan does not exist
     * @throws LendingRefusedException if a lending rule forbids the renewal
     */
    Loan renew(LoanId loanId);
}
