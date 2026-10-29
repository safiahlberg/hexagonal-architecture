package com.wixia.library.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.wixia.library.domain.catalog.BookCopy;
import com.wixia.library.domain.catalog.BookCopyId;
import com.wixia.library.domain.loan.LendingPolicy;
import com.wixia.library.domain.loan.LendingRefusedException;
import com.wixia.library.domain.member.Member;
import com.wixia.library.domain.member.MemberId;
import com.wixia.library.inbound.port.BorrowBookUseCase;
import com.wixia.library.outbound.persistence.JpaBookCopyRepository;
import com.wixia.library.outbound.persistence.JpaMemberRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Borrows that run at the same time must not get around the lending rules that {@link LendingPolicy} checks
 * one request at a time.
 */
@SpringBootTest
@Import(TestDatabaseConfiguration.class)
class ConcurrentBorrowingTest {

    private static final int REQUESTS = LendingPolicy.DEFAULT_MAX_ACTIVE_LOANS + 3;

    @Autowired
    private BorrowBookUseCase borrowBook;
    @Autowired
    private JpaMemberRepository members;
    @Autowired
    private JpaBookCopyRepository copies;
    @Autowired
    private TransactionTemplate transaction;

    @RepeatedTest(5)
    void memberCannotExceedTheLoanLimit() throws Exception {
        MemberId member = newMember();
        List<BookCopyId> differentCopies = IntStream.range(0, REQUESTS).mapToObj(i -> newCopy()).toList();

        Map<String, Long> outcomes = borrowAtOnce(differentCopies.stream()
                .map(copy -> (Callable<String>) () -> attempt(member, copy))
                .toList());

        assertEquals(Map.of("LENT", (long) LendingPolicy.DEFAULT_MAX_ACTIVE_LOANS,
                "LOAN_LIMIT_REACHED", (long) (REQUESTS - LendingPolicy.DEFAULT_MAX_ACTIVE_LOANS)), outcomes);
    }

    @RepeatedTest(5)
    void copyIsLentToOneMemberOnly() throws Exception {
        BookCopyId copy = newCopy();
        List<MemberId> differentMembers = IntStream.range(0, REQUESTS).mapToObj(i -> newMember()).toList();

        Map<String, Long> outcomes = borrowAtOnce(differentMembers.stream()
                .map(member -> (Callable<String>) () -> attempt(member, copy))
                .toList());

        assertEquals(Map.of("LENT", 1L, "COPY_ON_LOAN", (long) (REQUESTS - 1)), outcomes);
    }

    /** Starts all borrows together and counts their outcomes. */
    private static Map<String, Long> borrowAtOnce(List<Callable<String>> borrows) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(borrows.size())) {
            List<Future<String>> futures = new ArrayList<>();
            for (Callable<String> borrow : borrows) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return borrow.call();
                }));
            }
            start.countDown();
            List<String> outcomes = new ArrayList<>();
            for (Future<String> future : futures) {
                outcomes.add(future.get());
            }
            return outcomes.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        }
    }

    private String attempt(MemberId member, BookCopyId copy) {
        try {
            borrowBook.borrow(member, copy);
            return "LENT";
        } catch (LendingRefusedException e) {
            return e.reason().name();
        }
    }

    private MemberId newMember() {
        Member member = new Member(MemberId.newId(), "Cleo", "cleo@example.com");
        transaction.executeWithoutResult(status -> members.add(member));
        return member.id();
    }

    private BookCopyId newCopy() {
        BookCopy copy = new BookCopy(BookCopyId.newId(), "978-0-553-29335-7", "Foundation");
        transaction.executeWithoutResult(status -> copies.add(copy));
        return copy.id();
    }
}
