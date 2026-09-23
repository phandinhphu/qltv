package fpt.training.qltv.repository.projection;

import java.time.LocalDateTime;

/**
 * Interface-based projection cho các query đọc danh sách review. Dùng nested sub-interface để
 * Spring Data JPA tự sinh JOIN và chỉ SELECT các cột cần.
 */
public interface ReviewSummaryProjection {

    Long getId();

    Integer getRating();

    String getComment();

    boolean isVisible();

    LocalDateTime getCreatedAt();

    UserView getUser();

    BookView getBook();

    interface UserView {
        Long getId();

        String getUsername();
    }

    interface BookView {
        Long getId();
    }
}
