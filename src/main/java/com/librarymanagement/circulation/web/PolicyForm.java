package com.librarymanagement.circulation.web;

import com.librarymanagement.circulation.CirculationPolicy;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PolicyForm {
    @NotBlank
    private String name;
    @Min(1) @Max(365)
    private int loanPeriodDays;
    @Min(1) @Max(100)
    private int maxLoans;
    @Min(0) @Max(20)
    private int maxRenewals;
    @Min(0) @Max(30)
    private int gracePeriodDays;
    @NotNull @DecimalMin("0.00")
    private BigDecimal finePerDay;
    @NotNull @DecimalMin("0.00")
    private BigDecimal maxFinePerLoan;
    @NotNull @DecimalMin("0.00")
    private BigDecimal blockAtBalance;
    @Min(1) @Max(30)
    private int pickupDays;

    public static PolicyForm from(CirculationPolicy policy) {
        PolicyForm form = new PolicyForm();
        form.setName(policy.getName());
        form.setLoanPeriodDays(policy.getLoanPeriodDays());
        form.setMaxLoans(policy.getMaxLoans());
        form.setMaxRenewals(policy.getMaxRenewals());
        form.setGracePeriodDays(policy.getGracePeriodDays());
        form.setFinePerDay(policy.getFinePerDay());
        form.setMaxFinePerLoan(policy.getMaxFinePerLoan());
        form.setBlockAtBalance(policy.getBlockAtBalance());
        form.setPickupDays(policy.getPickupDays());
        return form;
    }
}
