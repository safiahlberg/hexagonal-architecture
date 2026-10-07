package ${package}.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ${package}.domain.catalog.BookCopy;
import ${package}.domain.catalog.BookCopyId;
import ${package}.domain.loan.Loan;
import ${package}.domain.loan.LoanId;
import ${package}.domain.member.Member;
import ${package}.domain.member.MemberId;
import ${package}.outbound.port.MemberNotifier;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class AfterCommitMemberNotifierTest {

    private static final Member ADA = new Member(MemberId.newId(), "Ada", "ada@example.com");
    private static final BookCopy DUNE = new BookCopy(BookCopyId.newId(), "978-0-441-17271-9", "Dune");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 7);
    private static final Loan LOAN = Loan.restore(LoanId.newId(), ADA.id(), DUNE.id(), TODAY, TODAY.plusWeeks(3), false, null);

    private final List<String> sent = new ArrayList<>();
    private final TransactionTemplate transaction = new TransactionTemplate(new NoOpTransactionManager());

    @Test
    void notifiesOnlyAfterCommit() {
        MemberNotifier notifier = new AfterCommitMemberNotifier(recording());

        transaction.executeWithoutResult(status -> {
            notifier.loanConfirmed(ADA, DUNE, LOAN);
            notifier.loanRenewed(ADA, DUNE, LOAN);
            assertEquals(List.of(), sent);
        });

        assertEquals(List.of("confirmed", "renewed"), sent);
    }

    @Test
    void sendsNothingWhenRolledBack() {
        MemberNotifier notifier = new AfterCommitMemberNotifier(recording());

        transaction.executeWithoutResult(status -> {
            notifier.loanConfirmed(ADA, DUNE, LOAN);
            status.setRollbackOnly();
        });

        assertEquals(List.of(), sent);
    }

    @Test
    void failedNotificationDoesNotFailTheCommittedTransaction() {
        MemberNotifier failing = new MemberNotifier() {
            @Override
            public void loanConfirmed(Member member, BookCopy copy, Loan loan) {
                throw new IllegalStateException("mail server down");
            }

            @Override
            public void loanRenewed(Member member, BookCopy copy, Loan loan) {
                throw new IllegalStateException("mail server down");
            }
        };
        MemberNotifier notifier = new AfterCommitMemberNotifier(failing);

        String result = transaction.execute(status -> {
            notifier.loanConfirmed(ADA, DUNE, LOAN);
            return "lent";
        });

        assertEquals("lent", result);
    }

    @Test
    void notifiesAtOnceOutsideATransaction() {
        new AfterCommitMemberNotifier(recording()).loanConfirmed(ADA, DUNE, LOAN);

        assertEquals(List.of("confirmed"), sent);
    }

    private MemberNotifier recording() {
        return new MemberNotifier() {
            @Override
            public void loanConfirmed(Member member, BookCopy copy, Loan loan) {
                sent.add("confirmed");
            }

            @Override
            public void loanRenewed(Member member, BookCopy copy, Loan loan) {
                sent.add("renewed");
            }
        };
    }

    /** Runs Spring's transaction lifecycle, including synchronizations, without a database. */
    private static final class NoOpTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
