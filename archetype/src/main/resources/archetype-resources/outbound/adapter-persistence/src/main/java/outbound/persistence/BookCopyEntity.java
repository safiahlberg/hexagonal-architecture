package ${package}.outbound.persistence;

import ${package}.domain.catalog.BookCopy;
import ${package}.domain.catalog.BookCopyId;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * The stored form of a book copy.
 */
@Entity
@Table(name = "book_copies")
class BookCopyEntity {

    @Id
    private UUID id;
    private String isbn;
    private String title;

    protected BookCopyEntity() {
        // for JPA
    }

    static BookCopyEntity from(BookCopy copy) {
        BookCopyEntity entity = new BookCopyEntity();
        entity.id = copy.id().value();
        entity.isbn = copy.isbn();
        entity.title = copy.title();
        return entity;
    }

    BookCopy toBookCopy() {
        return new BookCopy(new BookCopyId(id), isbn, title);
    }
}
