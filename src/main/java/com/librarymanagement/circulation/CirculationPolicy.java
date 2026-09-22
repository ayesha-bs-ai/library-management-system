package com.librarymanagement.circulation;

import com.librarymanagement.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "circulation_policy")
public class CirculationPolicy extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "default_policy", nullable = false)
    private boolean defaultPolicy = true;

    @Column(name = "loan_period_days", nullable = false)
    private int loanPeriodDays = 14;

    @Column(name = "max_loans", nullable = false)
    private int maxLoans = 5;

    @Column(name = "max_renewals", nullable = false)
    private int maxRenewals = 2;

    @Column(name = "grace_period_days", nullable = false)
    private int gracePeriodDays = 0;

    @Column(name = "fine_per_day", nullable = false, precision = 12, scale = 2)
    private BigDecimal finePerDay = new BigDecimal("10.00");

    @Column(name = "max_fine_per_loan", nullable = false, precision = 12, scale = 2)
    private BigDecimal maxFinePerLoan = new BigDecimal("500.00");

    @Column(name = "block_at_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal blockAtBalance = new BigDecimal("1000.00");

    @Column(name = "pickup_days", nullable = false)
    private int pickupDays = 3;
}
