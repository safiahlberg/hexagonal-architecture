package com.wixia.library.inbound.rest;

import java.util.UUID;

record BorrowRequest(UUID memberId, UUID copyId) {

    BorrowRequest {
        // Thrown during deserialisation, so Spring answers 400 Bad Request.
        if (memberId == null || copyId == null) {
            throw new IllegalArgumentException("memberId and copyId are required");
        }
    }
}
