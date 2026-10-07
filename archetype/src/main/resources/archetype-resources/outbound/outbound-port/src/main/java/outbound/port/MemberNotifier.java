package ${package}.outbound.port;

import ${package}.domain.catalog.BookCopy;
import ${package}.domain.loan.Loan;
import ${package}.domain.member.Member;

/**
 * Tells members about their loans. How (e-mail, SMS, ...) is up to the adapter.
 */
public interface MemberNotifier {

    void loanConfirmed(Member member, BookCopy copy, Loan loan);

    void loanRenewed(Member member, BookCopy copy, Loan loan);
}
