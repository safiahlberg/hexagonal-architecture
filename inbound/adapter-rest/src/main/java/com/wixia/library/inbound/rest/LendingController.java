package com.wixia.library.inbound.rest;

import com.wixia.library.domain.catalog.BookCopyId;
import com.wixia.library.domain.loan.Loan;
import com.wixia.library.domain.loan.LoanId;
import com.wixia.library.domain.member.MemberId;
import com.wixia.library.inbound.port.BorrowBookUseCase;
import com.wixia.library.inbound.port.RenewLoanUseCase;
import com.wixia.library.inbound.port.ReturnBookUseCase;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class LendingController {

    private final BorrowBookUseCase borrowBook;
    private final ReturnBookUseCase returnBook;
    private final RenewLoanUseCase renewLoan;

    LendingController(BorrowBookUseCase borrowBook, ReturnBookUseCase returnBook, RenewLoanUseCase renewLoan) {
        this.borrowBook = borrowBook;
        this.returnBook = returnBook;
        this.renewLoan = renewLoan;
    }

    @PostMapping("/loans")
    ResponseEntity<LoanResponse> borrow(@RequestBody BorrowRequest request) {
        List<String> missing = request.missingFields();
        if (!missing.isEmpty()) {
            throw new InvalidRequestException("Missing required fields: " + String.join(", ", missing));
        }
        Loan loan = borrowBook.borrow(new MemberId(request.memberId()), new BookCopyId(request.copyId()));
        return ResponseEntity.created(URI.create("/loans/" + loan.id().value())).body(LoanResponse.from(loan));
    }

    @PostMapping("/loans/{loanId}/renewal")
    LoanResponse renew(@PathVariable UUID loanId) {
        return LoanResponse.from(renewLoan.renew(new LoanId(loanId)));
    }

    @PostMapping("/copies/{copyId}/return")
    LoanResponse returnCopy(@PathVariable UUID copyId) {
        return LoanResponse.from(returnBook.returnCopy(new BookCopyId(copyId)));
    }
}
