package com.wixia.library.outbound.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.wixia.library.domain.catalog.BookCopy;
import com.wixia.library.domain.catalog.BookCopyId;
import com.wixia.library.domain.member.Member;
import com.wixia.library.domain.member.MemberId;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JpaMemberAndCopyRepositoryTest {

    private EntityManager entityManager;
    private JpaMemberRepository members;
    private JpaBookCopyRepository copies;

    @BeforeEach
    void beginTransaction() {
        entityManager = TestDatabase.entityManagerFactory().createEntityManager();
        entityManager.getTransaction().begin();
        members = new JpaMemberRepository(entityManager);
        copies = new JpaBookCopyRepository(entityManager);
    }

    @AfterEach
    void rollBack() {
        entityManager.getTransaction().rollback();
        entityManager.close();
    }

    @Test
    void addedMemberCanBeFound() {
        Member member = new Member(MemberId.newId(), "Ada", "ada@example.com");

        members.add(member);
        entityManager.flush();
        entityManager.clear();

        assertEquals(Optional.of(member), members.findById(member.id()));
        assertEquals(Optional.empty(), members.findById(MemberId.newId()));
    }

    @Test
    void addingAMemberAgainUpdatesIt() {
        MemberId id = MemberId.newId();
        members.add(new Member(id, "Ada", "ada@example.com"));
        entityManager.flush();
        entityManager.clear();

        Member renamed = new Member(id, "Ada Lovelace", "ada@example.com");
        members.add(renamed);
        entityManager.flush();
        entityManager.clear();

        assertEquals(Optional.of(renamed), members.findById(id));
    }

    @Test
    void memberFoundForUpdateIsLockedUntilTheTransactionEnds() {
        Member member = new Member(MemberId.newId(), "Ada", "ada@example.com");
        try (EntityManager setup = TestDatabase.entityManagerFactory().createEntityManager()) {
            setup.getTransaction().begin();
            new JpaMemberRepository(setup).add(member);
            setup.getTransaction().commit();
        }

        assertEquals(Optional.of(member), members.findByIdForUpdate(member.id()));

        try (EntityManager other = TestDatabase.entityManagerFactory().createEntityManager()) {
            other.getTransaction().begin();
            // A timeout of 0 makes the second lock fail at once (NOWAIT) instead of waiting for this test's transaction.
            assertThrows(PersistenceException.class, () -> other.find(MemberEntity.class, member.id().value(),
                    LockModeType.PESSIMISTIC_WRITE, Map.of("jakarta.persistence.lock.timeout", 0)));
            other.getTransaction().rollback();
        }
    }

    @Test
    void addedCopyCanBeFound() {
        BookCopy copy = new BookCopy(BookCopyId.newId(), "978-0-441-56959-5", "Neuromancer");

        copies.add(copy);
        entityManager.flush();
        entityManager.clear();

        assertEquals(Optional.of(copy), copies.findById(copy.id()));
        assertEquals(Optional.empty(), copies.findById(BookCopyId.newId()));
    }
}
