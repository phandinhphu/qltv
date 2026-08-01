package fpt.training.qltv.dto.request;

import fpt.training.qltv.entity.BookStatus;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBookRequest {

    @Pattern(regexp = ".*\\S.*", message = "Tên sách không được để trống")
    private String title;

    @Pattern(regexp = ".*\\S.*", message = "ISBN không được để trống")
    private String isbn;
    private String description;
    private Integer publishYear;
    private String language;
    private Integer totalCopies;
    private BookStatus status;
    private List<Long> categoryIds;
    private List<Long> authorIds;
}
