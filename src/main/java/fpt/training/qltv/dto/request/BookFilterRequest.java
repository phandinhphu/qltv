package fpt.training.qltv.dto.request;

import fpt.training.qltv.entity.BookStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookFilterRequest {

    private String title;
    private Long categoryId;
    private Long authorId;
    private String language;
    private BookStatus status;
    private Integer publishYear;
    private Boolean deleted;
}
