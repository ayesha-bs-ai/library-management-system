package com.librarymanagement.config.seed;

import com.librarymanagement.audit.AuditEvent;
import com.librarymanagement.audit.AuditEventRepository;
import com.librarymanagement.auth.UserAccount;
import com.librarymanagement.auth.UserAccountRepository;
import com.librarymanagement.auth.UserRole;
import com.librarymanagement.branch.LibraryBranch;
import com.librarymanagement.branch.LibraryBranchRepository;
import com.librarymanagement.catalog.Author;
import com.librarymanagement.catalog.AuthorRepository;
import com.librarymanagement.catalog.Book;
import com.librarymanagement.catalog.BookRepository;
import com.librarymanagement.catalog.Category;
import com.librarymanagement.catalog.CategoryRepository;
import com.librarymanagement.circulation.CirculationPolicy;
import com.librarymanagement.circulation.CirculationPolicyRepository;
import com.librarymanagement.circulation.Loan;
import com.librarymanagement.circulation.LoanRepository;
import com.librarymanagement.circulation.LoanStatus;
import com.librarymanagement.fine.FineLedgerEntry;
import com.librarymanagement.fine.FineLedgerEntryRepository;
import com.librarymanagement.fine.LedgerEntryType;
import com.librarymanagement.inventory.BookCopy;
import com.librarymanagement.inventory.BookCopyRepository;
import com.librarymanagement.inventory.CopyCondition;
import com.librarymanagement.inventory.CopyStatus;
import com.librarymanagement.member.Member;
import com.librarymanagement.member.MemberRepository;
import com.librarymanagement.member.MemberStatus;
import com.librarymanagement.member.MemberType;
import com.librarymanagement.notification.Notification;
import com.librarymanagement.notification.NotificationRepository;
import com.librarymanagement.notification.NotificationType;
import com.librarymanagement.reservation.Reservation;
import com.librarymanagement.reservation.ReservationRepository;
import com.librarymanagement.reservation.ReservationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {
    private final UserAccountRepository users;
    private final LibraryBranchRepository branches;
    private final AuthorRepository authors;
    private final CategoryRepository categories;
    private final BookRepository books;
    private final BookCopyRepository copies;
    private final MemberRepository members;
    private final CirculationPolicyRepository policies;
    private final LoanRepository loans;
    private final ReservationRepository reservations;
    private final FineLedgerEntryRepository ledger;
    private final NotificationRepository notifications;
    private final AuditEventRepository audits;
    private final PasswordEncoder passwords;

    public DevDataInitializer(UserAccountRepository users, LibraryBranchRepository branches,
                              AuthorRepository authors, CategoryRepository categories, BookRepository books,
                              BookCopyRepository copies, MemberRepository members,
                              CirculationPolicyRepository policies, LoanRepository loans,
                              ReservationRepository reservations, FineLedgerEntryRepository ledger,
                              NotificationRepository notifications, AuditEventRepository audits,
                              PasswordEncoder passwords) {
        this.users = users;
        this.branches = branches;
        this.authors = authors;
        this.categories = categories;
        this.books = books;
        this.copies = copies;
        this.members = members;
        this.policies = policies;
        this.loans = loans;
        this.reservations = reservations;
        this.ledger = ledger;
        this.notifications = notifications;
        this.audits = audits;
        this.passwords = passwords;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.count() > 0) return;

        UserAccount admin = user("Ayesha Admin", "admin@library.local", "Admin@12345", UserRole.ADMIN);
        UserAccount librarian = user("Noor Librarian", "librarian@library.local", "Library@12345", UserRole.LIBRARIAN);
        UserAccount memberUser = user("Ali Raza", "member@library.local", "Member@12345", UserRole.MEMBER);

        LibraryBranch main = new LibraryBranch();
        main.setCode("MAIN");
        main.setName("Main Library");
        main.setAddress("Central campus");
        branches.save(main);

        CirculationPolicy policy = new CirculationPolicy();
        policy.setName("Standard borrowing policy");
        policy.setDefaultPolicy(true);
        policy.setLoanPeriodDays(14);
        policy.setMaxLoans(5);
        policy.setMaxRenewals(2);
        policy.setGracePeriodDays(1);
        policy.setFinePerDay(new BigDecimal("10.00"));
        policy.setMaxFinePerLoan(new BigDecimal("500.00"));
        policy.setBlockAtBalance(new BigDecimal("1000.00"));
        policy.setPickupDays(3);
        policies.save(policy);

        Category software = category("Software Engineering");
        Category fiction = category("Fiction");
        Category science = category("Science");
        Category business = category("Business");

        Book effectiveJava = book("9780134685991", "Effective Java", "Best practices for the Java platform",
                "A practical guide to writing robust, maintainable Java applications.", "Joshua Bloch", software, 2018, "Addison-Wesley");
        Book cleanCode = book("9780132350884", "Clean Code", "A handbook of agile software craftsmanship",
                "Principles and patterns for making code readable, testable, and maintainable.", "Robert C. Martin", software, 2008, "Prentice Hall");
        Book pragmatic = book("9780135957059", "The Pragmatic Programmer", "Your journey to mastery",
                "Timeless habits and techniques for thoughtful software development.", "David Thomas, Andrew Hunt", software, 2019, "Addison-Wesley");
        Book atomicHabits = book("9780735211292", "Atomic Habits", "Tiny changes, remarkable results",
                "A practical framework for building good habits and breaking bad ones.", "James Clear", business, 2018, "Avery");
        Book cosmos = book("9780345539434", "Cosmos", null,
                "A journey through science, civilization, and the universe.", "Carl Sagan", science, 2013, "Ballantine Books");
        Book alchemist = book("9780062315007", "The Alchemist", null,
                "A fable about courage, purpose, and following a personal legend.", "Paulo Coelho", fiction, 2014, "HarperOne");

        List<Book> allBooks = List.of(effectiveJava, cleanCode, pragmatic, atomicHabits, cosmos, alchemist);
        int sequence = 1001;
        for (Book book : allBooks) {
            copy(book, main, "LIB-" + sequence, "ACC-" + sequence, "A-" + ((sequence % 5) + 1));
            sequence++;
            copy(book, main, "LIB-" + sequence, "ACC-" + sequence, "A-" + ((sequence % 5) + 1));
            sequence++;
        }

        Member ali = member("MEM-1001", "Ali Raza", "member@library.local", memberUser, MemberType.STUDENT);
        Member sana = member("MEM-1002", "Sana Ahmed", "sana@example.com", null, MemberType.GENERAL);
        member("MEM-1003", "Usman Khan", "usman@example.com", null, MemberType.TEACHER);

        BookCopy loanedCopy = copies.findByBookIdOrderByAccessionNumberAsc(effectiveJava.getId()).getFirst();
        Loan active = loan(loanedCopy, ali, librarian, LocalDate.now().plusDays(4));
        loanedCopy.setStatus(CopyStatus.ON_LOAN);

        BookCopy overdueCopy = copies.findByBookIdOrderByAccessionNumberAsc(cleanCode.getId()).getFirst();
        loan(overdueCopy, sana, librarian, LocalDate.now().minusDays(5));
        overdueCopy.setStatus(CopyStatus.ON_LOAN);

        BookCopy heldCopy = copies.findByBookIdOrderByAccessionNumberAsc(atomicHabits.getId()).getFirst();
        heldCopy.setStatus(CopyStatus.RESERVED);
        Reservation reservation = new Reservation();
        reservation.setBook(atomicHabits);
        reservation.setMember(ali);
        reservation.setAssignedCopy(heldCopy);
        reservation.setStatus(ReservationStatus.READY_FOR_PICKUP);
        reservation.setReadyAt(Instant.now());
        reservation.setExpiresAt(Instant.now().plus(3, ChronoUnit.DAYS));
        reservations.save(reservation);

        FineLedgerEntry charge = new FineLedgerEntry();
        charge.setMember(ali);
        charge.setType(LedgerEntryType.ADJUSTMENT);
        charge.setAmount(new BigDecimal("120.00"));
        charge.setDescription("Demo opening balance");
        charge.setCreatedBy(librarian);
        ledger.save(charge);

        Notification welcome = new Notification();
        welcome.setUser(memberUser);
        welcome.setTitle("Welcome to Library");
        welcome.setMessage("Your member portal is ready. Search the catalog, review loans, and reserve books from anywhere.");
        welcome.setType(NotificationType.INFO);
        notifications.save(welcome);

        Notification due = new Notification();
        due.setUser(memberUser);
        due.setTitle("Book due soon");
        due.setMessage(effectiveJava.getTitle() + " is due on " + active.getDueOn() + ".");
        due.setType(NotificationType.DUE_SOON);
        due.setDedupeKey("demo-due:" + active.getId());
        notifications.save(due);

        AuditEvent event = new AuditEvent();
        event.setActorEmail("system");
        event.setAction("DEMO_DATA_CREATED");
        event.setEntityType("System");
        event.setEntityId("dev");
        event.setDetails("Safe development accounts and sample catalog created");
        audits.save(event);
    }

    private UserAccount user(String name, String email, String password, UserRole role) {
        UserAccount user = new UserAccount();
        user.setDisplayName(name);
        user.setEmail(email);
        user.setPasswordHash(passwords.encode(password));
        user.setRole(role);
        user.setEnabled(true);
        return users.save(user);
    }

    private Category category(String name) {
        Category category = new Category();
        category.setName(name);
        category.setNormalizedName(name.toLowerCase());
        return categories.save(category);
    }

    private Book book(String isbn, String title, String subtitle, String description, String authorNames,
                      Category category, int year, String publisher) {
        Book book = new Book();
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setSubtitle(subtitle);
        book.setDescription(description);
        book.setPublishedYear(year);
        book.setPublisher(publisher);
        book.setLanguage("English");
        book.setCategories(new LinkedHashSet<>(List.of(category)));
        LinkedHashSet<Author> bookAuthors = new LinkedHashSet<>();
        for (String name : authorNames.split(",")) {
            Author author = new Author();
            author.setName(name.trim());
            author.setNormalizedName(name.trim().toLowerCase());
            bookAuthors.add(authors.save(author));
        }
        book.setAuthors(bookAuthors);
        return books.save(book);
    }

    private BookCopy copy(Book book, LibraryBranch branch, String barcode, String accession, String shelf) {
        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setBranch(branch);
        copy.setBarcode(barcode);
        copy.setAccessionNumber(accession);
        copy.setShelfLocation(shelf);
        copy.setStatus(CopyStatus.AVAILABLE);
        copy.setCondition(CopyCondition.GOOD);
        copy.setAcquiredOn(LocalDate.now().minusMonths(3));
        copy.setPurchasePrice(new BigDecimal("2500.00"));
        return copies.save(copy);
    }

    private Member member(String number, String name, String email, UserAccount account, MemberType type) {
        Member member = new Member();
        member.setMembershipNumber(number);
        member.setFullName(name);
        member.setEmail(email);
        member.setPhone("+92 300 0000000");
        member.setType(type);
        member.setStatus(MemberStatus.ACTIVE);
        member.setJoinedOn(LocalDate.now().minusMonths(2));
        member.setExpiresOn(LocalDate.now().plusYears(1));
        member.setUserAccount(account);
        return members.save(member);
    }

    private Loan loan(BookCopy copy, Member member, UserAccount librarian, LocalDate dueOn) {
        Loan loan = new Loan();
        loan.setCopy(copy);
        loan.setMember(member);
        loan.setCheckedOutAt(Instant.now().minus(10, ChronoUnit.DAYS));
        loan.setDueOn(dueOn);
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setCheckedOutBy(librarian);
        loan.setPolicyLoanDays(14);
        loan.setPolicyGraceDays(1);
        loan.setPolicyFinePerDay(new BigDecimal("10.00"));
        loan.setPolicyMaxFine(new BigDecimal("500.00"));
        return loans.save(loan);
    }
}
