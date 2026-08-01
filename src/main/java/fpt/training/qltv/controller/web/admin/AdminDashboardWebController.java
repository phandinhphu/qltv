package fpt.training.qltv.controller.web.admin;

import fpt.training.qltv.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardWebController {

    private final DashboardService dashboardService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("dashboard", dashboardService.getDashboard());
        return "admin/dashboard";
    }
}
