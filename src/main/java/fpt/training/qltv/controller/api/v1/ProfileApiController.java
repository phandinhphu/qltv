package fpt.training.qltv.controller.api.v1;

import fpt.training.qltv.dto.request.UpdateProfileRequest;
import fpt.training.qltv.dto.response.ApiResponse;
import fpt.training.qltv.dto.response.UserResponse;
import fpt.training.qltv.exception.common.BusinessException;
import fpt.training.qltv.security.CustomUserDetails;
import fpt.training.qltv.service.UserService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileApiController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getProfile() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(userService.getCurrentUser(userId), "Thành công"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @NotNull @RequestBody UpdateProfileRequest request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(userService.updateProfile(userId, request), "Cập nhật hồ sơ thành công"));
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BusinessException("Không tìm thấy thông tin người dùng đăng nhập");
        }
        return userDetails.getId();
    }
}
