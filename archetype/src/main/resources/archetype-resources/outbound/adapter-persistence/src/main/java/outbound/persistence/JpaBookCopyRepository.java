package ${package}.outbound.persistence;

import ${package}.domain.catalog.BookCopy;
import ${package}.domain.catalog.BookCopyId;
import ${package}.outbound.port.BookCopyRepository;

import jakarta.persistence.EntityManager;

import java.util.Objects;
import java.util.Optional;

/**
 * Must be called inside a transaction, which the caller manages.
 */
public final class JpaBookCopyRepository implements BookCopyRepository {

    private final EntityManager entityManager;

    public JpaBookCopyRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    /** Adds a copy to the catalogue, or updates one with the same id. Not part of the port: the use cases never create copies. */
    public void add(BookCopy copy) {
        entityManager.merge(BookCopyEntity.from(copy));
    }

    @Override
    public Optional<BookCopy> findById(BookCopyId id) {
        return Optional.ofNullable(entityManager.find(BookCopyEntity.class, id.value())).map(BookCopyEntity::toBookCopy);
    }
}
