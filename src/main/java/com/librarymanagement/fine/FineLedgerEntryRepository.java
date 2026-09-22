package com.librarymanagement.fine;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FineLedgerEntryRepository extends JpaRepository<FineLedgerEntry, Long> {
    @Query("select coalesce(sum(e.amount), 0) from FineLedgerEntry e where e.member.id = :memberId")
    BigDecimal balanceForMember(@Param("memberId") Long memberId);

    @EntityGraph(attributePaths = {"member", "loan", "createdBy"})
    List<FineLedgerEntry> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    @EntityGraph(attributePaths = {"member", "loan", "createdBy"})
    List<FineLedgerEntry> findTop50ByOrderByCreatedAtDesc();

    boolean existsByLoanIdAndType(Long loanId, LedgerEntryType type);
}
