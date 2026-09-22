package com.librarymanagement.catalog;

import com.librarymanagement.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "book")
public class Book extends BaseEntity {
    @Column(unique = true, length = 20)
    private String isbn;

    @Column(nullable = false, length = 240)
    private String title;

    @Column(length = 240)
    private String subtitle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String language = "English";

    @Column(name = "published_year")
    private Integer publishedYear;

    @Column(length = 160)
    private String publisher;

    @Column(nullable = false)
    private boolean archived = false;

    @Column(name = "cover_image_path", length = 500)
    private String coverImagePath;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "book_author",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id"))
    private Set<Author> authors = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "book_category",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new LinkedHashSet<>();

    public String authorNames() {
        return authors.stream().map(Author::getName).sorted().reduce((a, b) -> a + ", " + b).orElse("Unknown author");
    }

    public String categoryNames() {
        return categories.stream().map(Category::getName).sorted().reduce((a, b) -> a + ", " + b).orElse("Uncategorized");
    }

    public String getCoverImage() {
        if (coverImagePath != null && !coverImagePath.isBlank()) {
            return "/uploads/covers/" + coverImagePath;
        }
        if (coverImageUrl != null && !coverImageUrl.isBlank()) {
            return coverImageUrl;
        }
        return null;
    }

    public boolean hasCoverImage() {
        return (coverImagePath != null && !coverImagePath.isBlank()) || 
               (coverImageUrl != null && !coverImageUrl.isBlank());
    }
}
