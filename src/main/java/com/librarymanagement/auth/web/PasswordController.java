package com.librarymanagement.auth.web;

import com.librarymanagement.auth.CurrentUserService;
import com.librarymanagement.auth.UserAccountService;
import com.librarymanagement.common.exception.BusinessRuleException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PasswordController {
    private final CurrentUserService currentUsers;
    private final UserAccountService users;

    public PasswordController(CurrentUserService currentUsers, UserAccountService users) {
        this.currentUsers = currentUsers;
        this.users = users;
    }

    @GetMapping("/account/password")
    public String form(Model model) {
        model.addAttribute("passwordChangeForm", new PasswordChangeForm());
        model.addAttribute("title", "Change password");
        return "auth/change-password";
    }

    @PostMapping("/account/password")
    public String change(@Valid @ModelAttribute PasswordChangeForm passwordChangeForm,
                         BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) return "auth/change-password";
        try {
            users.changePassword(currentUsers.requireCurrentUser(), passwordChangeForm.getCurrentPassword(),
                    passwordChangeForm.getNewPassword(), passwordChangeForm.getConfirmPassword());
            redirect.addFlashAttribute("success", "Your password has been changed.");
            return "redirect:/account/profile";
        } catch (BusinessRuleException ex) {
            binding.reject("password", ex.getMessage());
            model.addAttribute("title", "Change password");
            return "auth/change-password";
        }
    }
}
