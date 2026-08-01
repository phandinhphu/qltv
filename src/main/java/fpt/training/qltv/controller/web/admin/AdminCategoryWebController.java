package fpt.training.qltv.controller.web.admin;

import fpt.training.qltv.dto.request.CreateCategoryRequest;
import fpt.training.qltv.dto.request.UpdateCategoryRequest;
import fpt.training.qltv.dto.response.CategoryResponse;
import fpt.training.qltv.service.CategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryWebController {

    private final CategoryService categoryService;

    @GetMapping
    public String index(Model model) {
        List<CategoryResponse> categories = categoryService.findAll();
        model.addAttribute("categories", categories);
        return "admin/category/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("request", new CreateCategoryRequest());
        model.addAttribute("mode", "create");
        model.addAttribute("categoryId", null);
        return "admin/category/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute CreateCategoryRequest request, RedirectAttributes redirectAttributes) {
        try {
            categoryService.create(request);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo danh mục thành công");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        CategoryResponse category = categoryService.findById(id);
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName(category.getName());
        request.setDescription(category.getDescription());
        model.addAttribute("request", request);
        model.addAttribute("mode", "edit");
        model.addAttribute("categoryId", id);
        return "admin/category/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute UpdateCategoryRequest request,
                         RedirectAttributes redirectAttributes) {
        try {
            categoryService.update(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật danh mục thành công");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa danh mục thành công");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/categories";
    }
}
