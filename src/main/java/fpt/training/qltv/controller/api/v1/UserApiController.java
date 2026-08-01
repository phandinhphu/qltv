package fpt.training.qltv.controller.api.v1;

import fpt.training.qltv.dto.paginate.PageResponse;
import fpt.training.qltv.dto.response.ApiResponse;
import fpt.training.qltv.dto.response.UserResponse;
import fpt.training.qltv.mapper.PageResponseMapper;
import fpt.training.qltv.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserApiController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<UserResponse> pageResponse = PageResponseMapper.from(
            userService.findAll(page, size)
        );
        return ResponseEntity.ok(ApiResponse.success(pageResponse, "Thành công"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.findById(id), "Thành công"));
    }

    @PutMapping("/{id}/toggle-active")
    public ResponseEntity<ApiResponse<UserResponse>> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.toggleActive(id), "Cập nhật trạng thái thành công"));
    }
}
