package com.wixia.library.bootstrap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Runs the whole application, from HTTP request through the core to the in-memory adapters.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LibraryApplicationTest {

    private static final UUID ADA = SampleData.MEMBERS.get(0).id().value();
    private static final UUID BOB = SampleData.MEMBERS.get(1).id().value();
    private static final UUID DUNE = SampleData.COPIES.get(0).id().value();

    @Autowired
    private MockMvc mvc;

    @Test
    void lendingRoundTrip() throws Exception {
        String loan = borrow(ADA, DUNE)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String loanId = JsonPath.read(loan, "$.id");

        borrow(BOB, DUNE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("COPY_ON_LOAN"));

        mvc.perform(post("/loans/{id}/renewal", loanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.renewed").value(true));

        mvc.perform(post("/copies/{id}/return", DUNE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnedOn").isNotEmpty());

        borrow(BOB, DUNE)
                .andExpect(status().isCreated());
    }

    @Test
    void unknownMemberIsNotFound() throws Exception {
        borrow(UUID.randomUUID(), DUNE)
                .andExpect(status().isNotFound());
    }

    @Test
    void malformedIdIsExplainedEvenWithBootProblemDetailsEnabled() throws Exception {
        mvc.perform(post("/loans").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"oops\",\"copyId\":\"%s\"}".formatted(DUNE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid value for memberId, expected UUID"));
    }

    private ResultActions borrow(UUID member, UUID copy) throws Exception {
        return mvc.perform(post("/loans").contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":\"%s\",\"copyId\":\"%s\"}".formatted(member, copy)));
    }
}
