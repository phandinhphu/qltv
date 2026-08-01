package fpt.training.qltv.service;

public interface RefreshTokenService {

    String issueRefreshToken(String username);

    RefreshTokenRotationResult rotateRefreshToken(String refreshToken);
}
