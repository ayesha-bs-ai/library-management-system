package com.librarymanagement.reporting;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff/dashboard")
public class StaffDashboardController {
    private final DashboardService dashboard;

    public StaffDashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("snapshot", dashboard.snapshot());
        model.addAttribute("title", "Operations overview");
        return "staff/dashboard";
    }
}
