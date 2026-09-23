package fpt.training.qltv.repository;

import fpt.training.qltv.entity.Review;
import fpt.training.qltv.repository.projection.ReviewSummaryProjection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReviewRepository
        extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    Optional<Review> findByUserIdAndBookId(Long userId, Long bookId);

    List<Review> findByBookIdAndVisibleTrue(Long bookId);

    Page<ReviewSummaryProjection> findBy(Specification<Review> spec, Pageable pageable);

    List<ReviewSummaryProjection> findProjectedByBookIdAndVisibleTrue(Long bookId);
}
