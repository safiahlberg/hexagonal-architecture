package ${package}.outbound.port;

import ${package}.domain.catalog.BookCopy;
import ${package}.domain.catalog.BookCopyId;

import java.util.Optional;

public interface BookCopyRepository {

    Optional<BookCopy> findById(BookCopyId id);
}
