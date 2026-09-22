package com.librarymanagement.inventory;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {
    @EntityGraph(attributePaths = {"book", "book.authors", "branch"})
    List<BookCopy> findByBookIdOrderByAccessionNumberAsc(Long bookId);

    @EntityGraph(attributePaths = {"book", "book.authors", "branch"})
    Optional<BookCopy> findByBarcodeIgnoreCase(String barcode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"book", "branch"})
    @Query("select c from BookCopy c where lower(c.barcode) = lower(:barcode)")
    Optional<BookCopy> findByBarcodeForUpdate(@Param("barcode") String barcode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<BookCopy> findFirstByBookIdAndStatusOrderByIdAsc(Long bookId, CopyStatus status);

    long countByStatus(CopyStatus status);
    long countByBookIdAndStatus(Long bookId, CopyStatus status);
    boolean existsByBarcodeIgnoreCase(String barcode);
    boolean existsByAccessionNumberIgnoreCase(String accessionNumber);
}
