package com.librarymanagement.reporting;

import com.librarymanagement.catalog.BookRepository;
import com.librarymanagement.circulation.CirculationService;
import com.librarymanagement.circulation.Loan;
import com.librarymanagement.inventory.BookCopyRepository;
import com.librarymanagement.inventory.CopyStatus;
import com.librarymanagement.member.Member;
import com.librarymanagement.member.MemberRepository;
import com.librarymanagement.member.MemberStatus;
import com.librarymanagement.reservation.ReservationService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final BookRepository books;
    private final BookCopyRepository copies;
    private final MemberRepository members;
    private final CirculationService circulation;
    private final ReservationService reservations;

    public DashboardService(BookRepository books, BookCopyRepository copies, MemberRepository members,
                            CirculationService circulation, ReservationService reservations) {
        this.books = books;
        this.copies = copies;
        this.members = members;
        this.circulation = circulation;
        this.reservations = reservations;
    }

    @Transactional(readOnly = true)
    public Snapshot snapshot() {
        return new Snapshot(
                books.countByArchivedFalse(),
                copies.count(),
                copies.countByStatus(CopyStatus.AVAILABLE),
                members.countByStatus(MemberStatus.ACTIVE),
                circulation.activeCount(),
                circulation.overdueCount(),
                reservations.waitingCount(),
                circulation.upcomingLoans(),
                members.findTop5ByOrderByCreatedAtDesc());
    }

    public record Snapshot(long titles, long copies, long availableCopies, long activeMembers,
                           long activeLoans, long overdueLoans, long waitingReservations,
                           List<Loan> upcomingLoans, List<Member> recentMembers) {}
}
