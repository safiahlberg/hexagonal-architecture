package com.wixia.library.inbound.rest;

import com.wixia.library.domain.loan.LendingRefusedException;
import com.wixia.library.inbound.port.NotFoundException;

import java.util.stream.Collectors;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Translates the use cases' exceptions, and malformed requests, into RFC 9457 problem details.
 * <p>
 * Ordered first so it is consulted before Spring Boot's own problem-details handler, which would otherwise answer
 * malformed requests with its generic messages.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class LendingExceptionHandler {

    @ExceptionHandler
    ProblemDetail invalid(InvalidRequestException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    /**
     * A request body that could not be turned into a request object. When Jackson can say which field held a value of
     * the wrong type, the detail names it; for anything else (broken JSON, no body) a general message is used.
     */
    @ExceptionHandler
    ProblemDetail unreadable(HttpMessageNotReadableException exception) {
        String detail = exception.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()
                ? invalidValue(fieldName(mismatch), mismatch.getTargetType())
                : "Request body is missing or is not valid JSON";
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    /**
     * A path variable of the wrong type, e.g. an id that is not a UUID.
     */
    @ExceptionHandler
    ProblemDetail mismatchedPathVariable(MethodArgumentTypeMismatchException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                invalidValue(exception.getName(), exception.getRequiredType()));
    }

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

    private static String invalidValue(String field, Class<?> expectedType) {
        return expectedType == null
                ? "Invalid value for " + field
                : "Invalid value for " + field + ", expected " + expectedType.getSimpleName();
    }

    private static String fieldName(MismatchedInputException mismatch) {
        return mismatch.getPath().stream()
                .map(LendingExceptionHandler::pathElement)
                .collect(Collectors.joining("."));
    }

    private static String pathElement(JacksonException.Reference reference) {
        return reference.getPropertyName() != null ? reference.getPropertyName() : "[" + reference.getIndex() + "]";
    }
}
