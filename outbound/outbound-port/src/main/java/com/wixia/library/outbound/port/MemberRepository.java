package com.wixia.library.outbound.port;

import com.wixia.library.domain.member.Member;
import com.wixia.library.domain.member.MemberId;

import java.util.Optional;

public interface MemberRepository {

    Optional<Member> findById(MemberId id);
}
