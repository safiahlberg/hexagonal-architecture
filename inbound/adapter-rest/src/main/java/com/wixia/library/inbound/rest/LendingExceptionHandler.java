package com.wixia.library.inbound.rest;

import com.wixia.library.domain.loan.LendingRefusedException;
import com.wixia.library.inbound.port.NotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates the use cases' exceptions into RFC 9457 problem details.
 */
@RestControllerAdvice
class LendingExceptionHandler {

    @ExceptionHandler
    ProblemDetail notFound(NotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler
    ProblemDetail refused(LendingRefusedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Lending refused");
        problem.setProperty("reason", exception.reason());
        return problem;
    }
}
