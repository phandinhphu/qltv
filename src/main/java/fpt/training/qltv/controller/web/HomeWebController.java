package fpt.training.qltv.controller.web;

import fpt.training.qltv.dto.response.BookResponse;
import fpt.training.qltv.dto.response.CategoryResponse;
import fpt.training.qltv.dto.response.PageResponse;
import fpt.training.qltv.service.BookService;
import fpt.training.qltv.service.CategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class HomeWebController {

    private final BookService bookService;
    private final CategoryService categoryService;

    @GetMapping
    public String index(Model model) {
        PageResponse<BookResponse> pageData = bookService.findAll(null, 0, 8);
        List<CategoryResponse> categories = categoryService.findAll();

        model.addAttribute("books", pageData.getContent());
        model.addAttribute("categories", categories.size() > 5 ? categories.subList(0, 5) : categories);
        return "home/index";
    }
}
