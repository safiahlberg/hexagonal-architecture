package com.wixia.library.outbound.notification;

import com.wixia.library.domain.catalog.BookCopy;
import com.wixia.library.domain.loan.Loan;
import com.wixia.library.domain.member.Member;
import com.wixia.library.outbound.port.MemberNotifier;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

public final class LoggingMemberNotifier implements MemberNotifier {

    private static final Logger LOG = System.getLogger(LoggingMemberNotifier.class.getName());

    @Override
    public void loanConfirmed(Member member, BookCopy copy, Loan loan) {
        LOG.log(Level.INFO, "To {0}: you have borrowed \"{1}\", due back on {2}.", member.email(), copy.title(), loan.dueOn());
    }

    @Override
    public void loanRenewed(Member member, BookCopy copy, Loan loan) {
        LOG.log(Level.INFO, "To {0}: \"{1}\" is renewed, now due back on {2}.", member.email(), copy.title(), loan.dueOn());
    }
}
