package fpt.training.qltv.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCategoryRequest {

    @Pattern(regexp = ".*\\S.*", message = "Tên danh mục không được để trống")
    private String name;

    private String description;
}
