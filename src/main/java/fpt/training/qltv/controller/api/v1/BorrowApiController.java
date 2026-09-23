package fpt.training.qltv.controller.api.v1;

import fpt.training.qltv.dto.paginate.PageResponse;
import fpt.training.qltv.dto.request.BorrowFilterRequest;
import fpt.training.qltv.dto.request.BorrowRequest;
import fpt.training.qltv.dto.response.ApiResponse;
import fpt.training.qltv.dto.response.BorrowRecordResponse;
import fpt.training.qltv.mapper.PageResponseMapper;
import fpt.training.qltv.security.CustomUserDetails;
import fpt.training.qltv.service.BorrowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/borrows")
@RequiredArgsConstructor
public class BorrowApiController {

    private final BorrowService borrowService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<BorrowRecordResponse>> borrow(
            @Valid @RequestBody BorrowRequest request) {

        System.out.println("Received borrow request: " + request);
        Long userId = getCurrentUserId();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                borrowService.borrow(request.getBookId(), userId),
                                "Mượn sách thành công"));
    }

    @PutMapping("/{id}/return")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<BorrowRecordResponse>> returnBook(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(
                ApiResponse.success(borrowService.returnBook(id, userId), "Trả sách thành công"));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<PageResponse<BorrowRecordResponse>>> getMyBorrows(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        PageResponse<BorrowRecordResponse> pageResponse =
                PageResponseMapper.from(borrowService.getMyBorrows(userId, page, size));
        return ResponseEntity.ok(ApiResponse.success(pageResponse, "Thành công"));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Resource> downloadFile(@RequestParam String token) {
        Long userId = getCurrentUserId();
        Resource resource = borrowService.downloadFile(token, userId);
        String filename = resource.getFilename() == null ? "book.pdf" : resource.getFilename();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<BorrowRecordResponse>>> getAllBorrows(
            @ModelAttribute BorrowFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<BorrowRecordResponse> pageResponse =
                PageResponseMapper.from(borrowService.getAllBorrows(filter, page, size));
        return ResponseEntity.ok(ApiResponse.success(pageResponse, "Thành công"));
    }

    /** Helper method lấy userId từ SecurityContext */
    private Long getCurrentUserId() {
        CustomUserDetails userDetails =
                (CustomUserDetails)
                        SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (userDetails == null) {
            throw new RuntimeException("User not authenticated");
        }

        return userDetails.getId();
    }
}
