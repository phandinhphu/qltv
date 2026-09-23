package fpt.training.qltv.controller.web.admin;

import fpt.training.qltv.dto.request.BookFilterRequest;
import fpt.training.qltv.dto.request.CreateBookRequest;
import fpt.training.qltv.dto.request.UpdateBookRequest;
import fpt.training.qltv.dto.response.BookDetailResponse;
import fpt.training.qltv.dto.response.BookResponse;
import fpt.training.qltv.dto.response.PageResponse;
import fpt.training.qltv.entity.BookStatus;
import fpt.training.qltv.service.AuthorService;
import fpt.training.qltv.service.BookService;
import fpt.training.qltv.service.CategoryService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/admin/books")
@RequiredArgsConstructor
public class AdminBookWebController {

    private final BookService bookService;
    private final CategoryService categoryService;
    private final AuthorService authorService;

    @GetMapping
    public String index(
            @ModelAttribute BookFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        PageResponse<BookResponse> pageData = bookService.findAll(filter, page, 10);
        model.addAttribute("pageData", pageData);
        model.addAttribute("filter", filter);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("queryParams", buildQueryParams(filter));
        return "admin/book/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("request", new CreateBookRequest());
        model.addAttribute("mode", "create");
        model.addAttribute("bookId", null);
        populateFormOptions(model);
        return "admin/book/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute CreateBookRequest request,
            @RequestParam(required = false) MultipartFile cover,
            @RequestParam(required = false) MultipartFile file,
            RedirectAttributes redirectAttributes) {
        try {
            bookService.create(request, cover, file);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo sách thành công");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/books";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        BookDetailResponse book = bookService.findById(id);
        UpdateBookRequest request = new UpdateBookRequest();
        request.setTitle(book.getTitle());
        request.setIsbn(book.getIsbn());
        request.setDescription(book.getDescription());
        request.setPublishYear(book.getPublishYear());
        request.setLanguage(book.getLanguage());
        request.setTotalCopies(book.getTotalCopies());
        request.setStatus(book.getStatus());
        request.setCategoryIds(book.getCategoryIds());
        request.setAuthorIds(book.getAuthorIds());

        model.addAttribute("request", request);
        model.addAttribute("mode", "edit");
        model.addAttribute("bookId", id);
        populateFormOptions(model);
        return "admin/book/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute UpdateBookRequest request,
            @RequestParam(required = false) MultipartFile cover,
            @RequestParam(required = false) MultipartFile file,
            RedirectAttributes redirectAttributes) {
        try {
            bookService.update(id, request, cover, file);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật sách thành công");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/books";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa sách thành công");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/books";
    }

    @GetMapping("/trash")
    public String trash(@RequestParam(defaultValue = "0") int page, Model model) {
        PageResponse<BookResponse> pageData = bookService.findAllDeleted(page, 10);
        model.addAttribute("pageData", pageData);
        model.addAttribute("categories", categoryService.findAll());
        return "admin/book/trash";
    }

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookService.restore(id);
            redirectAttributes.addFlashAttribute("successMessage", "Khôi phục sách thành công");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/books/trash";
    }

    @PostMapping("/{id}/force")
    public String forceDelete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookService.forceDelete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa vĩnh viễn sách thành công");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/books/trash";
    }

    private void populateFormOptions(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("authors", authorService.findAll(null, 0, 100).getContent());
        model.addAttribute("statuses", BookStatus.values());
    }

    private Map<String, Object> buildQueryParams(BookFilterRequest filter) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (filter == null) {
            return params;
        }
        if (filter.getTitle() != null && !filter.getTitle().isBlank()) {
            params.put("title", filter.getTitle().trim());
        }
        if (filter.getCategoryId() != null) {
            params.put("categoryId", filter.getCategoryId());
        }
        if (filter.getAuthorId() != null) {
            params.put("authorId", filter.getAuthorId());
        }
        if (filter.getLanguage() != null && !filter.getLanguage().isBlank()) {
            params.put("language", filter.getLanguage().trim());
        }
        if (filter.getStatus() != null) {
            params.put("status", filter.getStatus().name());
        }
        if (filter.getPublishYear() != null) {
            params.put("publishYear", filter.getPublishYear());
        }
        return params;
    }
}
