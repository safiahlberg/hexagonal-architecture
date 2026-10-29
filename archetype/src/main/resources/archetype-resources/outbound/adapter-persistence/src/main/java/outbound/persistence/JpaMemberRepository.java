package ${package}.outbound.persistence;

import ${package}.domain.member.Member;
import ${package}.domain.member.MemberId;
import ${package}.outbound.port.MemberRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

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

    /** Locks the member's row with {@code select ... for no key update}, which PostgreSQL holds until commit. */
    @Override
    public Optional<Member> findByIdForUpdate(MemberId id) {
        return Optional.ofNullable(entityManager.find(MemberEntity.class, id.value(), LockModeType.PESSIMISTIC_WRITE))
                .map(MemberEntity::toMember);
    }
}
