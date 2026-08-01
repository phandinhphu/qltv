package fpt.training.qltv.dto.response;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewResponse {

    private Long id;
    private Integer rating;
    private String comment;
    private boolean visible;
    private Long userId;
    private String username;
    private Long bookId;
    private LocalDateTime createdAt;
}
