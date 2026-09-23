package fpt.training.qltv.repository.projection;

import fpt.training.qltv.entity.Role;
import java.time.LocalDateTime;

/**
 * Interface-based projection cho các query đọc danh sách user. Loại bỏ field {@code password} khỏi
 * SQL SELECT — bảo mật và hiệu năng.
 */
public interface UserSummaryProjection {

    Long getId();

    String getUsername();

    String getEmail();

    Role getRole();

    boolean isActive();

    LocalDateTime getCreatedAt();

    LocalDateTime getUpdatedAt();
}
