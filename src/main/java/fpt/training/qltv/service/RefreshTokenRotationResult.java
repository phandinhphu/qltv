package fpt.training.qltv.service;

import fpt.training.qltv.entity.User;

public record RefreshTokenRotationResult(User user, String refreshToken) {}
