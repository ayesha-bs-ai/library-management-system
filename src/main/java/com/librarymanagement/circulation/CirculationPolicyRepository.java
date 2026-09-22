package com.librarymanagement.circulation;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CirculationPolicyRepository extends JpaRepository<CirculationPolicy, Long> {
    Optional<CirculationPolicy> findFirstByDefaultPolicyTrueOrderByIdAsc();
}
