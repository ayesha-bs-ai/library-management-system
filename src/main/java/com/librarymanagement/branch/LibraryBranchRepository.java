package com.librarymanagement.branch;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryBranchRepository extends JpaRepository<LibraryBranch, Long> {
    Optional<LibraryBranch> findByCode(String code);
    List<LibraryBranch> findByActiveTrueOrderByNameAsc();
}
