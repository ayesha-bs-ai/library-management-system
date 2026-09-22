package com.librarymanagement.member;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {
    @EntityGraph(attributePaths = "userAccount")
    Optional<Member> findByMembershipNumberIgnoreCase(String membershipNumber);

    @EntityGraph(attributePaths = "userAccount")
    Optional<Member> findByUserAccountEmailIgnoreCase(String email);

    @Override
    @EntityGraph(attributePaths = "userAccount")
    Optional<Member> findById(Long id);

    @Query("""
            select m from Member m where :query = ''
            or lower(m.fullName) like lower(concat('%', :query, '%'))
            or lower(m.membershipNumber) like lower(concat('%', :query, '%'))
            or lower(m.email) like lower(concat('%', :query, '%'))
            order by m.fullName
            """)
    Page<Member> search(@Param("query") String query, Pageable pageable);

    boolean existsByMembershipNumberIgnoreCase(String membershipNumber);
    boolean existsByMembershipNumberIgnoreCaseAndIdNot(String membershipNumber, Long id);
    long countByStatus(MemberStatus status);
    List<Member> findTop5ByOrderByCreatedAtDesc();
}
