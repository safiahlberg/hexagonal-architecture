package com.wixia.library.outbound.port;

import com.wixia.library.domain.catalog.BookCopy;
import com.wixia.library.domain.loan.Loan;
import com.wixia.library.domain.member.Member;

/**
 * Tells members about their loans. How (e-mail, SMS, ...) is up to the adapter.
 */
public interface MemberNotifier {

    void loanConfirmed(Member member, BookCopy copy, Loan loan);

    void loanRenewed(Member member, BookCopy copy, Loan loan);
}
