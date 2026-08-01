package fpt.training.qltv.repository.projection;

import fpt.training.qltv.entity.BorrowStatus;
import java.time.LocalDateTime;

/**
 * Interface-based projection dùng cho {@code findByDownloadToken} — chỉ đọc các field
 * cần thiết để validate token và lấy đường dẫn file (read-only, không mutate state).
 */
public interface BorrowTokenProjection {

    Long getId();

    BorrowStatus getStatus();

    LocalDateTime getTokenExpiredAt();

    UserView getUser();

    BookView getBook();

    interface UserView {
        Long getId();
    }

    interface BookView {
        String getFileUrl();
    }
}
