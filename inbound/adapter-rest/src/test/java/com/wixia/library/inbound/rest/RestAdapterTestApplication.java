package com.wixia.library.inbound.rest;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Lets the web-layer tests start without the real composition root, which lives in the bootstrap module.
 */
@SpringBootApplication
class RestAdapterTestApplication {
}
