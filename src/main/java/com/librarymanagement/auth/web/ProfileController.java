package com.librarymanagement.auth.web;

import com.librarymanagement.auth.CurrentUserService;
import com.librarymanagement.auth.UserAccount;
import com.librarymanagement.auth.UserAccountService;
import com.librarymanagement.common.exception.BusinessRuleException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account")
public class ProfileController {
    private final CurrentUserService currentUsers;
    private final UserAccountService users;

    public ProfileController(CurrentUserService currentUsers, UserAccountService users) {
        this.currentUsers = currentUsers;
        this.users = users;
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        UserAccount user = currentUsers.requireCurrentUser();
        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", ProfileForm.from(user));
        }
        if (!model.containsAttribute("passwordChangeForm")) {
            model.addAttribute("passwordChangeForm", new PasswordChangeForm());
        }
        model.addAttribute("currentAccount", user);
        model.addAttribute("title", "Edit profile");
        return "auth/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profileForm") ProfileForm profileForm,
                                BindingResult binding,
                                Model model,
                                HttpServletRequest request,
                                RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("currentAccount", currentUsers.requireCurrentUser());
            model.addAttribute("passwordChangeForm", new PasswordChangeForm());
            model.addAttribute("title", "Edit profile");
            return "auth/profile";
        }
        try {
            UserAccount current = currentUsers.requireCurrentUser();
            boolean emailChanged = users.updateProfile(current, profileForm.getDisplayName(), profileForm.getEmail());

            if (emailChanged) {
                // Email changed - force re-login for security
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                redirect.addFlashAttribute("success", "Email updated to " + profileForm.getEmail().trim().toLowerCase() + ". Please sign in again with your new email.");
                return "redirect:/login?profileUpdated";
            }

            redirect.addFlashAttribute("success", "Profile updated successfully.");
            return "redirect:/account/profile";
        } catch (BusinessRuleException ex) {
            binding.reject("profile", ex.getMessage());
            model.addAttribute("currentAccount", currentUsers.requireCurrentUser());
            model.addAttribute("passwordChangeForm", new PasswordChangeForm());
            model.addAttribute("title", "Edit profile");
            return "auth/profile";
        }
    }

    @PostMapping("/profile/password")
    public String changePassword(@Valid @ModelAttribute("passwordChangeForm") PasswordChangeForm passwordChangeForm,
                                 BindingResult binding,
                                 Model model,
                                 RedirectAttributes redirect) {
        // Keep profile form in model for re-render
        UserAccount user = currentUsers.requireCurrentUser();
        model.addAttribute("profileForm", ProfileForm.from(user));
        model.addAttribute("currentAccount", user);
        model.addAttribute("title", "Edit profile");

        if (binding.hasErrors()) {
            return "auth/profile";
        }
        try {
            users.changePassword(user, passwordChangeForm.getCurrentPassword(),
                    passwordChangeForm.getNewPassword(), passwordChangeForm.getConfirmPassword());
            redirect.addFlashAttribute("success", "Your password has been changed.");
            return "redirect:/account/profile";
        } catch (BusinessRuleException ex) {
            binding.reject("password", ex.getMessage());
            return "auth/profile";
        }
    }
}
