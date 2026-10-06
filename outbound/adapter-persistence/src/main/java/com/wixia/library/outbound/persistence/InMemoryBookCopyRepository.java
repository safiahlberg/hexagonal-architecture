package com.wixia.library.outbound.persistence;

import com.wixia.library.domain.catalog.BookCopy;
import com.wixia.library.domain.catalog.BookCopyId;
import com.wixia.library.outbound.port.BookCopyRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryBookCopyRepository implements BookCopyRepository {

    private final Map<BookCopyId, BookCopy> copies = new ConcurrentHashMap<>();

    /** Adds a copy to the catalogue. Not part of the port: the use cases never create copies. */
    public void add(BookCopy copy) {
        copies.put(copy.id(), copy);
    }

    @Override
    public Optional<BookCopy> findById(BookCopyId id) {
        return Optional.ofNullable(copies.get(id));
    }
}
