package fpt.training.qltv.controller.api.v1;

import fpt.training.qltv.dto.request.BookFilterRequest;
import fpt.training.qltv.dto.request.CreateBookRequest;
import fpt.training.qltv.dto.request.UpdateBookRequest;
import fpt.training.qltv.dto.paginate.PageResponse;
import fpt.training.qltv.dto.response.ApiResponse;
import fpt.training.qltv.dto.response.BookDetailResponse;
import fpt.training.qltv.dto.response.BookResponse;
import fpt.training.qltv.mapper.PageResponseMapper;
import fpt.training.qltv.service.BookService;
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
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookApiController {

    private final BookService bookService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BookResponse>>> findAll(
            @ModelAttribute BookFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<BookResponse> pageResponse = PageResponseMapper.from(
                bookService.findAll(filter, page, size));
        return ResponseEntity.ok(ApiResponse.success(pageResponse, "Thành công"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookDetailResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(bookService.findById(id), "Thành công"));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BookResponse>> create(
            @Valid @ModelAttribute CreateBookRequest request,
            @RequestParam(required = false) MultipartFile cover,
            @RequestParam(required = false) MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(bookService.create(request, cover, file), "Tạo sách thành công"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BookResponse>> update(
            @PathVariable Long id,
            @Valid @ModelAttribute UpdateBookRequest request,
            @RequestParam(required = false) MultipartFile cover,
            @RequestParam(required = false) MultipartFile file) {
        return ResponseEntity
                .ok(ApiResponse.success(bookService.update(id, request, cover, file), "Cập nhật sách thành công"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ResponseEntity.ok(ApiResponse.successWithoutData("Xóa sách thành công"));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> restore(@PathVariable Long id) {
        bookService.restore(id);
        return ResponseEntity.ok(ApiResponse.successWithoutData("Khôi phục sách thành công"));
    }

    @DeleteMapping("/{id}/force")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> forceDelete(@PathVariable Long id) {
        bookService.forceDelete(id);
        return ResponseEntity.ok(ApiResponse.successWithoutData("Xóa vĩnh viễn sách thành công"));
    }
}
