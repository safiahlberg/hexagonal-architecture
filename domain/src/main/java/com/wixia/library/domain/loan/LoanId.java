package com.wixia.library.domain.loan;

import java.util.Objects;
import java.util.UUID;

public record LoanId(UUID value) {

    public LoanId {
        Objects.requireNonNull(value, "value");
    }

    public static LoanId newId() {
        return new LoanId(UUID.randomUUID());
    }
}
