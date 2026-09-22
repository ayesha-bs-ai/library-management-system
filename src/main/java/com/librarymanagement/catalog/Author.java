package com.librarymanagement.catalog;

import com.librarymanagement.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "author")
public class Author extends BaseEntity {
    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "normalized_name", nullable = false, unique = true, length = 150)
    private String normalizedName;
}
