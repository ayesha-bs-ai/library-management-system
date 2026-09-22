package com.librarymanagement.circulation;

import com.librarymanagement.audit.AuditService;
import com.librarymanagement.auth.UserAccount;
import com.librarymanagement.circulation.web.PolicyForm;
import com.librarymanagement.common.exception.BusinessRuleException;
import com.librarymanagement.common.exception.ResourceNotFoundException;
import com.librarymanagement.fine.FineService;
import com.librarymanagement.inventory.BookCopy;
import com.librarymanagement.inventory.BookCopyRepository;
import com.librarymanagement.inventory.CopyCondition;
import com.librarymanagement.inventory.CopyStatus;
import com.librarymanagement.member.Member;
import com.librarymanagement.member.MemberService;
import com.librarymanagement.member.MemberStatus;
import com.librarymanagement.notification.NotificationService;
import com.librarymanagement.notification.NotificationType;
import com.librarymanagement.reservation.Reservation;
import com.librarymanagement.reservation.ReservationRepository;
import com.librarymanagement.reservation.ReservationService;
import com.librarymanagement.reservation.ReservationStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CirculationService {
    private final LoanRepository loans;
    private final BookCopyRepository copies;
    private final CirculationPolicyRepository policies;
    private final MemberService members;
    private final FineService fines;
    private final ReservationService reservationService;
    private final ReservationRepository reservations;
    private final NotificationService notifications;
    private final AuditService audit;
    private final Clock clock;

    public CirculationService(LoanRepository loans, BookCopyRepository copies, CirculationPolicyRepository policies,
                              MemberService members, FineService fines, ReservationService reservationService,
                              ReservationRepository reservations, NotificationService notifications,
                              AuditService audit, Clock clock) {
        this.loans = loans;
        this.copies = copies;
        this.policies = policies;
        this.members = members;
        this.fines = fines;
        this.reservationService = reservationService;
        this.reservations = reservations;
        this.notifications = notifications;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public Loan checkout(String memberNumber, String barcode, UserAccount actor) {
        Member member = members.getByNumber(memberNumber);
        validateMember(member);
        CirculationPolicy policy = defaultPolicy();

        if (loans.countByMemberIdAndStatus(member.getId(), LoanStatus.ACTIVE) >= policy.getMaxLoans()) {
            throw new BusinessRuleException("This member has reached the maximum of " + policy.getMaxLoans() + " active loans.");
        }
        BigDecimal balance = fines.balance(member.getId());
        if (balance.compareTo(policy.getBlockAtBalance()) >= 0) {
            throw new BusinessRuleException("Borrowing is blocked because the member's outstanding balance is " + balance + ".");
        }

        BookCopy copy = copies.findByBarcodeForUpdate(barcode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("No copy matches that barcode."));
        Optional<Reservation> ready = reservationService.readyFor(copy.getBook().getId(), member.getId());
        if (copy.getStatus() == CopyStatus.RESERVED) {
            if (ready.isEmpty() || ready.get().getAssignedCopy() == null || !ready.get().getAssignedCopy().getId().equals(copy.getId())) {
                throw new BusinessRuleException("This copy is reserved for another member.");
            }
        } else if (copy.getStatus() != CopyStatus.AVAILABLE) {
            throw new BusinessRuleException("This copy cannot be issued because its status is " + copy.getStatus() + ".");
        }

        LocalDate today = LocalDate.now(clock);
        Loan loan = new Loan();
        loan.setCopy(copy);
        loan.setMember(member);
        loan.setCheckedOutAt(Instant.now(clock));
        loan.setDueOn(today.plusDays(policy.getLoanPeriodDays()));
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setCheckedOutBy(actor);
        loan.setPolicyLoanDays(policy.getLoanPeriodDays());
        loan.setPolicyGraceDays(policy.getGracePeriodDays());
        loan.setPolicyFinePerDay(policy.getFinePerDay());
        loan.setPolicyMaxFine(policy.getMaxFinePerLoan());
        loans.save(loan);
        copy.setStatus(CopyStatus.ON_LOAN);
        reservationService.fulfillForCheckout(copy, member);

        notifications.notify(member.getUserAccount(), "Book checked out",
                copy.getBook().getTitle() + " is due on " + loan.getDueOn() + ".",
                NotificationType.CHECKOUT, "checkout:" + loan.getId());
        audit.record("BOOK_CHECKED_OUT", "Loan", loan.getId().toString(), copy.getBarcode() + " to " + member.getMembershipNumber());
        return loan;
    }

    @Transactional
    public ReturnResult returnBook(String barcode, CopyCondition condition, UserAccount actor) {
        Loan loan = loans.findActiveByBarcodeForUpdate(barcode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("No active loan matches that barcode."));
        Instant now = Instant.now(clock);
        loan.setReturnedAt(now);
        loan.setReturnedBy(actor);
        loan.setStatus(LoanStatus.RETURNED);

        BookCopy copy = loan.getCopy();
        copy.setCondition(condition);
        if (condition == CopyCondition.DAMAGED) {
            copy.setStatus(CopyStatus.DAMAGED);
        } else {
            reservationService.processReturnedCopy(copy);
        }

        BigDecimal fine = fines.assessOverdue(loan, LocalDate.now(clock), actor);
        notifications.notify(loan.getMember().getUserAccount(), "Book returned",
                copy.getBook().getTitle() + " was returned." + (fine.signum() > 0 ? " An overdue charge of " + fine + " was added." : ""),
                NotificationType.INFO, "return:" + loan.getId());
        audit.record("BOOK_RETURNED", "Loan", loan.getId().toString(), copy.getBarcode() + "; fine=" + fine);
        return new ReturnResult(loan, fine);
    }

    @Transactional
    public Loan renew(Long loanId, UserAccount actor, Long requiredMemberId) {
        Loan loan = loans.findById(loanId).orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
        if (requiredMemberId != null && !loan.getMember().getId().equals(requiredMemberId)) {
            throw new BusinessRuleException("You can renew only your own loans.");
        }
        if (loan.getStatus() != LoanStatus.ACTIVE) throw new BusinessRuleException("Only an active loan can be renewed.");
        CirculationPolicy policy = defaultPolicy();
        if (loan.getRenewalCount() >= policy.getMaxRenewals()) {
            throw new BusinessRuleException("This loan has reached its renewal limit.");
        }
        if (reservations.existsByBookIdAndStatus(loan.getCopy().getBook().getId(), ReservationStatus.WAITING)) {
            throw new BusinessRuleException("This loan cannot be renewed because another member is waiting for the title.");
        }
        validateMember(loan.getMember());
        LocalDate base = loan.getDueOn().isAfter(LocalDate.now(clock)) ? loan.getDueOn() : LocalDate.now(clock);
        loan.setDueOn(base.plusDays(policy.getLoanPeriodDays()));
        loan.setRenewalCount(loan.getRenewalCount() + 1);
        notifications.notify(loan.getMember().getUserAccount(), "Loan renewed",
                loan.getCopy().getBook().getTitle() + " is now due on " + loan.getDueOn() + ".",
                NotificationType.INFO, "renewal:" + loan.getId() + ":" + loan.getRenewalCount());
        audit.record("LOAN_RENEWED", "Loan", loan.getId().toString(), "New due date " + loan.getDueOn() + " by " + actor.getEmail());
        return loan;
    }

    @Transactional(readOnly = true)
    public List<Loan> activeLoans() {
        return loans.findByStatusOrderByDueOnAsc(LoanStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<Loan> upcomingLoans() {
        return loans.findTop10ByStatusOrderByDueOnAsc(LoanStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<Loan> memberLoans(Long memberId) {
        return loans.findByMemberIdOrderByCheckedOutAtDesc(memberId);
    }

    @Transactional(readOnly = true)
    public long activeCount() {
        return loans.countByStatus(LoanStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public long overdueCount() {
        return loans.countOverdue(LocalDate.now(clock));
    }

    @Transactional(readOnly = true)
    public CirculationPolicy defaultPolicy() {
        return policies.findFirstByDefaultPolicyTrueOrderByIdAsc()
                .orElseThrow(() -> new BusinessRuleException("No default circulation policy is configured."));
    }

    @Transactional
    public CirculationPolicy updatePolicy(PolicyForm form) {
        CirculationPolicy policy = defaultPolicy();
        policy.setName(form.getName().trim());
        policy.setLoanPeriodDays(form.getLoanPeriodDays());
        policy.setMaxLoans(form.getMaxLoans());
        policy.setMaxRenewals(form.getMaxRenewals());
        policy.setGracePeriodDays(form.getGracePeriodDays());
        policy.setFinePerDay(form.getFinePerDay());
        policy.setMaxFinePerLoan(form.getMaxFinePerLoan());
        policy.setBlockAtBalance(form.getBlockAtBalance());
        policy.setPickupDays(form.getPickupDays());
        audit.record("POLICY_UPDATED", "CirculationPolicy", policy.getId().toString(), policy.getName());
        return policy;
    }

    private void validateMember(Member member) {
        if (member.getStatus() != MemberStatus.ACTIVE) throw new BusinessRuleException("This membership is not active.");
        if (member.getExpiresOn().isBefore(LocalDate.now(clock))) throw new BusinessRuleException("This membership has expired.");
    }

    public record ReturnResult(Loan loan, BigDecimal fine) {}
}
