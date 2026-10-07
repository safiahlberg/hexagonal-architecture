package com.wixia.library.outbound.persistence;

import com.wixia.library.domain.member.Member;
import com.wixia.library.domain.member.MemberId;
import com.wixia.library.outbound.port.MemberRepository;

import jakarta.persistence.EntityManager;

import java.util.Objects;
import java.util.Optional;

/**
 * Must be called inside a transaction, which the caller manages.
 */
public final class JpaMemberRepository implements MemberRepository {

    private final EntityManager entityManager;

    public JpaMemberRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    /** Registers a member, or updates one with the same id. Not part of the port: the use cases never create members. */
    public void add(Member member) {
        entityManager.merge(MemberEntity.from(member));
    }

    @Override
    public Optional<Member> findById(MemberId id) {
        return Optional.ofNullable(entityManager.find(MemberEntity.class, id.value())).map(MemberEntity::toMember);
    }
}
