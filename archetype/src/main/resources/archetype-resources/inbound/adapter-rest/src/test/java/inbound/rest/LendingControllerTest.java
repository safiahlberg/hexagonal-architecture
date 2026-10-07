#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
package ${package}.inbound.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ${package}.domain.catalog.BookCopyId;
import ${package}.domain.loan.LendingRefusedException;
import ${package}.domain.loan.LendingRefusedException.Reason;
import ${package}.domain.loan.Loan;
import ${package}.domain.loan.LoanId;
import ${package}.domain.member.MemberId;
import ${package}.inbound.port.BorrowBookUseCase;
import ${package}.inbound.port.NotFoundException;
import ${package}.inbound.port.RenewLoanUseCase;
import ${package}.inbound.port.ReturnBookUseCase;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LendingController.class)
class LendingControllerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 6);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private BorrowBookUseCase borrowBook;

    @MockitoBean
    private ReturnBookUseCase returnBook;

    @MockitoBean
    private RenewLoanUseCase renewLoan;

    private final Loan loan = Loan.restore(LoanId.newId(), MemberId.newId(), BookCopyId.newId(),
            TODAY, TODAY.plusWeeks(3), false, null);

    @Test
    void borrowingCreatesLoan() throws Exception {
        given(borrowBook.borrow(loan.memberId(), loan.copyId())).willReturn(loan);

        mvc.perform(post("/loans").contentType(MediaType.APPLICATION_JSON).content(borrowJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/loans/" + loan.id().value()))
                .andExpect(jsonPath("${symbol_dollar}.id").value(loan.id().value().toString()))
                .andExpect(jsonPath("${symbol_dollar}.dueOn").value("2026-10-27"))
                .andExpect(jsonPath("${symbol_dollar}.renewed").value(false));
    }

    @Test
    void borrowingWithoutCopyIsBadRequest() throws Exception {
        mvc.perform(post("/loans").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"%s\"}".formatted(loan.memberId().value())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("${symbol_dollar}.detail").value("Missing required fields: copyId"));
    }

    @Test
    void borrowingWithEmptyBodyNamesBothFields() throws Exception {
        mvc.perform(post("/loans").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("${symbol_dollar}.detail").value("Missing required fields: memberId, copyId"));
    }

    @Test
    void refusedLoanIsConflictWithReason() throws Exception {
        given(borrowBook.borrow(any(), any())).willThrow(new LendingRefusedException(Reason.COPY_ON_LOAN));

        mvc.perform(post("/loans").contentType(MediaType.APPLICATION_JSON).content(borrowJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("${symbol_dollar}.reason").value("COPY_ON_LOAN"));
    }

    @Test
    void renewingReturnsRenewedLoan() throws Exception {
        Loan renewed = Loan.restore(loan.id(), loan.memberId(), loan.copyId(), TODAY, TODAY.plusWeeks(6), true, null);
        given(renewLoan.renew(loan.id())).willReturn(renewed);

        mvc.perform(post("/loans/{id}/renewal", loan.id().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("${symbol_dollar}.renewed").value(true))
                .andExpect(jsonPath("${symbol_dollar}.dueOn").value("2026-11-17"));
    }

    @Test
    void returningCopyNotOnLoanIsNotFound() throws Exception {
        given(returnBook.returnCopy(any())).willThrow(new NotFoundException("Copy is not on loan"));

        mvc.perform(post("/copies/{id}/return", loan.copyId().value()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("${symbol_dollar}.detail").value("Copy is not on loan"));
    }

    @Test
    void malformedPathIdIsBadRequest() throws Exception {
        mvc.perform(post("/copies/{id}/return", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("${symbol_dollar}.detail").value("Invalid value for copyId, expected UUID"));
    }

    @Test
    void malformedBodyIdIsBadRequest() throws Exception {
        mvc.perform(post("/loans").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"oops\",\"copyId\":\"%s\"}".formatted(loan.copyId().value())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("${symbol_dollar}.detail").value("Invalid value for memberId, expected UUID"));
    }

    @Test
    void bodyIdOfWrongJsonTypeIsBadRequest() throws Exception {
        mvc.perform(post("/loans").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"%s\",\"copyId\":[1]}".formatted(loan.memberId().value())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("${symbol_dollar}.detail").value("Invalid value for copyId, expected UUID"));
    }

    @Test
    void brokenJsonIsBadRequest() throws Exception {
        mvc.perform(post("/loans").contentType(MediaType.APPLICATION_JSON).content("{\"memberId\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("${symbol_dollar}.detail").value("Request body is missing or is not valid JSON"));
    }

    private String borrowJson() {
        return "{\"memberId\":\"%s\",\"copyId\":\"%s\"}".formatted(loan.memberId().value(), loan.copyId().value());
    }
}
