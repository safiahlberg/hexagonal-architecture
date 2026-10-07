package ${package}.domain.member;

import java.util.Objects;

/**
 * A person registered with the library.
 */
public record Member(MemberId id, String name, String email) {

    public Member {
        Objects.requireNonNull(id, "id");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("email must be an e-mail address");
        }
    }
}
