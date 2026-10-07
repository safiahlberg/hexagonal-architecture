package com.wixia.library.core;

import com.wixia.library.domain.catalog.BookCopyId;
import com.wixia.library.domain.loan.Loan;
import com.wixia.library.inbound.port.NotFoundException;
import com.wixia.library.inbound.port.ReturnBookUseCase;
import com.wixia.library.outbound.port.LoanRepository;

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
