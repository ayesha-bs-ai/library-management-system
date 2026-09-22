package com.librarymanagement.config;

import com.librarymanagement.auth.CurrentUserService;
import com.librarymanagement.auth.UserAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PasswordChangeInterceptor implements HandlerInterceptor {
    private final CurrentUserService currentUsers;

    public PasswordChangeInterceptor(CurrentUserService currentUsers) {
        this.currentUsers = currentUsers;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) return true;
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.equals("/account/password") || path.equals("/logout") || path.startsWith("/css/")
                || path.startsWith("/js/") || path.startsWith("/error")) return true;
        UserAccount user = currentUsers.requireCurrentUser();
        if (user.isMustChangePassword()) {
            response.sendRedirect(request.getContextPath() + "/account/password");
            return false;
        }
        return true;
    }
}
