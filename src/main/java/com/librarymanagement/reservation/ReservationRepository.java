package com.librarymanagement.reservation;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @Override
    @EntityGraph(attributePaths = {"book", "book.authors", "member", "assignedCopy"})
    Optional<Reservation> findById(Long id);

    @EntityGraph(attributePaths = {"book", "book.authors", "member", "assignedCopy"})
    List<Reservation> findByStatusInOrderByCreatedAtAsc(Collection<ReservationStatus> statuses);

    @EntityGraph(attributePaths = {"book", "book.authors", "assignedCopy"})
    List<Reservation> findByMemberIdAndStatusInOrderByCreatedAtDesc(Long memberId, Collection<ReservationStatus> statuses);

    @EntityGraph(attributePaths = {"member", "member.userAccount", "book"})
    Optional<Reservation> findFirstByBookIdAndStatusOrderByCreatedAtAsc(Long bookId, ReservationStatus status);

    @EntityGraph(attributePaths = {"book", "member", "assignedCopy"})
    Optional<Reservation> findFirstByBookIdAndMemberIdAndStatus(Long bookId, Long memberId, ReservationStatus status);

    boolean existsByBookIdAndMemberIdAndStatusIn(Long bookId, Long memberId, Collection<ReservationStatus> statuses);
    boolean existsByBookIdAndStatus(Long bookId, ReservationStatus status);
    long countByStatus(ReservationStatus status);

    @EntityGraph(attributePaths = {"member", "member.userAccount", "book", "assignedCopy"})
    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant now);
}
