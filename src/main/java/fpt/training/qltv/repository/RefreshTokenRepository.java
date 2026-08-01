package fpt.training.qltv.repository;

import fpt.training.qltv.entity.RefreshToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    List<RefreshToken> findAllByUserIdAndRevokedAtIsNull(Long userId);

    List<RefreshToken> findAllByUserIdAndExpiresAtBefore(Long userId, LocalDateTime expiresAt);
}
