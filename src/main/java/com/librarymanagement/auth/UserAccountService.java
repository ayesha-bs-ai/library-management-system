package com.librarymanagement.auth;

import com.librarymanagement.audit.AuditService;
import com.librarymanagement.auth.web.UserForm;
import com.librarymanagement.common.exception.BusinessRuleException;
import com.librarymanagement.common.exception.ResourceNotFoundException;
import com.librarymanagement.member.MemberRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {
    private final UserAccountRepository users;
    private final MemberRepository members;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public UserAccountService(UserAccountRepository users, MemberRepository members, PasswordEncoder passwordEncoder, AuditService audit) {
        this.users = users;
        this.members = members;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<UserAccount> list() {
        return users.findAllByOrderByDisplayNameAsc();
    }

    @Transactional
    public UserAccount create(UserForm form) {
        String email = form.getEmail().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("An account with this email already exists.");
        }
        if (form.getRole() == UserRole.MEMBER) {
            throw new BusinessRuleException("Member accounts are created from the member screen.");
        }
        UserAccount user = new UserAccount();
        user.setDisplayName(form.getDisplayName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(form.getTemporaryPassword()));
        user.setRole(form.getRole());
        user.setEnabled(true);
        user.setMustChangePassword(true);
        users.save(user);
        audit.record("USER_CREATED", "UserAccount", user.getId().toString(), "Created " + user.getRole() + " account for " + email);
        return user;
    }

    @Transactional
    public void changePassword(UserAccount user, String currentPassword, String newPassword, String confirmation) {
        UserAccount managed = users.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(currentPassword, managed.getPasswordHash())) {
            throw new BusinessRuleException("The current password is incorrect.");
        }
        if (newPassword == null || newPassword.length() < 12) {
            throw new BusinessRuleException("The new password must contain at least 12 characters.");
        }
        if (!newPassword.equals(confirmation)) {
            throw new BusinessRuleException("The new password and confirmation do not match.");
        }
        if (passwordEncoder.matches(newPassword, managed.getPasswordHash())) {
            throw new BusinessRuleException("Choose a password different from the current password.");
        }
        managed.setPasswordHash(passwordEncoder.encode(newPassword));
        managed.setMustChangePassword(false);
        audit.record("PASSWORD_CHANGED", "UserAccount", managed.getId().toString(), managed.getEmail());
    }

    @Transactional
    public boolean updateProfile(UserAccount currentUser, String newDisplayName, String newEmail) {
        UserAccount managed = users.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String trimmedName = newDisplayName == null ? "" : newDisplayName.trim();
        String trimmedEmail = newEmail == null ? "" : newEmail.trim().toLowerCase();

        if (trimmedName.isBlank()) {
            throw new BusinessRuleException("Display name cannot be empty.");
        }
        if (trimmedName.length() > 120) {
            throw new BusinessRuleException("Display name is too long (max 120 characters).");
        }
        if (trimmedEmail.isBlank()) {
            throw new BusinessRuleException("Email cannot be empty.");
        }
        if (!trimmedEmail.matches("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")) {
            throw new BusinessRuleException("Please provide a valid email address.");
        }

        boolean emailChanged = !managed.getEmail().equalsIgnoreCase(trimmedEmail);
        boolean nameChanged = !managed.getDisplayName().equals(trimmedName);

        if (!emailChanged && !nameChanged) {
            return false;
        }

        if (emailChanged) {
            Optional<UserAccount> existing = users.findByEmailIgnoreCase(trimmedEmail);
            if (existing.isPresent() && !existing.get().getId().equals(managed.getId())) {
                throw new BusinessRuleException("An account with this email already exists.");
            }
            String oldEmail = managed.getEmail();
            managed.setEmail(trimmedEmail);
            // Sync member email if linked via userAccountId or old email
            members.findByUserAccountId(managed.getId()).ifPresent(member -> {
                member.setEmail(trimmedEmail);
                if (nameChanged) {
                    member.setFullName(trimmedName);
                }
            });
            members.findByUserAccountEmailIgnoreCase(oldEmail).ifPresent(member -> {
                // In case member was found by old email but not by userAccountId (legacy)
                if (member.getUserAccount() == null || !member.getUserAccount().getId().equals(managed.getId())) {
                    member.setEmail(trimmedEmail);
                    if (nameChanged) member.setFullName(trimmedName);
                }
            });
            audit.record("PROFILE_EMAIL_CHANGED", "UserAccount", managed.getId().toString(), oldEmail + " -> " + trimmedEmail);
        }

        if (nameChanged) {
            managed.setDisplayName(trimmedName);
            if (!emailChanged) {
                members.findByUserAccountId(managed.getId()).ifPresent(member -> member.setFullName(trimmedName));
            }
            audit.record("PROFILE_NAME_CHANGED", "UserAccount", managed.getId().toString(), trimmedName);
        }

        return emailChanged;
    }

    @Transactional
    public void toggleEnabled(Long id, UserAccount actor) {
        UserAccount user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getId().equals(actor.getId())) {
            throw new BusinessRuleException("You cannot disable your own account.");
        }
        user.setEnabled(!user.isEnabled());
        audit.record(user.isEnabled() ? "USER_ENABLED" : "USER_DISABLED", "UserAccount", id.toString(), user.getEmail());
    }
}
