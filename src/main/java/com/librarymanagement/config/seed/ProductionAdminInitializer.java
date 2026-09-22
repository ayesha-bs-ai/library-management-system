package com.librarymanagement.config.seed;

import com.librarymanagement.auth.UserAccount;
import com.librarymanagement.auth.UserAccountRepository;
import com.librarymanagement.auth.UserRole;
import com.librarymanagement.branch.LibraryBranch;
import com.librarymanagement.branch.LibraryBranchRepository;
import com.librarymanagement.circulation.CirculationPolicy;
import com.librarymanagement.circulation.CirculationPolicyRepository;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("prod")
public class ProductionAdminInitializer implements CommandLineRunner {
    private final UserAccountRepository users;
    private final LibraryBranchRepository branches;
    private final CirculationPolicyRepository policies;
    private final PasswordEncoder passwords;
    private final String email;
    private final String password;

    public ProductionAdminInitializer(UserAccountRepository users, LibraryBranchRepository branches,
                                      CirculationPolicyRepository policies, PasswordEncoder passwords,
                                      @Value("${ADMIN_EMAIL:}") String email,
                                      @Value("${ADMIN_PASSWORD:}") String password) {
        this.users = users;
        this.branches = branches;
        this.policies = policies;
        this.passwords = passwords;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (branches.count() == 0) {
            LibraryBranch branch = new LibraryBranch();
            branch.setCode("MAIN");
            branch.setName("Main Library");
            branch.setActive(true);
            branches.save(branch);
        }
        if (policies.count() == 0) {
            CirculationPolicy policy = new CirculationPolicy();
            policy.setName("Standard borrowing policy");
            policy.setDefaultPolicy(true);
            policy.setLoanPeriodDays(14);
            policy.setMaxLoans(5);
            policy.setMaxRenewals(2);
            policy.setGracePeriodDays(1);
            policy.setFinePerDay(new BigDecimal("10.00"));
            policy.setMaxFinePerLoan(new BigDecimal("500.00"));
            policy.setBlockAtBalance(new BigDecimal("1000.00"));
            policy.setPickupDays(3);
            policies.save(policy);
        }
        if (users.count() > 0) return;
        if (email.isBlank() || password.length() < 12) {
            throw new IllegalStateException("ADMIN_EMAIL and an ADMIN_PASSWORD of at least 12 characters are required for the first production start.");
        }
        UserAccount admin = new UserAccount();
        admin.setDisplayName("Library Administrator");
        admin.setEmail(email.trim().toLowerCase());
        admin.setPasswordHash(passwords.encode(password));
        admin.setRole(UserRole.ADMIN);
        admin.setEnabled(true);
        admin.setMustChangePassword(true);
        users.save(admin);
    }
}
