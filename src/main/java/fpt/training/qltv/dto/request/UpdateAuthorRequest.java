package fpt.training.qltv.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAuthorRequest {

    @Pattern(regexp = ".*\\S.*", message = "Tên tác giả không được để trống")
    private String name;
    private String bio;
}
