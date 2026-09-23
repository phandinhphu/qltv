package fpt.training.qltv.controller.api.v1;

import fpt.training.qltv.dto.paginate.PageResponse;
import fpt.training.qltv.dto.request.AuthorFilterRequest;
import fpt.training.qltv.dto.request.CreateAuthorRequest;
import fpt.training.qltv.dto.request.UpdateAuthorRequest;
import fpt.training.qltv.dto.response.ApiResponse;
import fpt.training.qltv.dto.response.AuthorResponse;
import fpt.training.qltv.mapper.PageResponseMapper;
import fpt.training.qltv.service.AuthorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/authors")
@RequiredArgsConstructor
public class AuthorApiController {

    private final AuthorService authorService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuthorResponse>>> findAll(
            @ModelAttribute AuthorFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<AuthorResponse> pageResponse =
                PageResponseMapper.from(authorService.findAll(filter, page, size));
        return ResponseEntity.ok(ApiResponse.success(pageResponse, "Thành công"));
    }

    @GetMapping("/trash")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<AuthorResponse>>> findAllDeleted(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<AuthorResponse> pageResponse =
                PageResponseMapper.from(authorService.findAllDeleted(page, size));
        return ResponseEntity.ok(ApiResponse.success(pageResponse, "Thành công"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AuthorResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(authorService.findById(id), "Thành công"));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AuthorResponse>> create(
            @Valid @ModelAttribute CreateAuthorRequest request,
            @RequestParam(required = false) MultipartFile avatar) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                authorService.create(request, avatar), "Tạo tác giả thành công"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AuthorResponse>> update(
            @PathVariable Long id,
            @Valid @ModelAttribute UpdateAuthorRequest request,
            @RequestParam(required = false) MultipartFile avatar) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        authorService.update(id, request, avatar), "Cập nhật tác giả thành công"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        authorService.delete(id);
        return ResponseEntity.ok(ApiResponse.successWithoutData("Xóa tác giả thành công"));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> restore(@PathVariable Long id) {
        authorService.restore(id);
        return ResponseEntity.ok(ApiResponse.successWithoutData("Khôi phục tác giả thành công"));
    }

    @DeleteMapping("/{id}/force")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> forceDelete(@PathVariable Long id) {
        authorService.forceDelete(id);
        return ResponseEntity.ok(
                ApiResponse.successWithoutData("Xóa vĩnh viễn tác giả thành công"));
    }
}
