package fpt.training.qltv.service;

import fpt.training.qltv.dto.request.CreateReviewRequest;
import fpt.training.qltv.dto.response.PageResponse;
import fpt.training.qltv.dto.response.ReviewResponse;

public interface ReviewService {

    ReviewResponse createReview(CreateReviewRequest request, Long userId);

    ReviewResponse getMyReview(Long userId, Long bookId);

    PageResponse<ReviewResponse> getByBook(Long bookId, int page, int size);

    void deleteReview(Long reviewId, Long userId);
}
