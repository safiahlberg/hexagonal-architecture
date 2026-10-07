package com.wixia.library.domain.catalog;

import java.util.Objects;
import java.util.UUID;

public record BookCopyId(UUID value) {

    public BookCopyId {
        Objects.requireNonNull(value, "value");
    }

    public static BookCopyId newId() {
        return new BookCopyId(UUID.randomUUID());
    }
}
