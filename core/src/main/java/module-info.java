/**
 * The use cases: implements the inbound ports using the domain and the outbound ports.
 */
module com.wixia.library.core {
    requires transitive com.wixia.library.inbound.port;
    requires transitive com.wixia.library.outbound.port;

    exports com.wixia.library.core;
}
