package com.librarymanagement.member;

import com.librarymanagement.audit.AuditService;
import com.librarymanagement.auth.UserAccount;
import com.librarymanagement.auth.UserAccountRepository;
import com.librarymanagement.auth.UserRole;
import com.librarymanagement.common.exception.BusinessRuleException;
import com.librarymanagement.common.exception.ResourceNotFoundException;
import com.librarymanagement.member.web.MemberForm;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {
    private final MemberRepository members;
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public MemberService(MemberRepository members, UserAccountRepository users, PasswordEncoder passwordEncoder, AuditService audit) {
        this.members = members;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Page<Member> search(String query, int page) {
        return members.search(query == null ? "" : query.trim(), PageRequest.of(Math.max(page, 0), 20));
    }

    @Transactional(readOnly = true)
    public Member get(Long id) {
        return members.findById(id).orElseThrow(() -> new ResourceNotFoundException("Member not found"));
    }

    @Transactional(readOnly = true)
    public Member getByNumber(String number) {
        return members.findByMembershipNumberIgnoreCase(number.trim())
                .orElseThrow(() -> new ResourceNotFoundException("No member matches that membership number."));
    }

    @Transactional(readOnly = true)
    public Member getForUser(String email) {
        return members.findByUserAccountEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No member profile is linked to this account."));
    }

    @Transactional(readOnly = true)
    public List<Member> recent() {
        return members.findTop5ByOrderByCreatedAtDesc();
    }

    @Transactional
    public Member create(MemberForm form) {
        if (members.existsByMembershipNumberIgnoreCase(form.getMembershipNumber().trim())) {
            throw new BusinessRuleException("That membership number is already in use.");
        }
        validateDates(form);
        Member member = new Member();
        apply(member, form);
        if (form.isCreatePortalAccount()) member.setUserAccount(createPortalAccount(form));
        members.save(member);
        audit.record("MEMBER_CREATED", "Member", member.getId().toString(), member.getMembershipNumber() + " — " + member.getFullName());
        return member;
    }

    @Transactional
    public Member update(Long id, MemberForm form) {
        Member member = get(id);
        if (members.existsByMembershipNumberIgnoreCaseAndIdNot(form.getMembershipNumber().trim(), id)) {
            throw new BusinessRuleException("That membership number is already in use.");
        }
        validateDates(form);
        apply(member, form);
        if (member.getUserAccount() == null && form.isCreatePortalAccount()) {
            member.setUserAccount(createPortalAccount(form));
        } else if (member.getUserAccount() != null) {
            UserAccount account = member.getUserAccount();
            users.findByEmailIgnoreCase(form.getEmail().trim()).ifPresent(existing -> {
                if (!existing.getId().equals(account.getId())) throw new BusinessRuleException("That email belongs to another account.");
            });
            account.setDisplayName(form.getFullName().trim());
            account.setEmail(form.getEmail().trim().toLowerCase());
            account.setEnabled(form.getStatus() == MemberStatus.ACTIVE);
            if (form.getTemporaryPassword() != null && !form.getTemporaryPassword().isBlank()) {
                validatePassword(form.getTemporaryPassword());
                account.setPasswordHash(passwordEncoder.encode(form.getTemporaryPassword()));
                account.setMustChangePassword(true);
            }
        }
        audit.record("MEMBER_UPDATED", "Member", id.toString(), member.getMembershipNumber() + " — " + member.getFullName());
        return member;
    }

    private void apply(Member member, MemberForm form) {
        member.setMembershipNumber(form.getMembershipNumber().trim().toUpperCase());
        member.setFullName(form.getFullName().trim());
        member.setEmail(form.getEmail().trim().toLowerCase());
        member.setPhone(trimToNull(form.getPhone()));
        member.setAddress(trimToNull(form.getAddress()));
        member.setType(form.getType());
        member.setStatus(form.getStatus());
        member.setJoinedOn(form.getJoinedOn());
        member.setExpiresOn(form.getExpiresOn());
    }

    private UserAccount createPortalAccount(MemberForm form) {
        validatePassword(form.getTemporaryPassword());
        String email = form.getEmail().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw new BusinessRuleException("An account with this email already exists.");
        UserAccount account = new UserAccount();
        account.setDisplayName(form.getFullName().trim());
        account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode(form.getTemporaryPassword()));
        account.setRole(UserRole.MEMBER);
        account.setEnabled(form.getStatus() == MemberStatus.ACTIVE);
        account.setMustChangePassword(true);
        return users.save(account);
    }

    private void validateDates(MemberForm form) {
        if (form.getExpiresOn().isBefore(form.getJoinedOn())) {
            throw new BusinessRuleException("Membership expiry must be after the join date.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 10) {
            throw new BusinessRuleException("A portal account needs a temporary password of at least 10 characters.");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
