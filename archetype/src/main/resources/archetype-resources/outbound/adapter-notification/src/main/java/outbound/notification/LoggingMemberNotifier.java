package ${package}.outbound.notification;

import ${package}.domain.catalog.BookCopy;
import ${package}.domain.loan.Loan;
import ${package}.domain.member.Member;
import ${package}.outbound.port.MemberNotifier;

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
