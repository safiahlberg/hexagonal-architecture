/**
 * Member notification that only logs. Stands in for an e-mail or SMS adapter.
 */
module com.wixia.library.outbound.notification {
    requires transitive com.wixia.library.outbound.port;

    exports com.wixia.library.outbound.notification;
}
