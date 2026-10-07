package com.wixia.library.outbound.port;

import com.wixia.library.domain.catalog.BookCopy;
import com.wixia.library.domain.catalog.BookCopyId;

import java.util.Optional;

public interface BookCopyRepository {

    Optional<BookCopy> findById(BookCopyId id);
}
