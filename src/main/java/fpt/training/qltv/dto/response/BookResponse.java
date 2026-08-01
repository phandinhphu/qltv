package fpt.training.qltv.dto.response;

import fpt.training.qltv.entity.BookStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookResponse {

    private Long id;
    private String title;
    private String isbn;
    private String description;
    private String coverImageUrl;
    private String fileUrl;
    private Integer publishYear;
    private String language;
    private Integer totalCopies;
    private Integer availableCopies;
    private BookStatus status;
    private List<String> categoryNames;
    private List<String> authorNames;
    private Double avgRating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
