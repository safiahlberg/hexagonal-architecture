package com.wixia.library.inbound.rest;

/**
 * Thrown when a request is well-formed JSON but lacks something the API needs.
 */
final class InvalidRequestException extends RuntimeException {

    InvalidRequestException(String message) {
        super(message);
    }
}
