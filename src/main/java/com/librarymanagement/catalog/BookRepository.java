package com.librarymanagement.catalog;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {
    @Query(value = """
            select distinct b from Book b
            left join b.authors a
            left join b.categories c
            where b.archived = false and (
              :query = '' or lower(b.title) like lower(concat('%', :query, '%'))
              or lower(coalesce(b.isbn, '')) like lower(concat('%', :query, '%'))
              or lower(coalesce(b.publisher, '')) like lower(concat('%', :query, '%'))
              or lower(a.name) like lower(concat('%', :query, '%'))
              or lower(c.name) like lower(concat('%', :query, '%'))
            )
            """,
            countQuery = """
            select count(distinct b) from Book b
            left join b.authors a
            left join b.categories c
            where b.archived = false and (
              :query = '' or lower(b.title) like lower(concat('%', :query, '%'))
              or lower(coalesce(b.isbn, '')) like lower(concat('%', :query, '%'))
              or lower(coalesce(b.publisher, '')) like lower(concat('%', :query, '%'))
              or lower(a.name) like lower(concat('%', :query, '%'))
              or lower(c.name) like lower(concat('%', :query, '%'))
            )
            """)
    Page<Book> search(@Param("query") String query, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"authors", "categories"})
    Optional<Book> findById(Long id);

    boolean existsByIsbn(String isbn);
    boolean existsByIsbnAndIdNot(String isbn, Long id);
    long countByArchivedFalse();
}
