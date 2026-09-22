package com.librarymanagement.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.librarymanagement.auth.UserAccount;
import com.librarymanagement.auth.UserAccountRepository;
import com.librarymanagement.auth.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WebSmokeTest {
    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired PasswordEncoder passwords;

    @BeforeEach
    void createAdmin() {
        UserAccount admin = new UserAccount();
        admin.setDisplayName("Web Test Admin");
        admin.setEmail("web-admin@example.com");
        admin.setPasswordHash(passwords.encode("StrongPassword123"));
        admin.setRole(UserRole.ADMIN);
        admin.setEnabled(true);
        users.save(admin);
    }

    @Test
    void publicHomeAndLoginRender() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Find a book")));
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sign in to your library")));
    }

    @Test
    void staffAreaRequiresAuthenticationAndAdminCanLogin() throws Exception {
        mvc.perform(get("/staff/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mvc.perform(formLogin().user("email", "web-admin@example.com").password("StrongPassword123"))
                .andExpect(authenticated().withUsername("web-admin@example.com"));
    }
}
