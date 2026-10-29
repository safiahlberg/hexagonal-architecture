package ${package}.outbound.persistence;

import ${package}.domain.catalog.BookCopyId;
import ${package}.domain.loan.Loan;
import ${package}.domain.loan.LoanId;
import ${package}.domain.member.MemberId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/**
 * The stored form of a loan. The repository maps it to and from {@link Loan}, so changes to a loan are kept only
 * once it is saved.
 */
@Entity
@Table(name = "loans")
class LoanEntity {

    @Id
    private UUID id;
    @Column(name = "member_id")
    private UUID memberId;
    @Column(name = "copy_id")
    private UUID copyId;
    @Column(name = "borrowed_on")
    private LocalDate borrowedOn;
    @Column(name = "due_on")
    private LocalDate dueOn;
    private boolean renewed;
    @Column(name = "returned_on")
    private LocalDate returnedOn;

    protected LoanEntity() {
        // for JPA
    }

    static LoanEntity from(Loan loan) {
        LoanEntity entity = new LoanEntity();
        entity.id = loan.id().value();
        entity.memberId = loan.memberId().value();
        entity.copyId = loan.copyId().value();
        entity.borrowedOn = loan.borrowedOn();
        entity.dueOn = loan.dueOn();
        entity.renewed = loan.isRenewed();
        entity.returnedOn = loan.returnedOn().orElse(null);
        return entity;
    }

    Loan toLoan() {
        return Loan.restore(new LoanId(id), new MemberId(memberId), new BookCopyId(copyId),
                borrowedOn, dueOn, renewed, returnedOn);
    }
}
