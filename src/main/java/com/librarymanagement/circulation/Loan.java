package com.librarymanagement.circulation;

import com.librarymanagement.auth.UserAccount;
import com.librarymanagement.common.domain.BaseEntity;
import com.librarymanagement.inventory.BookCopy;
import com.librarymanagement.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "loan")
public class Loan extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "copy_id", nullable = false)
    private BookCopy copy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "checked_out_at", nullable = false)
    private Instant checkedOutAt;

    @Column(name = "due_on", nullable = false)
    private LocalDate dueOn;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status = LoanStatus.ACTIVE;

    @Column(name = "renewal_count", nullable = false)
    private int renewalCount = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checked_out_by", nullable = false)
    private UserAccount checkedOutBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "returned_by")
    private UserAccount returnedBy;

    @Column(name = "policy_loan_days", nullable = false)
    private int policyLoanDays;

    @Column(name = "policy_grace_days", nullable = false)
    private int policyGraceDays;

    @Column(name = "policy_fine_per_day", nullable = false, precision = 12, scale = 2)
    private BigDecimal policyFinePerDay;

    @Column(name = "policy_max_fine", nullable = false, precision = 12, scale = 2)
    private BigDecimal policyMaxFine;

    public boolean isOverdue(LocalDate today) {
        return status == LoanStatus.ACTIVE && dueOn.isBefore(today);
    }
}
