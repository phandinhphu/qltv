package fpt.training.qltv.dto.request;

import fpt.training.qltv.entity.BorrowStatus;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BorrowFilterRequest {

    private Long userId;
    private Long bookId;
    private BorrowStatus status;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
}
