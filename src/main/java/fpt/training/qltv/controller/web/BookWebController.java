package fpt.training.qltv.controller.web;

import fpt.training.qltv.dto.request.BookFilterRequest;
import fpt.training.qltv.dto.request.CreateReviewRequest;
import fpt.training.qltv.dto.response.BookDetailResponse;
import fpt.training.qltv.dto.response.BookResponse;
import fpt.training.qltv.dto.response.BorrowRecordResponse;
import fpt.training.qltv.dto.response.CategoryResponse;
import fpt.training.qltv.dto.response.PageResponse;
import fpt.training.qltv.dto.response.ReviewResponse;
import fpt.training.qltv.entity.BorrowStatus;
import fpt.training.qltv.exception.common.BusinessException;
import fpt.training.qltv.security.CustomUserDetails;
import fpt.training.qltv.service.BookService;
import fpt.training.qltv.service.BorrowService;
import fpt.training.qltv.service.CategoryService;
import fpt.training.qltv.service.ReviewService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookWebController {

    private final BookService bookService;
    private final CategoryService categoryService;
    private final BorrowService borrowService;
    private final ReviewService reviewService;

    @GetMapping
    public String findAll(@ModelAttribute BookFilterRequest filter,
                          @RequestParam(defaultValue = "0") int page,
                          Model model) {
        PageResponse<BookResponse> pageData = bookService.findAll(filter, page, 12);
        List<CategoryResponse> categories = categoryService.findAll();

        model.addAttribute("pageData", pageData);
        model.addAttribute("filter", filter);
        model.addAttribute("categories", categories);
        model.addAttribute("queryParams", buildQueryParams(filter));
        return "book/list";
    }

    @GetMapping("/{id}")
    public String findById(@PathVariable Long id, Model model) {
        BookDetailResponse book = bookService.findById(id);
        List<ReviewResponse> reviews = book.getReviews() != null ? book.getReviews() : Collections.emptyList();
        Long userId = getCurrentUserId();
        ReviewResponse myReview = reviewService.getMyReview(userId, id);
        BorrowRecordResponse activeBorrow = getActiveBorrow(userId, id);

        model.addAttribute("book", book);
        model.addAttribute("reviews", reviews);
        model.addAttribute("userBorrowStatus", hasBorrowedBook(book.getId()));
        model.addAttribute("canRead", activeBorrow != null);
        model.addAttribute("downloadToken", activeBorrow != null ? activeBorrow.getDownloadToken() : null);
        model.addAttribute("currentUserId", userId);
        model.addAttribute("myReview", myReview);
        return "book/detail";
    }

    @PostMapping("/{id}/reviews")
    public String submitReview(@PathVariable Long id,
                               @Valid @ModelAttribute CreateReviewRequest request,
                               RedirectAttributes redirectAttributes) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            reviewService.createReview(request, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu đánh giá");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/books/" + id;
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

    private boolean hasBorrowedBook(Long bookId) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return false;
        }

        // Kiểm tra người dùng đã từng mượn sách này chưa
        List<BorrowRecordResponse> borrows = borrowService.findByUserIdAndBookId(userId, bookId);
        return borrows != null && !borrows.isEmpty();
    }

    private BorrowRecordResponse getActiveBorrow(Long userId, Long bookId) {
        if (userId == null || bookId == null) {
            return null;
        }

        List<BorrowRecordResponse> borrows = borrowService.findByUserIdAndBookId(userId, bookId);
        if (borrows == null || borrows.isEmpty()) {
            return null;
        }

        return borrows.stream()
            .filter(borrow -> borrow.getStatus() == BorrowStatus.BORROWING)
            .filter(BorrowRecordResponse::isHasValidToken)
            .filter(BorrowRecordResponse::isHasFile)
            .findFirst()
            .orElse(null);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return null;
        }
        return userDetails.getId();
    }
}
