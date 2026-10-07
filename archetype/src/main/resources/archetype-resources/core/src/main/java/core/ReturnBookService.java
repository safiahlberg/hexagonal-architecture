package ${package}.core;

import ${package}.domain.catalog.BookCopyId;
import ${package}.domain.loan.Loan;
import ${package}.inbound.port.NotFoundException;
import ${package}.inbound.port.ReturnBookUseCase;
import ${package}.outbound.port.LoanRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

public final class ReturnBookService implements ReturnBookUseCase {

    private final LoanRepository loans;
    private final Clock clock;

    public ReturnBookService(LoanRepository loans, Clock clock) {
        this.loans = Objects.requireNonNull(loans, "loans");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public Loan returnCopy(BookCopyId copyId) {
        Loan loan = loans.findActiveByCopy(copyId)
                .orElseThrow(() -> new NotFoundException("Copy " + copyId.value() + " is not on loan"));

        loan.giveBack(LocalDate.now(clock));
        loans.save(loan);
        return loan;
    }
}
