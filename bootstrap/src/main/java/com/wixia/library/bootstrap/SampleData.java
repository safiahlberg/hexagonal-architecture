package com.wixia.library.bootstrap;

import com.wixia.library.domain.catalog.BookCopy;
import com.wixia.library.domain.catalog.BookCopyId;
import com.wixia.library.domain.member.Member;
import com.wixia.library.domain.member.MemberId;
import com.wixia.library.outbound.persistence.JpaBookCopyRepository;
import com.wixia.library.outbound.persistence.JpaMemberRepository;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Members and copies to try the API with, since the use cases cannot create either. They are written at every start,
 * replacing any rows with the same ids. The ids are fixed so requests can be written in advance.
 */
@Component
class SampleData implements ApplicationRunner {

    static final List<Member> MEMBERS = List.of(
            new Member(new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111")), "Ada", "ada@example.com"),
            new Member(new MemberId(UUID.fromString("22222222-2222-2222-2222-222222222222")), "Bob", "bob@example.com"));

    static final List<BookCopy> COPIES = List.of(
            new BookCopy(new BookCopyId(UUID.fromString("c0000000-0000-0000-0000-000000000001")), "978-0-441-17271-9", "Dune"),
            new BookCopy(new BookCopyId(UUID.fromString("c0000000-0000-0000-0000-000000000002")), "978-0-441-56959-5", "Neuromancer"),
            new BookCopy(new BookCopyId(UUID.fromString("c0000000-0000-0000-0000-000000000003")), "978-0-441-47812-5", "The Left Hand of Darkness"));

    private static final Logger LOG = LoggerFactory.getLogger(SampleData.class);

    private final JpaMemberRepository members;
    private final JpaBookCopyRepository copies;
    private final TransactionTemplate transaction;

    SampleData(JpaMemberRepository members, JpaBookCopyRepository copies, TransactionTemplate transaction) {
        this.members = members;
        this.copies = copies;
        this.transaction = transaction;
    }

    @Override
    public void run(ApplicationArguments args) {
        transaction.executeWithoutResult(status -> {
            MEMBERS.forEach(members::add);
            COPIES.forEach(copies::add);
        });
        MEMBERS.forEach(member -> LOG.info("Sample member {}: {}", member.name(), member.id().value()));
        COPIES.forEach(copy -> LOG.info("Sample copy \"{}\": {}", copy.title(), copy.id().value()));
    }
}
