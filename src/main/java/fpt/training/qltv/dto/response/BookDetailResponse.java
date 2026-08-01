package fpt.training.qltv.dto.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookDetailResponse extends BookResponse {

    private Long borrowCount;
    private List<ReviewResponse> reviews;
    private List<Long> categoryIds;
    private List<Long> authorIds;
}
