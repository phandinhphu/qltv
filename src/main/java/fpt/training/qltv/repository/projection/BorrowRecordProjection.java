package fpt.training.qltv.repository.projection;

import fpt.training.qltv.entity.BorrowStatus;
import java.time.LocalDateTime;
import lombok.Getter;

/**
 * Constructor-based DTO dùng với {@code SELECT NEW} trong JPQL query. Dùng Option B vì BorrowRecord
 * cần JOIN 2 bảng (users, books) với nhiều field cross-table. Thứ tự và kiểu tham số trong
 * constructor phải khớp chính xác với query JPQL.
 */
@Getter
public class BorrowRecordProjection {

    private final Long id;
    private final Long userId;
    private final String username;
    private final Long bookId;
    private final String bookTitle;
    private final String bookFileUrl;
    private final LocalDateTime borrowDate;
    private final LocalDateTime dueDate;
    private final LocalDateTime returnDate;
    private final BorrowStatus status;
    private final String downloadToken;
    private final LocalDateTime tokenExpiredAt;

    public BorrowRecordProjection(
            Long id,
            Long userId,
            String username,
            Long bookId,
            String bookTitle,
            String bookFileUrl,
            LocalDateTime borrowDate,
            LocalDateTime dueDate,
            LocalDateTime returnDate,
            BorrowStatus status,
            String downloadToken,
            LocalDateTime tokenExpiredAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.bookFileUrl = bookFileUrl;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.status = status;
        this.downloadToken = downloadToken;
        this.tokenExpiredAt = tokenExpiredAt;
    }
}
