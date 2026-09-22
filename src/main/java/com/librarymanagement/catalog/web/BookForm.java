package com.librarymanagement.catalog.web;

import com.librarymanagement.catalog.Book;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class BookForm {
    @Size(max = 20)
    private String isbn;

    @NotBlank
    @Size(max = 240)
    private String title;

    @Size(max = 240)
    private String subtitle;

    @Size(max = 5000)
    private String description;

    @NotBlank
    @Size(max = 50)
    private String language = "English";

    @Min(1000)
    @Max(2200)
    private Integer publishedYear;

    @Size(max = 160)
    private String publisher;

    @NotBlank(message = "Add at least one author")
    private String authors;

    @NotBlank(message = "Add at least one category")
    private String categories;

    // Image upload fields
    private MultipartFile coverImage;
    
    @Size(max = 500, message = "Image URL too long")
    private String coverImageUrl;
    
    private String existingCoverImagePath;
    
    private boolean removeCoverImage = false;

    public static BookForm from(Book book) {
        BookForm form = new BookForm();
        form.setIsbn(book.getIsbn());
        form.setTitle(book.getTitle());
        form.setSubtitle(book.getSubtitle());
        form.setDescription(book.getDescription());
        form.setLanguage(book.getLanguage());
        form.setPublishedYear(book.getPublishedYear());
        form.setPublisher(book.getPublisher());
        form.setAuthors(book.authorNames().replace("Unknown author", ""));
        form.setCategories(book.categoryNames().replace("Uncategorized", ""));
        form.setCoverImageUrl(book.getCoverImageUrl());
        form.setExistingCoverImagePath(book.getCoverImagePath());
        return form;
    }
}
