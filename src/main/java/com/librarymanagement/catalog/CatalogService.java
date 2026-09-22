package com.librarymanagement.catalog;

import com.librarymanagement.audit.AuditService;
import com.librarymanagement.branch.LibraryBranch;
import com.librarymanagement.branch.LibraryBranchRepository;
import com.librarymanagement.catalog.web.BookForm;
import com.librarymanagement.common.exception.BusinessRuleException;
import com.librarymanagement.common.exception.ResourceNotFoundException;
import com.librarymanagement.inventory.BookCopy;
import com.librarymanagement.inventory.BookCopyRepository;
import com.librarymanagement.inventory.CopyStatus;
import com.librarymanagement.inventory.web.CopyForm;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
    private final BookRepository books;
    private final AuthorRepository authors;
    private final CategoryRepository categories;
    private final BookCopyRepository copies;
    private final LibraryBranchRepository branches;
    private final AuditService audit;

    public CatalogService(BookRepository books, AuthorRepository authors, CategoryRepository categories,
                          BookCopyRepository copies, LibraryBranchRepository branches, AuditService audit) {
        this.books = books;
        this.authors = authors;
        this.categories = categories;
        this.copies = copies;
        this.branches = branches;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Page<Book> search(String query, int page, int size) {
        String safeQuery = query == null ? "" : query.trim();
        Page<Book> result = books.search(safeQuery, PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 50), Sort.by("title").ascending()));
        result.getContent().forEach(book -> {
            book.getAuthors().size();
            book.getCategories().size();
        });
        return result;
    }

    @Transactional(readOnly = true)
    public Book get(Long id) {
        return books.findById(id).orElseThrow(() -> new ResourceNotFoundException("Book not found"));
    }

    @Transactional(readOnly = true)
    public List<BookCopy> copiesFor(Long bookId) {
        return copies.findByBookIdOrderByAccessionNumberAsc(bookId);
    }

    @Transactional(readOnly = true)
    public long availableCopies(Long bookId) {
        return copies.countByBookIdAndStatus(bookId, CopyStatus.AVAILABLE);
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> availabilityFor(List<Book> bookList) {
        return bookList.stream().collect(Collectors.toMap(Book::getId, b -> availableCopies(b.getId())));
    }

    @Transactional
    public Book create(BookForm form) {
        Book book = new Book();
        apply(book, form);
        books.save(book);
        audit.record("BOOK_CREATED", "Book", book.getId().toString(), book.getTitle());
        return book;
    }

    @Transactional
    public Book update(Long id, BookForm form) {
        Book book = get(id);
        apply(book, form);
        audit.record("BOOK_UPDATED", "Book", id.toString(), book.getTitle());
        return book;
    }

    @Transactional
    public void archive(Long id) {
        Book book = get(id);
        long unavailable = copiesFor(id).stream().filter(c -> c.getStatus() == CopyStatus.ON_LOAN || c.getStatus() == CopyStatus.RESERVED).count();
        if (unavailable > 0) throw new BusinessRuleException("Return or resolve all loaned and reserved copies before archiving this title.");
        book.setArchived(true);
        audit.record("BOOK_ARCHIVED", "Book", id.toString(), book.getTitle());
    }

    @Transactional
    public BookCopy addCopy(Long bookId, CopyForm form) {
        if (copies.existsByBarcodeIgnoreCase(form.getBarcode().trim())) {
            throw new BusinessRuleException("That barcode is already assigned to another copy.");
        }
        if (copies.existsByAccessionNumberIgnoreCase(form.getAccessionNumber().trim())) {
            throw new BusinessRuleException("That accession number already exists.");
        }
        Book book = get(bookId);
        LibraryBranch branch = branches.findById(form.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setBranch(branch);
        copy.setBarcode(form.getBarcode().trim().toUpperCase(Locale.ROOT));
        copy.setAccessionNumber(form.getAccessionNumber().trim().toUpperCase(Locale.ROOT));
        copy.setShelfLocation(trimToNull(form.getShelfLocation()));
        copy.setCondition(form.getCondition());
        copy.setAcquiredOn(form.getAcquiredOn());
        copy.setPurchasePrice(form.getPurchasePrice());
        copy.setStatus(CopyStatus.AVAILABLE);
        copies.save(copy);
        audit.record("COPY_CREATED", "BookCopy", copy.getId().toString(), copy.getBarcode() + " — " + book.getTitle());
        return copy;
    }

    @Transactional(readOnly = true)
    public List<LibraryBranch> activeBranches() {
        return branches.findByActiveTrueOrderByNameAsc();
    }

    private void apply(Book book, BookForm form) {
        String isbn = normalizeIsbn(form.getIsbn());
        boolean duplicate = isbn != null && (book.getId() == null ? books.existsByIsbn(isbn) : books.existsByIsbnAndIdNot(isbn, book.getId()));
        if (duplicate) throw new BusinessRuleException("A title with this ISBN already exists.");

        book.setIsbn(isbn);
        book.setTitle(form.getTitle().trim());
        book.setSubtitle(trimToNull(form.getSubtitle()));
        book.setDescription(trimToNull(form.getDescription()));
        book.setLanguage(form.getLanguage().trim());
        book.setPublishedYear(form.getPublishedYear());
        book.setPublisher(trimToNull(form.getPublisher()));
        book.setAuthors(resolveAuthors(form.getAuthors()));
        book.setCategories(resolveCategories(form.getCategories()));
    }

    private Set<Author> resolveAuthors(String commaSeparated) {
        return split(commaSeparated).stream().map(name -> {
            String normalized = normalize(name);
            return authors.findByNormalizedName(normalized).orElseGet(() -> {
                Author author = new Author();
                author.setName(name.trim());
                author.setNormalizedName(normalized);
                return authors.save(author);
            });
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Category> resolveCategories(String commaSeparated) {
        return split(commaSeparated).stream().map(name -> {
            String normalized = normalize(name);
            return categories.findByNormalizedName(normalized).orElseGet(() -> {
                Category category = new Category();
                category.setName(name.trim());
                category.setNormalizedName(normalized);
                return categories.save(category);
            });
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<String> split(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private String normalizeIsbn(String value) {
        if (value == null || value.isBlank()) return null;
        return value.replaceAll("[-\\s]", "").toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
