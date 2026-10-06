package com.wixia.library.inbound.port;

/**
 * Thrown when a use case refers to something the library does not know about.
 */
public final class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
