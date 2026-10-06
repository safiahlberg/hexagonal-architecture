package com.wixia.library.outbound.persistence;

import com.wixia.library.domain.member.Member;
import com.wixia.library.domain.member.MemberId;
import com.wixia.library.outbound.port.MemberRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryMemberRepository implements MemberRepository {

    private final Map<MemberId, Member> members = new ConcurrentHashMap<>();

    /** Registers a member. Not part of the port: the use cases never create members. */
    public void add(Member member) {
        members.put(member.id(), member);
    }

    @Override
    public Optional<Member> findById(MemberId id) {
        return Optional.ofNullable(members.get(id));
    }
}
