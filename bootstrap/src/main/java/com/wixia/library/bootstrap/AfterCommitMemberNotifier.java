package com.wixia.library.bootstrap;

import com.wixia.library.domain.catalog.BookCopy;
import com.wixia.library.domain.loan.Loan;
import com.wixia.library.domain.member.Member;
import com.wixia.library.outbound.port.MemberNotifier;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Holds back notifications until the use case's transaction has committed, so a member is never told about a loan
 * that was rolled back. Outside a transaction, notifications go out at once.
 * <p>
 * Delivery is at most once: a notification is lost if the process stops between commit and sending. If that matters,
 * use an outbox table written in the same transaction instead.
 */
final class AfterCommitMemberNotifier implements MemberNotifier {

    private static final Logger LOG = LoggerFactory.getLogger(AfterCommitMemberNotifier.class);

    private final MemberNotifier delegate;

    AfterCommitMemberNotifier(MemberNotifier delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public void loanConfirmed(Member member, BookCopy copy, Loan loan) {
        afterCommit(() -> delegate.loanConfirmed(member, copy, loan));
    }

    @Override
    public void loanRenewed(Member member, BookCopy copy, Loan loan) {
        afterCommit(() -> delegate.loanRenewed(member, copy, loan));
    }

    private static void afterCommit(Runnable notification) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notification.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // The loan is already committed, so a failed notification must not fail the request.
                try {
                    notification.run();
                } catch (RuntimeException e) {
                    LOG.error("Could not notify member", e);
                }
            }
        });
    }
}
