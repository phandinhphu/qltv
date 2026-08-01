package fpt.training.qltv.service;

import fpt.training.qltv.dto.request.RegisterRequest;
import fpt.training.qltv.dto.request.UpdateProfileRequest;
import fpt.training.qltv.dto.response.PageResponse;
import fpt.training.qltv.dto.response.UserResponse;

public interface UserService {

    UserResponse register(RegisterRequest request);

    PageResponse<UserResponse> findAll(int page, int size);

    UserResponse findById(Long id);

    UserResponse toggleActive(Long id);

    UserResponse getCurrentUser(Long userId);

    UserResponse updateProfile(Long userId, UpdateProfileRequest request);
}
