package ${package}.inbound.rest;

import ${package}.domain.loan.Loan;

import java.time.LocalDate;
import java.util.UUID;

record LoanResponse(UUID id, UUID memberId, UUID copyId, LocalDate borrowedOn, LocalDate dueOn,
                    boolean renewed, LocalDate returnedOn) {

    static LoanResponse from(Loan loan) {
        return new LoanResponse(loan.id().value(), loan.memberId().value(), loan.copyId().value(),
                loan.borrowedOn(), loan.dueOn(), loan.isRenewed(), loan.returnedOn().orElse(null));
    }
}
