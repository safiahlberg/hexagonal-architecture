package ${package}.domain.loan;

import java.util.Objects;

/**
 * Thrown when a lending rule forbids the requested operation.
 */
public final class LendingRefusedException extends RuntimeException {

    public enum Reason {
        COPY_ON_LOAN("the copy is already on loan"),
        LOAN_LIMIT_REACHED("the member has reached the maximum number of loans"),
        MEMBER_HAS_OVERDUE_LOANS("the member has overdue loans"),
        ALREADY_RETURNED("the loan has already been returned"),
        ALREADY_RENEWED("the loan has already been renewed"),
        LOAN_OVERDUE("the loan is overdue and can no longer be renewed");

        private final String description;

        Reason(String description) {
            this.description = description;
        }
    }

    private final Reason reason;

    public LendingRefusedException(Reason reason) {
        super(Objects.requireNonNull(reason, "reason").description);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
