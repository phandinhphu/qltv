package fpt.training.qltv.service.impl;

import fpt.training.qltv.dto.request.RegisterRequest;
import fpt.training.qltv.dto.request.UpdateProfileRequest;
import fpt.training.qltv.dto.response.PageResponse;
import fpt.training.qltv.dto.response.UserResponse;
import fpt.training.qltv.entity.Role;
import fpt.training.qltv.entity.User;
import fpt.training.qltv.exception.common.BusinessException;
import fpt.training.qltv.exception.common.ResourceNotFoundException;
import fpt.training.qltv.repository.UserRepository;
import fpt.training.qltv.repository.projection.UserSummaryProjection;
import fpt.training.qltv.service.UserService;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "dashboard", allEntries = true)
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new BusinessException("Username đã tồn tại");
        }

        if (userRepository.existsByEmail(request.getEmail().trim())) {
            throw new BusinessException("Email đã tồn tại");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<UserResponse> result = userRepository.findAllProjectedBy(pageable).map(this::toResponse);
        return PageResponse.of(result);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return toResponse(getUserOrThrow(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "dashboard", allEntries = true)
    public UserResponse toggleActive(Long id) {
        User user = getUserOrThrow(id);
        user.setActive(!user.isActive());
        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        return toResponse(getUserOrThrow(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "dashboard", allEntries = true)
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getUserOrThrow(userId);
        if (request.getUsername() != null) {
            String trimmedUsername = request.getUsername().trim();
            userRepository.findByUsername(trimmedUsername)
                    .filter(existing -> !existing.getId().equals(user.getId()))
                    .ifPresent(existing -> {
                        throw new BusinessException("Username đã tồn tại");
                    });
            user.setUsername(trimmedUsername);
        }

        if (request.getEmail() != null) {
            String trimmedEmail = request.getEmail().trim();
            userRepository.findByEmail(trimmedEmail)
                    .filter(existing -> !existing.getId().equals(user.getId()))
                    .ifPresent(existing -> {
                        throw new BusinessException("Email đã tồn tại");
                    });
            user.setEmail(trimmedEmail);
        }

        handlePasswordChange(request, user);

        user.setUpdatedAt(LocalDateTime.now());
        return toResponse(userRepository.save(user));
    }

    private User getUserOrThrow(Long id) {
        if (id == null) {
            throw new BusinessException("Id user không được để trống");
        }
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setActive(user.isActive());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }

    private UserResponse toResponse(UserSummaryProjection projection) {
        UserResponse response = new UserResponse();
        response.setId(projection.getId());
        response.setUsername(projection.getUsername());
        response.setEmail(projection.getEmail());
        response.setRole(projection.getRole());
        response.setActive(projection.isActive());
        response.setCreatedAt(projection.getCreatedAt());
        response.setUpdatedAt(projection.getUpdatedAt());
        return response;
    }

    private void handlePasswordChange(UpdateProfileRequest request, User user) {
        boolean hasNewPassword = !isBlank(request.getNewPassword()) || !isBlank(request.getConfirmPassword());
        boolean hasCurrentPassword = !isBlank(request.getCurrentPassword());

        if (!hasNewPassword && !hasCurrentPassword) {
            return;
        }

        if (!hasCurrentPassword) {
            throw new BusinessException("Vui lòng nhập mật khẩu hiện tại để đổi mật khẩu");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Mật khẩu hiện tại không đúng");
        }

        if (isBlank(request.getNewPassword())) {
            throw new BusinessException("Mật khẩu mới không được để trống");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Xác nhận mật khẩu mới không khớp");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
