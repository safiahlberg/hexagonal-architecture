package ${package}.domain.catalog;

import java.util.Objects;

/**
 * One physical copy of a book. A title can have several copies, and it is copies that are lent out.
 */
public record BookCopy(BookCopyId id, String isbn, String title) {

    public BookCopy {
        Objects.requireNonNull(id, "id");
        if (isbn == null || isbn.isBlank()) {
            throw new IllegalArgumentException("isbn must not be blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
    }
}
