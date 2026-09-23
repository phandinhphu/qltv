package fpt.training.qltv.repository.projection;

import fpt.training.qltv.entity.BookStatus;
import java.time.LocalDateTime;

/**
 * Interface-based projection cho các query đọc danh sách sách. Spring Data JPA tự sinh proxy tại
 * runtime, SQL chỉ SELECT đúng các cột này.
 */
public interface BookSummaryProjection {

    Long getId();

    String getTitle();

    String getIsbn();

    String getDescription();

    String getCoverImageUrl();

    String getFileUrl();

    Integer getPublishYear();

    String getLanguage();

    Integer getTotalCopies();

    Integer getAvailableCopies();

    BookStatus getStatus();

    LocalDateTime getCreatedAt();

    LocalDateTime getUpdatedAt();
}
