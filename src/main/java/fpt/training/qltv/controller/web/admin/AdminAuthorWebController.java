package fpt.training.qltv.controller.web.admin;

import fpt.training.qltv.dto.request.AuthorFilterRequest;
import fpt.training.qltv.dto.request.CreateAuthorRequest;
import fpt.training.qltv.dto.request.UpdateAuthorRequest;
import fpt.training.qltv.dto.response.AuthorResponse;
import fpt.training.qltv.dto.response.PageResponse;
import fpt.training.qltv.service.AuthorService;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/authors")
@RequiredArgsConstructor
public class AdminAuthorWebController {

    private final AuthorService authorService;

    @GetMapping
    public String index(@ModelAttribute AuthorFilterRequest filter,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        PageResponse<AuthorResponse> pageData = authorService.findAll(filter, page, 10);
        model.addAttribute("pageData", pageData);
        model.addAttribute("filter", filter);
        model.addAttribute("queryParams", buildQueryParams(filter));
        return "admin/author/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("request", new CreateAuthorRequest());
        model.addAttribute("mode", "create");
        model.addAttribute("authorId", null);
        return "admin/author/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute CreateAuthorRequest request,
                         @RequestParam(required = false) MultipartFile avatar,
                         RedirectAttributes redirectAttributes) {
        try {
            authorService.create(request, avatar);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo tác giả thành công");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/authors";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        AuthorResponse author = authorService.findById(id);
        UpdateAuthorRequest request = new UpdateAuthorRequest();
        request.setName(author.getName());
        request.setBio(author.getBio());
        model.addAttribute("request", request);
        model.addAttribute("mode", "edit");
        model.addAttribute("authorId", id);
        return "admin/author/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute UpdateAuthorRequest request,
                         @RequestParam(required = false) MultipartFile avatar,
                         RedirectAttributes redirectAttributes) {
        try {
            authorService.update(id, request, avatar);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật tác giả thành công");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/authors";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            authorService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa tác giả thành công");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/authors";
    }

    @GetMapping("/trash")
    public String trash(@ModelAttribute AuthorFilterRequest filter,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        if (filter == null) {
            filter = new AuthorFilterRequest();
        }
        filter.setDeleted(true);
        PageResponse<AuthorResponse> pageData = authorService.findAll(filter, page, 10);
        model.addAttribute("pageData", pageData);
        model.addAttribute("filter", filter);
        model.addAttribute("queryParams", buildQueryParams(filter));
        return "admin/author/trash";
    }

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            authorService.restore(id);
            redirectAttributes.addFlashAttribute("successMessage", "Khôi phục tác giả thành công");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/authors/trash";
    }

    @PostMapping("/{id}/force")
    public String forceDelete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            authorService.forceDelete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa vĩnh viễn tác giả thành công");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/authors/trash";
    }

    private Map<String, Object> buildQueryParams(AuthorFilterRequest filter) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (filter == null) {
            return params;
        }
        if (filter.getName() != null && !filter.getName().isBlank()) {
            params.put("name", filter.getName().trim());
        }
        if (filter.getDeleted() != null) {
            params.put("deleted", filter.getDeleted());
        }
        return params;
    }
}
