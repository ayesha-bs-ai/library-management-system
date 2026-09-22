package com.librarymanagement.catalog.api;

import com.librarymanagement.catalog.Book;
import com.librarymanagement.catalog.CatalogService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/books")
public class BookApiController {
    private final CatalogService catalog;

    public BookApiController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public BookPage list(@RequestParam(defaultValue = "") String q,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "20") int size) {
        Page<Book> result = catalog.search(q, page, size);
        return new BookPage(result.getContent().stream().map(this::summary).toList(),
                result.getNumber(), result.getTotalPages(), result.getTotalElements());
    }

    @GetMapping("/{id}")
    public BookSummary get(@PathVariable Long id) {
        return summary(catalog.get(id));
    }

    private BookSummary summary(Book book) {
        return new BookSummary(book.getId(), book.getIsbn(), book.getTitle(), book.getSubtitle(),
                book.authorNames(), book.categoryNames(), book.getPublisher(), book.getPublishedYear(),
                catalog.availableCopies(book.getId()));
    }

    public record BookSummary(Long id, String isbn, String title, String subtitle, String authors,
                              String categories, String publisher, Integer publishedYear, long availableCopies) {}

    public record BookPage(List<BookSummary> items, int page, int totalPages, long totalItems) {}
}
