package com.wixia.library.outbound.persistence;

import com.wixia.library.domain.member.Member;
import com.wixia.library.domain.member.MemberId;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * The stored form of a member. Kept apart from {@link Member} so the domain carries no persistence annotations.
 */
@Entity
@Table(name = "members")
class MemberEntity {

    @Id
    private UUID id;
    private String name;
    private String email;

    protected MemberEntity() {
        // for JPA
    }

    static MemberEntity from(Member member) {
        MemberEntity entity = new MemberEntity();
        entity.id = member.id().value();
        entity.name = member.name();
        entity.email = member.email();
        return entity;
    }

    Member toMember() {
        return new Member(new MemberId(id), name, email);
    }
}
