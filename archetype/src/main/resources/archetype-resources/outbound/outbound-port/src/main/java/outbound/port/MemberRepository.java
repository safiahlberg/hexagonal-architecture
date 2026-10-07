package ${package}.outbound.port;

import ${package}.domain.member.Member;
import ${package}.domain.member.MemberId;

import java.util.Optional;

public interface MemberRepository {

    Optional<Member> findById(MemberId id);
}
