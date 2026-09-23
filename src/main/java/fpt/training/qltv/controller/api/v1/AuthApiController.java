package fpt.training.qltv.controller.api.v1;

import fpt.training.qltv.dto.request.LoginRequest;
import fpt.training.qltv.dto.request.RegisterRequest;
import fpt.training.qltv.dto.response.ApiResponse;
import fpt.training.qltv.dto.response.LoginResponse;
import fpt.training.qltv.dto.response.UserResponse;
import fpt.training.qltv.security.JwtUtil;
import fpt.training.qltv.service.RefreshTokenRotationResult;
import fpt.training.qltv.service.RefreshTokenService;
import fpt.training.qltv.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Date;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthApiController {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public AuthApiController(
            UserService userService,
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @NotNull @RequestBody RegisterRequest request) {
        UserResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Đăng ký thành công"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @NotNull @RequestBody LoginRequest request) {
        Authentication auth =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getUsername(), request.getPassword()));

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String role =
                userDetails.getAuthorities().stream()
                        .findFirst()
                        .map(a -> a.getAuthority())
                        .orElse("");
        String accessToken = jwtUtil.generateToken(userDetails.getUsername(), role);
        String refreshToken = refreshTokenService.issueRefreshToken(userDetails.getUsername());
        Date exp = jwtUtil.extractExpiration(accessToken);

        LoginResponse res = new LoginResponse();
        res.setAccessToken(accessToken);
        res.setUsername(userDetails.getUsername());
        if (role.startsWith("ROLE_")) role = role.substring(5);
        res.setRole(role);
        res.setExpiredAt(exp.getTime());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(refreshToken).toString())
                .body(ApiResponse.success(res, "Đăng nhập thành công"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshAccessToken(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadCredentialsException("Refresh token không hợp lệ");
        }

        RefreshTokenRotationResult rotationResult =
                refreshTokenService.rotateRefreshToken(refreshToken);
        fpt.training.qltv.entity.User user = rotationResult.user();
        String newRefreshToken = rotationResult.refreshToken();
        String role = user.getRole() != null ? "ROLE_" + user.getRole().name() : "";
        String accessToken = jwtUtil.generateToken(user.getUsername(), role);
        Date exp = jwtUtil.extractExpiration(accessToken);

        LoginResponse res = new LoginResponse();
        res.setAccessToken(accessToken);
        res.setUsername(user.getUsername());
        if (role.startsWith("ROLE_")) role = role.substring(5);
        res.setRole(role);
        res.setExpiredAt(exp.getTime());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(newRefreshToken).toString())
                .body(ApiResponse.success(res, "Làm mới access token thành công"));
    }

    private ResponseCookie buildRefreshCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/api/v1/auth")
                .sameSite("Lax")
                .maxAge(java.time.Duration.ofDays(30))
                .build();
    }
}
