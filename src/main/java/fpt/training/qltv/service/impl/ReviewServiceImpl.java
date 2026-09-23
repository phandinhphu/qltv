package fpt.training.qltv.service.impl;

import fpt.training.qltv.dto.request.CreateReviewRequest;
import fpt.training.qltv.dto.response.PageResponse;
import fpt.training.qltv.dto.response.ReviewResponse;
import fpt.training.qltv.entity.Book;
import fpt.training.qltv.entity.Review;
import fpt.training.qltv.entity.Role;
import fpt.training.qltv.entity.User;
import fpt.training.qltv.exception.common.BusinessException;
import fpt.training.qltv.exception.common.ResourceNotFoundException;
import fpt.training.qltv.repository.BookRepository;
import fpt.training.qltv.repository.BorrowRecordRepository;
import fpt.training.qltv.repository.ReviewRepository;
import fpt.training.qltv.repository.UserRepository;
import fpt.training.qltv.repository.projection.ReviewSummaryProjection;
import fpt.training.qltv.service.ReviewService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "books", key = "#request.bookId")
    public ReviewResponse createReview(CreateReviewRequest request, Long userId) {
        User user = getUserOrThrow(userId);
        Book book = getBookOrThrow(request.getBookId());

        if (!borrowRecordRepository.existsByUserIdAndBookId(userId, request.getBookId())) {
            throw new BusinessException("Bạn phải mượn sách trước khi đánh giá");
        }

        Review review =
                reviewRepository
                        .findByUserIdAndBookId(userId, request.getBookId())
                        .orElseGet(Review::new);

        review.setUser(user);
        review.setBook(book);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setVisible(true);
        review.setCreatedAt(review.getId() == null ? LocalDateTime.now() : review.getCreatedAt());
        review.setUpdatedAt(LocalDateTime.now());

        return toResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getMyReview(Long userId, Long bookId) {
        if (userId == null || bookId == null) {
            return null;
        }
        return reviewRepository
                .findByUserIdAndBookId(userId, bookId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getByBook(Long bookId, int page, int size) {
        if (bookId == null) {
            throw new BusinessException("Id sách không được để trống");
        }
        Pageable pageable =
                PageRequest.of(
                        Math.max(page, 0),
                        Math.max(size, 1),
                        Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Review> specification =
                (root, query, cb) -> {
                    query.distinct(true);
                    return cb.and(
                            cb.equal(root.get("book").get("id"), bookId),
                            cb.isTrue(root.get("visible")));
                };
        // Dùng projection — Spring Data JPA tự JOIN user và book, chỉ SELECT cột cần.
        Page<ReviewResponse> result =
                reviewRepository.findBy(specification, pageable).map(this::toResponse);
        return PageResponse.of(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(Long reviewId, Long userId) {
        Review review = getReviewOrThrow(reviewId);
        User user = getUserOrThrow(userId);

        boolean isAdmin = user.getRole() == Role.ADMIN;
        boolean isOwner = review.getUser().getId().equals(userId);

        if (!isAdmin && !isOwner) {
            throw new BusinessException("Bạn không có quyền xóa review này");
        }

        Long bookId = review.getBook().getId();
        reviewRepository.delete(review);

        // Evict books cache since average rating has changed
        org.springframework.cache.Cache booksCache = cacheManager.getCache("books");
        if (booksCache != null) {
            booksCache.evict(bookId);
        }
    }

    private User getUserOrThrow(Long id) {
        if (id == null) {
            throw new BusinessException("Id user không được để trống");
        }
        return userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private Book getBookOrThrow(Long id) {
        return bookRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id));
    }

    private Review getReviewOrThrow(Long id) {
        if (id == null) {
            throw new BusinessException("Id review không được để trống");
        }
        return reviewRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review", id));
    }

    private ReviewResponse toResponse(Review review) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setVisible(review.isVisible());
        response.setUserId(review.getUser().getId());
        response.setUsername(review.getUser().getUsername());
        response.setBookId(review.getBook().getId());
        response.setCreatedAt(review.getCreatedAt());
        return response;
    }

    private ReviewResponse toResponse(ReviewSummaryProjection projection) {
        ReviewResponse response = new ReviewResponse();
        response.setId(projection.getId());
        response.setRating(projection.getRating());
        response.setComment(projection.getComment());
        response.setVisible(projection.isVisible());
        response.setUserId(projection.getUser().getId());
        response.setUsername(projection.getUser().getUsername());
        response.setBookId(projection.getBook().getId());
        response.setCreatedAt(projection.getCreatedAt());
        return response;
    }
}
