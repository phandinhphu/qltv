package fpt.training.qltv.dto.request;

import fpt.training.qltv.entity.BookStatus;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBookRequest {

    @NotBlank(message = "Tên sách không được để trống")
    private String title;

    @NotBlank(message = "ISBN không được để trống")
    private String isbn;
    private String description;
    private Integer publishYear;
    private String language;
    private Integer totalCopies;
    private BookStatus status;
    private List<Long> categoryIds;
    private List<Long> authorIds;
}
