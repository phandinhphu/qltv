package fpt.training.qltv.controller.web.admin;

import fpt.training.qltv.dto.request.BorrowFilterRequest;
import fpt.training.qltv.dto.response.BorrowRecordResponse;
import fpt.training.qltv.dto.response.PageResponse;
import fpt.training.qltv.service.BorrowService;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/borrows")
@RequiredArgsConstructor
public class AdminBorrowWebController {

    private final BorrowService borrowService;

    @GetMapping
    public String index(
            @ModelAttribute BorrowFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        PageResponse<BorrowRecordResponse> pageData = borrowService.getAllBorrows(filter, page, 10);
        model.addAttribute("pageData", pageData);
        model.addAttribute("filter", filter);
        model.addAttribute("queryParams", buildQueryParams(filter));
        return "admin/borrow/list";
    }

    private Map<String, Object> buildQueryParams(BorrowFilterRequest filter) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (filter == null) {
            return params;
        }
        if (filter.getUserId() != null) {
            params.put("userId", filter.getUserId());
        }
        if (filter.getBookId() != null) {
            params.put("bookId", filter.getBookId());
        }
        if (filter.getStatus() != null) {
            params.put("status", filter.getStatus().name());
        }
        if (filter.getFromDate() != null) {
            params.put("fromDate", filter.getFromDate());
        }
        if (filter.getToDate() != null) {
            params.put("toDate", filter.getToDate());
        }
        return params;
    }
}
