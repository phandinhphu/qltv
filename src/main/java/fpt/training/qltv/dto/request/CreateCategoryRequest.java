package fpt.training.qltv.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCategoryRequest {

    @NotBlank(message = "Tên danh mục không được để trống")
    private String name;
    private String description;
}
