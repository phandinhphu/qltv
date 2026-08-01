package fpt.training.qltv.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    private String accessToken;
    private String username;
    private String role;
    private long expiredAt;
}
