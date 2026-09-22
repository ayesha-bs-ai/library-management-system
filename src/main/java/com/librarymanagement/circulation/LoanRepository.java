package com.librarymanagement.circulation;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"copy", "copy.book", "member", "checkedOutBy"})
    @Query("select l from Loan l where lower(l.copy.barcode) = lower(:barcode) and l.status = com.librarymanagement.circulation.LoanStatus.ACTIVE")
    Optional<Loan> findActiveByBarcodeForUpdate(@Param("barcode") String barcode);

    @Override
    @EntityGraph(attributePaths = {"copy", "copy.book", "member", "checkedOutBy", "returnedBy"})
    Optional<Loan> findById(Long id);

    @EntityGraph(attributePaths = {"copy", "copy.book", "member"})
    List<Loan> findByStatusOrderByDueOnAsc(LoanStatus status);

    @EntityGraph(attributePaths = {"copy", "copy.book", "member"})
    List<Loan> findTop10ByStatusOrderByDueOnAsc(LoanStatus status);

    @EntityGraph(attributePaths = {"copy", "copy.book"})
    List<Loan> findByMemberIdOrderByCheckedOutAtDesc(Long memberId);

    @EntityGraph(attributePaths = {"copy", "copy.book"})
    List<Loan> findByMemberIdAndStatusOrderByDueOnAsc(Long memberId, LoanStatus status);

    @EntityGraph(attributePaths = {"copy", "copy.book", "member", "member.userAccount"})
    List<Loan> findByStatusAndDueOnBetween(LoanStatus status, LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = {"copy", "copy.book", "member", "member.userAccount"})
    List<Loan> findByStatusAndDueOnBefore(LoanStatus status, LocalDate date);

    long countByStatus(LoanStatus status);
    long countByMemberIdAndStatus(Long memberId, LoanStatus status);

    @Query("select count(l) from Loan l where l.status = com.librarymanagement.circulation.LoanStatus.ACTIVE and l.dueOn < :today")
    long countOverdue(@Param("today") LocalDate today);

    @Query("select count(l) from Loan l where l.status = com.librarymanagement.circulation.LoanStatus.ACTIVE and l.copy.book.id = :bookId")
    long countActiveByBookId(@Param("bookId") Long bookId);
}
