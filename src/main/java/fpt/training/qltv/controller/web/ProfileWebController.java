package fpt.training.qltv.controller.web;

import fpt.training.qltv.dto.request.UpdateProfileRequest;
import fpt.training.qltv.dto.response.UserResponse;
import fpt.training.qltv.exception.common.BusinessException;
import fpt.training.qltv.security.CustomUserDetails;
import fpt.training.qltv.service.UserService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileWebController {

    private final UserService userService;

    @GetMapping
    public String viewProfile(Model model) {
        Long userId = getCurrentUserId();
        UserResponse user = userService.getCurrentUser(userId);

        UpdateProfileRequest profile = new UpdateProfileRequest();
        profile.setUsername(user.getUsername());
        profile.setEmail(user.getEmail());

        model.addAttribute("profile", profile);
        model.addAttribute("userInfo", user);
        return "user/profile";
    }

    @PostMapping
    public String updateProfile(@Valid @ModelAttribute("profile") UpdateProfileRequest request,
                                RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId();
            userService.updateProfile(userId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật profile thành công");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/profile";
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BusinessException("Không tìm thấy thông tin người dùng đăng nhập");
        }
        return userDetails.getId();
    }
}
