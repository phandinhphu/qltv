package fpt.training.qltv.controller.web;

import fpt.training.qltv.dto.response.BorrowRecordResponse;
import fpt.training.qltv.dto.response.PageResponse;
import fpt.training.qltv.exception.common.BusinessException;
import fpt.training.qltv.security.CustomUserDetails;
import fpt.training.qltv.service.BorrowService;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/borrows")
@RequiredArgsConstructor
public class BorrowWebController {

    private final BorrowService borrowService;

    @GetMapping("/my")
    public String getMyBorrows(@RequestParam(defaultValue = "0") int page, Model model) {
        Long userId = getCurrentUserId();
        PageResponse<BorrowRecordResponse> pageData = borrowService.getMyBorrows(userId, page, 10);
        model.addAttribute("pageData", pageData);
        model.addAttribute("queryParams", Collections.emptyMap());
        return "borrow/my-list";
    }

    @PostMapping("/{bookId}/borrow")
    public String borrow(@PathVariable Long bookId, RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId();
            borrowService.borrow(bookId, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Mượn sách thành công");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/books/" + bookId;
    }

    @PostMapping("/{id}/return")
    public String returnBook(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId();
            borrowService.returnBook(id, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Trả sách thành công");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/borrows/my";
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String token) {
        Long userId = getCurrentUserId();
        Resource resource = borrowService.downloadFile(token, userId);
        return buildFileResponse(resource, true);
    }

    @GetMapping("/read")
    public ResponseEntity<Resource> readFile(@RequestParam String token) {
        Long userId = getCurrentUserId();
        Resource resource = borrowService.readFile(token, userId);
        return buildFileResponse(resource, false);
    }

    private ResponseEntity<Resource> buildFileResponse(Resource resource, boolean attachment) {
        String filename = resource.getFilename() == null ? "book.pdf" : resource.getFilename();
        String disposition = attachment ? "attachment" : "inline";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + filename + "\"")
                .body(resource);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return null;
        }
        return userDetails.getId();
    }
}
