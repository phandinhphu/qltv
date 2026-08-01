package fpt.training.qltv.dto.response;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryResponse {

    private Long id;
    private String name;
    private String description;
    private String slug;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
