package com.librarymanagement.reservation;

import com.librarymanagement.audit.AuditService;
import com.librarymanagement.catalog.Book;
import com.librarymanagement.catalog.CatalogService;
import com.librarymanagement.common.exception.BusinessRuleException;
import com.librarymanagement.common.exception.ResourceNotFoundException;
import com.librarymanagement.circulation.CirculationPolicy;
import com.librarymanagement.circulation.CirculationPolicyRepository;
import com.librarymanagement.inventory.BookCopy;
import com.librarymanagement.inventory.BookCopyRepository;
import com.librarymanagement.inventory.CopyStatus;
import com.librarymanagement.member.Member;
import com.librarymanagement.member.MemberService;
import com.librarymanagement.member.MemberStatus;
import com.librarymanagement.notification.NotificationService;
import com.librarymanagement.notification.NotificationType;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {
    private static final Set<ReservationStatus> ACTIVE = Set.of(ReservationStatus.WAITING, ReservationStatus.READY_FOR_PICKUP);

    private final ReservationRepository reservations;
    private final BookCopyRepository copies;
    private final CatalogService catalog;
    private final MemberService members;
    private final CirculationPolicyRepository policies;
    private final NotificationService notifications;
    private final AuditService audit;
    private final Clock clock;

    public ReservationService(ReservationRepository reservations, BookCopyRepository copies, CatalogService catalog,
                              MemberService members, CirculationPolicyRepository policies,
                              NotificationService notifications, AuditService audit, Clock clock) {
        this.reservations = reservations;
        this.copies = copies;
        this.catalog = catalog;
        this.members = members;
        this.policies = policies;
        this.notifications = notifications;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public Reservation place(Long bookId, Long memberId) {
        Member member = members.get(memberId);
        validateMember(member);
        Book book = catalog.get(bookId);
        if (reservations.existsByBookIdAndMemberIdAndStatusIn(bookId, memberId, ACTIVE)) {
            throw new BusinessRuleException("You already have an active reservation for this title.");
        }

        Reservation reservation = new Reservation();
        reservation.setBook(book);
        reservation.setMember(member);
        reservation.setStatus(ReservationStatus.WAITING);
        reservations.save(reservation);

        Optional<BookCopy> available = copies.findFirstByBookIdAndStatusOrderByIdAsc(bookId, CopyStatus.AVAILABLE);
        available.ifPresent(copy -> markReady(reservation, copy));
        audit.record("RESERVATION_CREATED", "Reservation", reservation.getId().toString(), member.getMembershipNumber() + " — " + book.getTitle());
        return reservation;
    }

    @Transactional
    public void cancel(Long reservationId, Member requester, boolean staffOverride) {
        Reservation reservation = get(reservationId);
        if (!staffOverride && !reservation.getMember().getId().equals(requester.getId())) {
            throw new BusinessRuleException("You can cancel only your own reservation.");
        }
        if (!ACTIVE.contains(reservation.getStatus())) {
            throw new BusinessRuleException("This reservation is no longer active.");
        }
        BookCopy released = reservation.getAssignedCopy();
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setAssignedCopy(null);
        if (released != null) {
            released.setStatus(CopyStatus.AVAILABLE);
            promoteNext(released);
        }
        audit.record("RESERVATION_CANCELLED", "Reservation", reservationId.toString(), reservation.getBook().getTitle());
    }

    @Transactional
    public void fulfillForCheckout(BookCopy copy, Member member) {
        reservations.findFirstByBookIdAndMemberIdAndStatus(copy.getBook().getId(), member.getId(), ReservationStatus.READY_FOR_PICKUP)
                .ifPresent(reservation -> {
                    if (reservation.getAssignedCopy() == null || reservation.getAssignedCopy().getId().equals(copy.getId())) {
                        reservation.setStatus(ReservationStatus.FULFILLED);
                        reservation.setFulfilledAt(Instant.now(clock));
                        reservation.setAssignedCopy(copy);
                        audit.record("RESERVATION_FULFILLED", "Reservation", reservation.getId().toString(), copy.getBarcode());
                    }
                });
    }

    @Transactional
    public void processReturnedCopy(BookCopy copy) {
        copy.setStatus(CopyStatus.AVAILABLE);
        promoteNext(copy);
    }

    @Transactional(readOnly = true)
    public Optional<Reservation> readyFor(Long bookId, Long memberId) {
        return reservations.findFirstByBookIdAndMemberIdAndStatus(bookId, memberId, ReservationStatus.READY_FOR_PICKUP);
    }

    @Transactional(readOnly = true)
    public List<Reservation> forMember(Long memberId) {
        return reservations.findByMemberIdAndStatusInOrderByCreatedAtDesc(memberId,
                Set.of(ReservationStatus.WAITING, ReservationStatus.READY_FOR_PICKUP, ReservationStatus.FULFILLED,
                        ReservationStatus.CANCELLED, ReservationStatus.EXPIRED));
    }

    @Transactional(readOnly = true)
    public List<Reservation> activeQueue() {
        return reservations.findByStatusInOrderByCreatedAtAsc(ACTIVE);
    }

    @Transactional(readOnly = true)
    public Reservation get(Long id) {
        return reservations.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
    }

    @Transactional(readOnly = true)
    public long waitingCount() {
        return reservations.countByStatus(ReservationStatus.WAITING);
    }

    @Scheduled(cron = "0 15 * * * *")
    @Transactional
    public void expireReadyReservations() {
        List<Reservation> expired = reservations.findByStatusAndExpiresAtBefore(ReservationStatus.READY_FOR_PICKUP, Instant.now(clock));
        for (Reservation reservation : expired) {
            BookCopy copy = reservation.getAssignedCopy();
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservation.setAssignedCopy(null);
            if (copy != null) {
                copy.setStatus(CopyStatus.AVAILABLE);
                promoteNext(copy);
            }
            audit.record("RESERVATION_EXPIRED", "Reservation", reservation.getId().toString(), reservation.getBook().getTitle());
        }
    }

    private void promoteNext(BookCopy copy) {
        reservations.findFirstByBookIdAndStatusOrderByCreatedAtAsc(copy.getBook().getId(), ReservationStatus.WAITING)
                .ifPresent(next -> markReady(next, copy));
    }

    private void markReady(Reservation reservation, BookCopy copy) {
        CirculationPolicy policy = policies.findFirstByDefaultPolicyTrueOrderByIdAsc()
                .orElseThrow(() -> new BusinessRuleException("No default circulation policy is configured."));
        Instant now = Instant.now(clock);
        reservation.setStatus(ReservationStatus.READY_FOR_PICKUP);
        reservation.setAssignedCopy(copy);
        reservation.setReadyAt(now);
        reservation.setExpiresAt(now.plus(policy.getPickupDays(), ChronoUnit.DAYS));
        copy.setStatus(CopyStatus.RESERVED);
        notifications.notify(reservation.getMember().getUserAccount(), "Your reservation is ready",
                reservation.getBook().getTitle() + " is ready for pickup until " + LocalDate.now(clock).plusDays(policy.getPickupDays()) + ".",
                NotificationType.RESERVATION_READY, "reservation-ready:" + reservation.getId());
    }

    private void validateMember(Member member) {
        if (member.getStatus() != MemberStatus.ACTIVE) throw new BusinessRuleException("This membership is not active.");
        if (member.getExpiresOn().isBefore(LocalDate.now(clock))) throw new BusinessRuleException("This membership has expired.");
    }
}
