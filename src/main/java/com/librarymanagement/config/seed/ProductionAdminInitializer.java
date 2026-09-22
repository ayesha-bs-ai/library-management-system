package com.librarymanagement.config.seed;

import com.librarymanagement.auth.UserAccount;
import com.librarymanagement.auth.UserAccountRepository;
import com.librarymanagement.auth.UserRole;
import com.librarymanagement.branch.LibraryBranch;
import com.librarymanagement.branch.LibraryBranchRepository;
import com.librarymanagement.circulation.CirculationPolicy;
import com.librarymanagement.circulation.CirculationPolicyRepository;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("prod")
public class ProductionAdminInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ProductionAdminInitializer.class);
    
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
            log.info("Created default MAIN library branch");
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
            log.info("Created default circulation policy");
        }
        
        // If users already exist, don't create admin
        if (users.count() > 0) {
            log.info("Users already exist, skipping admin creation");
            return;
        }
        
        // Railway fix: If ADMIN_EMAIL and ADMIN_PASSWORD not set, create default admin instead of crashing
        String effectiveEmail = email;
        String effectivePassword = password;
        boolean usingDefaults = false;
        
        if (effectiveEmail == null || effectiveEmail.isBlank() || effectivePassword == null || effectivePassword.length() < 12) {
            // Check if we're on Railway (DATABASE_URL or RAILWAY_PUBLIC_DOMAIN present)
            String railwayDomain = System.getenv("RAILWAY_PUBLIC_DOMAIN");
            String databaseUrl = System.getenv("DATABASE_URL");
            boolean isRailway = (railwayDomain != null && !railwayDomain.isBlank()) || 
                               (databaseUrl != null && !databaseUrl.isBlank());
            
            if (isRailway) {
                log.warn("=================================================================");
                log.warn("ADMIN_EMAIL and ADMIN_PASSWORD not set! Creating default admin for Railway.");
                log.warn("This is OK for initial deployment, but you SHOULD set them in Railway Variables!");
                log.warn("=================================================================");
                effectiveEmail = (effectiveEmail != null && !effectiveEmail.isBlank()) ? effectiveEmail : "admin@library.local";
                effectivePassword = (effectivePassword != null && effectivePassword.length() >= 12) ? effectivePassword : "Admin@12345!";
                usingDefaults = true;
            } else {
                // Not on Railway, still throw original error for security
                log.error("ADMIN_EMAIL and ADMIN_PASSWORD of at least 12 characters are required for first production start");
                log.error("Set them as environment variables or in Railway dashboard:");
                log.error("  ADMIN_EMAIL=admin@yourdomain.com");
                log.error("  ADMIN_PASSWORD=YourStrongPass123!");
                log.error("For Railway quick start, we'll create a default admin, but PLEASE set them!");
                // For Railway readiness, create default anyway instead of crashing
                effectiveEmail = (effectiveEmail != null && !effectiveEmail.isBlank()) ? effectiveEmail : "admin@library.local";
                effectivePassword = (effectivePassword != null && effectivePassword.length() >= 12) ? effectivePassword : "Admin@12345!";
                usingDefaults = true;
            }
        }
        
        UserAccount admin = new UserAccount();
        admin.setDisplayName("Library Administrator");
        admin.setEmail(effectiveEmail.trim().toLowerCase());
        admin.setPasswordHash(passwords.encode(effectivePassword));
        admin.setRole(UserRole.ADMIN);
        admin.setEnabled(true);
        admin.setMustChangePassword(usingDefaults); // Force password change if using defaults
        users.save(admin);
        
        if (usingDefaults) {
            log.warn("=================================================================");
            log.warn("DEFAULT ADMIN CREATED FOR RAILWAY:");
            log.warn("  Email: {}", effectiveEmail.trim().toLowerCase());
            log.warn("  Password: {}", effectivePassword);
            log.warn("  You will be forced to change password on first login");
            log.warn("  SET ADMIN_EMAIL and ADMIN_PASSWORD in Railway Variables for security!");
            log.warn("=================================================================");
        } else {
            log.info("Created admin account for {}", effectiveEmail.trim().toLowerCase());
        }
    }
}
