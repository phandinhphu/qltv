package fpt.training.qltv.service.impl;

import fpt.training.qltv.entity.RefreshToken;
import fpt.training.qltv.entity.User;
import fpt.training.qltv.exception.common.BusinessException;
import fpt.training.qltv.repository.RefreshTokenRepository;
import fpt.training.qltv.repository.UserRepository;
import fpt.training.qltv.service.RefreshTokenRotationResult;
import fpt.training.qltv.service.RefreshTokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final long REFRESH_TOKEN_DAYS = 30L;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String issueRefreshToken(String username) {
        User user = getActiveUser(username);

        String rawToken = generateRawToken();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        toRefreshToken(user, rawToken, now);
        return rawToken;
    }

    private void toRefreshToken(User user, String rawToken, LocalDateTime now) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setExpiresAt(now.plusDays(REFRESH_TOKEN_DAYS));
        refreshToken.setCreatedAt(now);
        refreshToken.setUpdatedAt(now);

        refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefreshTokenRotationResult rotateRefreshToken(String refreshToken) {
        RefreshToken currentToken = findValidToken(refreshToken);
        User user = currentToken.getUser();
        if (!user.isActive()) {
            throw new BadCredentialsException("Tài khoản đã bị khóa");
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        currentToken.setRevokedAt(now);
        currentToken.setUpdatedAt(now);
        refreshTokenRepository.save(currentToken);

        String newRawToken = generateRawToken();
        toRefreshToken(user, newRawToken, now);

        return new RefreshTokenRotationResult(user, newRawToken);
    }

    private User getActiveUser(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessException("Username không được để trống");
        }

        User user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(
                                () ->
                                        new BadCredentialsException(
                                                "Không tìm thấy tài khoản hợp lệ"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Tài khoản đã bị khóa");
        }

        return user;
    }

    private RefreshToken findValidToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadCredentialsException("Refresh token không hợp lệ");
        }

        String tokenHash = hashToken(refreshToken);
        RefreshToken token =
                refreshTokenRepository
                        .findByTokenHashAndRevokedAtIsNull(tokenHash)
                        .orElseThrow(
                                () -> new BadCredentialsException("Refresh token không hợp lệ"));

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (token.getExpiresAt().isBefore(now)) {
            token.setRevokedAt(now);
            token.setUpdatedAt(now);
            refreshTokenRepository.save(token);
            throw new BadCredentialsException("Refresh token đã hết hạn");
        }

        return token;
    }

    private String generateRawToken() {
        return UUID.randomUUID() + "." + UUID.randomUUID();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Không thể băm refresh token", e);
        }
    }
}
