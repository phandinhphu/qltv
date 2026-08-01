package fpt.training.qltv.repository.spec;

import fpt.training.qltv.entity.BorrowRecord;
import fpt.training.qltv.entity.BorrowStatus;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;

public final class BorrowSpecification {

    private BorrowSpecification() {
    }

    public static Specification<BorrowRecord> of(
            Long userId,
            Long bookId,
            BorrowStatus status,
            LocalDateTime fromDate,
            LocalDateTime toDate) {
        Specification<BorrowRecord> specification = (root, query, cb) -> cb.conjunction();
        specification = specification.and(hasUserId(userId));
        specification = specification.and(hasBookId(bookId));
        specification = specification.and(hasStatus(status));
        specification = specification.and(fromDate(fromDate));
        specification = specification.and(toDate(toDate));
        return specification;
    }

    private static Specification<BorrowRecord> hasUserId(Long userId) {
        return (root, query, cb) -> userId == null ? null : cb.equal(root.get("user").get("id"), userId);
    }

    private static Specification<BorrowRecord> hasBookId(Long bookId) {
        return (root, query, cb) -> bookId == null ? null : cb.equal(root.get("book").get("id"), bookId);
    }

    private static Specification<BorrowRecord> hasStatus(BorrowStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    private static Specification<BorrowRecord> fromDate(LocalDateTime fromDate) {
        return (root, query, cb) -> fromDate == null ? null : cb.greaterThanOrEqualTo(root.get("borrowDate"), fromDate);
    }

    private static Specification<BorrowRecord> toDate(LocalDateTime toDate) {
        return (root, query, cb) -> toDate == null ? null : cb.lessThanOrEqualTo(root.get("borrowDate"), toDate);
    }
}
