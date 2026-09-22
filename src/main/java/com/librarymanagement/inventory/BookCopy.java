package com.librarymanagement.inventory;

import com.librarymanagement.branch.LibraryBranch;
import com.librarymanagement.catalog.Book;
import com.librarymanagement.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "book_copy")
public class BookCopy extends BaseEntity {
    @Column(nullable = false, unique = true, length = 60)
    private String barcode;

    @Column(name = "accession_number", nullable = false, unique = true, length = 60)
    private String accessionNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private LibraryBranch branch;

    @Column(name = "shelf_location", length = 80)
    private String shelfLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CopyStatus status = CopyStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_status", nullable = false, length = 20)
    private CopyCondition condition = CopyCondition.GOOD;

    @Column(name = "acquired_on")
    private LocalDate acquiredOn;

    @Column(name = "purchase_price", precision = 12, scale = 2)
    private BigDecimal purchasePrice;
}
