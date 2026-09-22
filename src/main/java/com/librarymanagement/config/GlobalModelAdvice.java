package com.librarymanagement.config;

import com.librarymanagement.auth.CurrentUserService;
import com.librarymanagement.auth.UserAccount;
import com.librarymanagement.notification.NotificationService;
import java.time.Year;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {
    private final AppProperties properties;
    private final CurrentUserService currentUsers;
    private final NotificationService notifications;

    public GlobalModelAdvice(AppProperties properties, CurrentUserService currentUsers, NotificationService notifications) {
        this.properties = properties;
        this.currentUsers = currentUsers;
        this.notifications = notifications;
    }

    @ModelAttribute
    void addGlobals(Model model, Authentication authentication) {
        model.addAttribute("libraryName", properties.getLibraryName());
        model.addAttribute("currency", properties.getCurrency());
        model.addAttribute("demoMode", properties.isDemoMode());
        model.addAttribute("currentYear", Year.now().getValue());
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            UserAccount user = currentUsers.requireCurrentUser();
            model.addAttribute("currentUser", user);
            model.addAttribute("unreadNotificationCount", notifications.unreadCount(user.getId()));
        }
    }
}
