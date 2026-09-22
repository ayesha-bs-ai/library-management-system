package com.librarymanagement.notification;

import com.librarymanagement.circulation.Loan;
import com.librarymanagement.circulation.LoanRepository;
import com.librarymanagement.circulation.LoanStatus;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LoanReminderJob {
    private final LoanRepository loans;
    private final NotificationService notifications;
    private final Clock clock;

    public LoanReminderJob(LoanRepository loans, NotificationService notifications, Clock clock) {
        this.loans = loans;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void createDailyReminders() {
        LocalDate today = LocalDate.now(clock);
        for (Loan loan : loans.findByStatusAndDueOnBetween(LoanStatus.ACTIVE, today, today.plusDays(2))) {
            notifications.notify(loan.getMember().getUserAccount(), "Book due soon",
                    loan.getCopy().getBook().getTitle() + " is due on " + loan.getDueOn() + ".",
                    NotificationType.DUE_SOON, "due-soon:" + loan.getId() + ":" + loan.getDueOn());
        }
        for (Loan loan : loans.findByStatusAndDueOnBefore(LoanStatus.ACTIVE, today)) {
            notifications.notify(loan.getMember().getUserAccount(), "Book overdue",
                    loan.getCopy().getBook().getTitle() + " was due on " + loan.getDueOn() + ". Please return it as soon as possible.",
                    NotificationType.OVERDUE, "overdue:" + loan.getId() + ":" + today);
        }
    }
}
