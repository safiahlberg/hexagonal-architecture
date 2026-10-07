package com.wixia.library.outbound.persistence;

import com.wixia.library.domain.catalog.BookCopyId;
import com.wixia.library.domain.loan.Loan;
import com.wixia.library.domain.loan.LoanId;
import com.wixia.library.domain.member.MemberId;
import com.wixia.library.outbound.port.LoanRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores loans as {@link LoanRecord} snapshots, so, as with a real database, changes to a loan are kept only once
 * it is saved.
 */
public final class InMemoryLoanRepository implements LoanRepository {

    private final Map<UUID, LoanRecord> loans = new ConcurrentHashMap<>();

    @Override
    public Optional<Loan> findById(LoanId id) {
        return Optional.ofNullable(loans.get(id.value())).map(LoanRecord::toLoan);
    }

    @Override
    public List<Loan> findActiveByMember(MemberId memberId) {
        return loans.values().stream()
                .filter(LoanRecord::isActive)
                .filter(loan -> loan.memberId().equals(memberId.value()))
                .map(LoanRecord::toLoan)
                .toList();
    }

    @Override
    public Optional<Loan> findActiveByCopy(BookCopyId copyId) {
        return loans.values().stream()
                .filter(LoanRecord::isActive)
                .filter(loan -> loan.copyId().equals(copyId.value()))
                .findFirst()
                .map(LoanRecord::toLoan);
    }

    @Override
    public void save(Loan loan) {
        loans.put(loan.id().value(), LoanRecord.from(loan));
    }
}
