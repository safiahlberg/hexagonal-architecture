package ${package}.inbound.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

record BorrowRequest(UUID memberId, UUID copyId) {

    /**
     * Checked after deserialisation rather than in the constructor: an exception thrown while Jackson builds the
     * record would reach the client only as a generic "Failed to read request".
     */
    List<String> missingFields() {
        List<String> missing = new ArrayList<>();
        if (memberId == null) {
            missing.add("memberId");
        }
        if (copyId == null) {
            missing.add("copyId");
        }
        return missing;
    }
}
