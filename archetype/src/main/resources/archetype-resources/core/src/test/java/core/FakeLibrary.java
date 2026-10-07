package ${package}.core;

import ${package}.domain.catalog.BookCopy;
import ${package}.domain.catalog.BookCopyId;
import ${package}.domain.loan.Loan;
import ${package}.domain.loan.LoanId;
import ${package}.domain.member.Member;
import ${package}.domain.member.MemberId;
import ${package}.outbound.port.BookCopyRepository;
import ${package}.outbound.port.LoanRepository;
import ${package}.outbound.port.MemberNotifier;
import ${package}.outbound.port.MemberRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Test double for all outbound ports. The core may not depend on the real adapters, so its tests use this.
 */
final class FakeLibrary implements MemberRepository, BookCopyRepository, LoanRepository, MemberNotifier {

    final Map<MemberId, Member> members = new HashMap<>();
    final Map<BookCopyId, BookCopy> copies = new HashMap<>();
    final Map<LoanId, Loan> loans = new HashMap<>();
    final List<String> notifications = new ArrayList<>();

    Member addMember(String name) {
        Member member = new Member(MemberId.newId(), name, name.toLowerCase() + "@example.com");
        members.put(member.id(), member);
        return member;
    }

    BookCopy addCopy(String title) {
        BookCopy copy = new BookCopy(BookCopyId.newId(), "978-0-00-000000-0", title);
        copies.put(copy.id(), copy);
        return copy;
    }

    @Override
    public Optional<Member> findById(MemberId id) {
        return Optional.ofNullable(members.get(id));
    }

    /** The tests run one use case at a time, so there is nothing to lock. */
    @Override
    public Optional<Member> findByIdForUpdate(MemberId id) {
        return findById(id);
    }

    @Override
    public Optional<BookCopy> findById(BookCopyId id) {
        return Optional.ofNullable(copies.get(id));
    }

    @Override
    public Optional<Loan> findById(LoanId id) {
        return Optional.ofNullable(loans.get(id));
    }

    @Override
    public List<Loan> findActiveByMember(MemberId memberId) {
        return loans.values().stream().filter(Loan::isActive).filter(l -> l.memberId().equals(memberId)).toList();
    }

    @Override
    public Optional<Loan> findActiveByCopy(BookCopyId copyId) {
        return loans.values().stream().filter(Loan::isActive).filter(l -> l.copyId().equals(copyId)).findFirst();
    }

    @Override
    public void save(Loan loan) {
        loans.put(loan.id(), loan);
    }

    @Override
    public void loanConfirmed(Member member, BookCopy copy, Loan loan) {
        notifications.add("confirmed: " + member.name() + " / " + copy.title());
    }

    @Override
    public void loanRenewed(Member member, BookCopy copy, Loan loan) {
        notifications.add("renewed: " + member.name() + " / " + copy.title());
    }
}
