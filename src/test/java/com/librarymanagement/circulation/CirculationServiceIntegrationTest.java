package com.librarymanagement.circulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.librarymanagement.auth.UserAccount;
import com.librarymanagement.auth.UserAccountRepository;
import com.librarymanagement.auth.UserRole;
import com.librarymanagement.branch.LibraryBranch;
import com.librarymanagement.branch.LibraryBranchRepository;
import com.librarymanagement.catalog.Book;
import com.librarymanagement.catalog.BookRepository;
import com.librarymanagement.common.exception.BusinessRuleException;
import com.librarymanagement.fine.FineService;
import com.librarymanagement.inventory.BookCopy;
import com.librarymanagement.inventory.BookCopyRepository;
import com.librarymanagement.inventory.CopyCondition;
import com.librarymanagement.inventory.CopyStatus;
import com.librarymanagement.member.Member;
import com.librarymanagement.member.MemberRepository;
import com.librarymanagement.member.MemberStatus;
import com.librarymanagement.member.MemberType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CirculationServiceIntegrationTest {
    @Autowired CirculationService circulation;
    @Autowired CirculationPolicyRepository policies;
    @Autowired UserAccountRepository users;
    @Autowired LibraryBranchRepository branches;
    @Autowired BookRepository books;
    @Autowired BookCopyRepository copies;
    @Autowired MemberRepository members;
    @Autowired LoanRepository loans;
    @Autowired FineService fines;

    private UserAccount librarian;
    private Member member;
    private BookCopy copy;

    @BeforeEach
    void setUp() {
        CirculationPolicy policy = new CirculationPolicy();
        policy.setName("Test policy");
        policy.setDefaultPolicy(true);
        policy.setLoanPeriodDays(14);
        policy.setMaxLoans(3);
        policy.setMaxRenewals(1);
        policy.setGracePeriodDays(0);
        policy.setFinePerDay(new BigDecimal("10.00"));
        policy.setMaxFinePerLoan(new BigDecimal("100.00"));
        policy.setBlockAtBalance(new BigDecimal("500.00"));
        policy.setPickupDays(3);
        policies.save(policy);

        librarian = new UserAccount();
        librarian.setDisplayName("Test Librarian");
        librarian.setEmail("librarian-test@example.com");
        librarian.setPasswordHash("not-used");
        librarian.setRole(UserRole.LIBRARIAN);
        users.save(librarian);

        LibraryBranch branch = new LibraryBranch();
        branch.setCode("TEST");
        branch.setName("Test Branch");
        branches.save(branch);

        Book book = new Book();
        book.setTitle("Testing Libraries");
        book.setLanguage("English");
        books.save(book);

        copy = new BookCopy();
        copy.setBook(book);
        copy.setBranch(branch);
        copy.setBarcode("TEST-0001");
        copy.setAccessionNumber("ACC-TEST-0001");
        copy.setStatus(CopyStatus.AVAILABLE);
        copy.setCondition(CopyCondition.GOOD);
        copies.save(copy);

        member = new Member();
        member.setMembershipNumber("MEM-TEST-1");
        member.setFullName("Test Member");
        member.setEmail("member-test@example.com");
        member.setType(MemberType.GENERAL);
        member.setStatus(MemberStatus.ACTIVE);
        member.setJoinedOn(LocalDate.now().minusDays(1));
        member.setExpiresOn(LocalDate.now().plusYears(1));
        members.save(member);
    }

    @Test
    void checkoutAndOverdueReturnUpdateInventoryAndLedger() {
        Loan loan = circulation.checkout(member.getMembershipNumber(), copy.getBarcode(), librarian);
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(copy.getStatus()).isEqualTo(CopyStatus.ON_LOAN);

        loan.setDueOn(LocalDate.now().minusDays(3));
        loans.saveAndFlush(loan);

        CirculationService.ReturnResult result = circulation.returnBook(copy.getBarcode(), CopyCondition.GOOD, librarian);

        assertThat(result.loan().getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(result.fine()).isEqualByComparingTo("30.00");
        assertThat(copy.getStatus()).isEqualTo(CopyStatus.AVAILABLE);
        assertThat(fines.balance(member.getId())).isEqualByComparingTo("30.00");
    }

    @Test
    void copyCannotBeCheckedOutTwice() {
        circulation.checkout(member.getMembershipNumber(), copy.getBarcode(), librarian);
        assertThatThrownBy(() -> circulation.checkout(member.getMembershipNumber(), copy.getBarcode(), librarian))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be issued");
    }
}
