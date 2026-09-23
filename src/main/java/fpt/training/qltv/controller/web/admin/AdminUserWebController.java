package fpt.training.qltv.controller.web.admin;

import fpt.training.qltv.dto.response.PageResponse;
import fpt.training.qltv.dto.response.UserResponse;
import fpt.training.qltv.service.UserService;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserWebController {

    private final UserService userService;

    @GetMapping
    public String index(@RequestParam(defaultValue = "0") int page, Model model) {
        PageResponse<UserResponse> pageData = userService.findAll(page, 10);
        model.addAttribute("pageData", pageData);
        model.addAttribute("queryParams", Collections.emptyMap());
        return "admin/user/list";
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.toggleActive(id);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Cập nhật trạng thái tài khoản thành công");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }
}
