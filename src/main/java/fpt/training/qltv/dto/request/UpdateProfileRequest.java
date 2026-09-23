package fpt.training.qltv.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @Pattern(regexp = ".*\\S.*", message = "Username không được để trống")
    private String username;

    @Pattern(regexp = ".*\\S.*", message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    private String currentPassword;
    private String newPassword;
    private String confirmPassword;
}
