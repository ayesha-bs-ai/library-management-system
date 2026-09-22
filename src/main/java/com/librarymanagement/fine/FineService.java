package com.librarymanagement.fine;

import com.librarymanagement.audit.AuditService;
import com.librarymanagement.auth.UserAccount;
import com.librarymanagement.circulation.Loan;
import com.librarymanagement.common.exception.BusinessRuleException;
import com.librarymanagement.member.Member;
import com.librarymanagement.member.MemberService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FineService {
    private final FineLedgerEntryRepository entries;
    private final MemberService members;
    private final AuditService audit;

    public FineService(FineLedgerEntryRepository entries, MemberService members, AuditService audit) {
        this.entries = entries;
        this.members = members;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public BigDecimal balance(Long memberId) {
        BigDecimal balance = entries.balanceForMember(memberId);
        return balance == null ? BigDecimal.ZERO : balance.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public List<FineLedgerEntry> ledger(Long memberId) {
        return entries.findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    @Transactional(readOnly = true)
    public List<FineLedgerEntry> recent() {
        return entries.findTop50ByOrderByCreatedAtDesc();
    }

    @Transactional
    public BigDecimal assessOverdue(Loan loan, LocalDate returnedOn, UserAccount actor) {
        if (entries.existsByLoanIdAndType(loan.getId(), LedgerEntryType.OVERDUE_CHARGE)) return BigDecimal.ZERO;
        LocalDate chargeStarts = loan.getDueOn().plusDays(loan.getPolicyGraceDays());
        long overdueDays = Math.max(0, ChronoUnit.DAYS.between(chargeStarts, returnedOn));
        if (overdueDays == 0) return BigDecimal.ZERO;
        BigDecimal amount = loan.getPolicyFinePerDay().multiply(BigDecimal.valueOf(overdueDays));
        amount = amount.min(loan.getPolicyMaxFine()).setScale(2, RoundingMode.HALF_UP);

        FineLedgerEntry entry = new FineLedgerEntry();
        entry.setMember(loan.getMember());
        entry.setLoan(loan);
        entry.setType(LedgerEntryType.OVERDUE_CHARGE);
        entry.setAmount(amount);
        entry.setDescription("Overdue charge for " + overdueDays + " day" + (overdueDays == 1 ? "" : "s") + " — " + loan.getCopy().getBook().getTitle());
        entry.setCreatedBy(actor);
        entries.save(entry);
        audit.record("FINE_CHARGED", "FineLedgerEntry", entry.getId().toString(), amount + " for loan " + loan.getId());
        return amount;
    }

    @Transactional
    public FineLedgerEntry recordPayment(Long memberId, BigDecimal amount, String description, UserAccount actor) {
        if (amount == null || amount.signum() <= 0) throw new BusinessRuleException("Payment must be greater than zero.");
        BigDecimal current = balance(memberId);
        if (current.signum() <= 0) throw new BusinessRuleException("This member has no outstanding balance.");
        if (amount.compareTo(current) > 0) throw new BusinessRuleException("Payment cannot exceed the outstanding balance of " + current + ".");
        Member member = members.get(memberId);
        FineLedgerEntry entry = new FineLedgerEntry();
        entry.setMember(member);
        entry.setType(LedgerEntryType.PAYMENT);
        entry.setAmount(amount.negate().setScale(2, RoundingMode.HALF_UP));
        entry.setDescription(description.trim());
        entry.setCreatedBy(actor);
        entries.save(entry);
        audit.record("PAYMENT_RECORDED", "FineLedgerEntry", entry.getId().toString(), amount + " from " + member.getMembershipNumber());
        return entry;
    }

    @Transactional
    public FineLedgerEntry recordWaiver(Long memberId, BigDecimal amount, String description, UserAccount actor) {
        if (amount == null || amount.signum() <= 0) throw new BusinessRuleException("Waiver must be greater than zero.");
        BigDecimal current = balance(memberId);
        if (amount.compareTo(current) > 0) throw new BusinessRuleException("Waiver cannot exceed the outstanding balance.");
        Member member = members.get(memberId);
        FineLedgerEntry entry = new FineLedgerEntry();
        entry.setMember(member);
        entry.setType(LedgerEntryType.WAIVER);
        entry.setAmount(amount.negate().setScale(2, RoundingMode.HALF_UP));
        entry.setDescription(description.trim());
        entry.setCreatedBy(actor);
        entries.save(entry);
        audit.record("FINE_WAIVED", "FineLedgerEntry", entry.getId().toString(), amount + " for " + member.getMembershipNumber());
        return entry;
    }
}
