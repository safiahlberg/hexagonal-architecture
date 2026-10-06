/**
 * What the library needs from the outside world: storage and member notification.
 */
module com.wixia.library.outbound.port {
    requires transitive com.wixia.library.domain;

    exports com.wixia.library.outbound.port;
}
