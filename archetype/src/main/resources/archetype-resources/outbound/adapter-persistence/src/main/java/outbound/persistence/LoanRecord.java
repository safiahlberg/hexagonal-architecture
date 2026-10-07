package ${package}.outbound.persistence;

import ${package}.domain.catalog.BookCopyId;
import ${package}.domain.loan.Loan;
import ${package}.domain.loan.LoanId;
import ${package}.domain.member.MemberId;

import java.time.LocalDate;
import java.util.UUID;

/**
 * The stored form of a loan, as a database row would hold it.
 */
record LoanRecord(UUID id, UUID memberId, UUID copyId, LocalDate borrowedOn, LocalDate dueOn,
                  boolean renewed, LocalDate returnedOn) {

    static LoanRecord from(Loan loan) {
        return new LoanRecord(loan.id().value(), loan.memberId().value(), loan.copyId().value(),
                loan.borrowedOn(), loan.dueOn(), loan.isRenewed(), loan.returnedOn().orElse(null));
    }

    Loan toLoan() {
        return Loan.restore(new LoanId(id), new MemberId(memberId), new BookCopyId(copyId),
                borrowedOn, dueOn, renewed, returnedOn);
    }

    boolean isActive() {
        return returnedOn == null;
    }
}
