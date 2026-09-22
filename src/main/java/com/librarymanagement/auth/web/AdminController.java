package com.librarymanagement.auth.web;

import com.librarymanagement.audit.AuditService;
import com.librarymanagement.auth.CurrentUserService;
import com.librarymanagement.auth.UserAccountService;
import com.librarymanagement.auth.UserRole;
import com.librarymanagement.circulation.CirculationService;
import com.librarymanagement.circulation.web.PolicyForm;
import com.librarymanagement.common.exception.BusinessRuleException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final UserAccountService users;
    private final CurrentUserService currentUsers;
    private final CirculationService circulation;
    private final AuditService audit;

    public AdminController(UserAccountService users, CurrentUserService currentUsers,
                           CirculationService circulation, AuditService audit) {
        this.users = users;
        this.currentUsers = currentUsers;
        this.circulation = circulation;
        this.audit = audit;
    }

    @GetMapping("/users")
    public String users(Model model) {
        if (!model.containsAttribute("userForm")) model.addAttribute("userForm", new UserForm());
        model.addAttribute("users", users.list());
        model.addAttribute("roles", new UserRole[]{UserRole.ADMIN, UserRole.LIBRARIAN});
        model.addAttribute("title", "Staff accounts");
        return "admin/users";
    }

    @PostMapping("/users")
    public String createUser(@Valid @ModelAttribute UserForm userForm, BindingResult binding,
                             Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("users", users.list());
            model.addAttribute("roles", new UserRole[]{UserRole.ADMIN, UserRole.LIBRARIAN});
            model.addAttribute("title", "Staff accounts");
            return "admin/users";
        }
        try {
            users.create(userForm);
            redirect.addFlashAttribute("success", "Staff account created.");
            return "redirect:/admin/users";
        } catch (BusinessRuleException ex) {
            binding.reject("user", ex.getMessage());
            model.addAttribute("users", users.list());
            model.addAttribute("roles", new UserRole[]{UserRole.ADMIN, UserRole.LIBRARIAN});
            model.addAttribute("title", "Staff accounts");
            return "admin/users";
        }
    }

    @PostMapping("/users/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            users.toggleEnabled(id, currentUsers.requireCurrentUser());
            redirect.addFlashAttribute("success", "Account status updated.");
        } catch (BusinessRuleException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        if (!model.containsAttribute("policyForm")) {
            model.addAttribute("policyForm", PolicyForm.from(circulation.defaultPolicy()));
        }
        model.addAttribute("title", "Circulation policy");
        return "admin/settings";
    }

    @PostMapping("/settings")
    public String updateSettings(@Valid @ModelAttribute PolicyForm policyForm, BindingResult binding,
                                 RedirectAttributes redirect) {
        if (binding.hasErrors()) return "admin/settings";
        circulation.updatePolicy(policyForm);
        redirect.addFlashAttribute("success", "Circulation policy updated.");
        return "redirect:/admin/settings";
    }

    @GetMapping("/audit")
    public String audit(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("events", audit.recent(page));
        model.addAttribute("title", "Audit log");
        return "admin/audit";
    }
}
