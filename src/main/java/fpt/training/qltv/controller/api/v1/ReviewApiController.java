package fpt.training.qltv.controller.api.v1;

import fpt.training.qltv.dto.paginate.PageResponse;
import fpt.training.qltv.dto.request.CreateReviewRequest;
import fpt.training.qltv.dto.response.ApiResponse;
import fpt.training.qltv.dto.response.ReviewResponse;
import fpt.training.qltv.mapper.PageResponseMapper;
import fpt.training.qltv.security.CustomUserDetails;
import fpt.training.qltv.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewApiController {

    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody CreateReviewRequest request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                reviewService.createReview(request, userId),
                                "Tạo đánh giá thành công"));
    }

    @GetMapping("/book/{bookId}")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getByBook(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<ReviewResponse> pageResponse =
                PageResponseMapper.from(reviewService.getByBook(bookId, page, size));
        return ResponseEntity.ok(ApiResponse.success(pageResponse, "Thành công"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        reviewService.deleteReview(id, userId);
        return ResponseEntity.ok(ApiResponse.successWithoutData("Xóa đánh giá thành công"));
    }

    /** Helper method lấy userId từ SecurityContext */
    private Long getCurrentUserId() {
        CustomUserDetails userDetails =
                (CustomUserDetails)
                        SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userDetails.getId();
    }
}
