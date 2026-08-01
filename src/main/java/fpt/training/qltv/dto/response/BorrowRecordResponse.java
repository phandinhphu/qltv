package fpt.training.qltv.dto.response;

import fpt.training.qltv.entity.BorrowStatus;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BorrowRecordResponse {

    private Long id;
    private Long userId;
    private String username;
    private Long bookId;
    private String bookTitle;
    private LocalDateTime borrowDate;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;
    private BorrowStatus status;
    private String downloadToken;
    private LocalDateTime tokenExpiredAt;
    private Integer daysRemaining;
    private boolean hasValidToken;
    private boolean hasFile;
}
