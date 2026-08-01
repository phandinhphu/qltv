package fpt.training.qltv.repository.projection;

import java.time.LocalDateTime;

/**
 * Interface-based projection cho các query đọc danh sách tác giả.
 */
public interface AuthorSummaryProjection {

    Long getId();

    String getName();

    String getBio();

    String getAvatarUrl();

    LocalDateTime getCreatedAt();

    LocalDateTime getUpdatedAt();
}
