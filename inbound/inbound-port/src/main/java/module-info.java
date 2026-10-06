/**
 * What the library offers to the outside world: one use case interface per operation.
 */
module com.wixia.library.inbound.port {
    requires transitive com.wixia.library.domain;

    exports com.wixia.library.inbound.port;
}
