package com.wixia.library.outbound.port;

import com.wixia.library.domain.member.Member;
import com.wixia.library.domain.member.MemberId;

import java.util.Optional;

public interface MemberRepository {

    Optional<Member> findById(MemberId id);

    /**
     * Like {@link #findById}, but also locks the member until the current transaction ends. Another transaction
     * that locks the same member waits until then, and so sees the loans this one stores. Used to check the
     * member's loans and add one without another borrow by the same member running in between.
     */
    Optional<Member> findByIdForUpdate(MemberId id);
}
