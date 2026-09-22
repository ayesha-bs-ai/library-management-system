package com.librarymanagement.member;

import com.librarymanagement.auth.CurrentUserService;
import com.librarymanagement.auth.UserAccount;
import com.librarymanagement.circulation.CirculationService;
import com.librarymanagement.common.exception.BusinessRuleException;
import com.librarymanagement.common.exception.ResourceNotFoundException;
import com.librarymanagement.fine.FineService;
import com.librarymanagement.notification.NotificationService;
import com.librarymanagement.reservation.ReservationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/my")
public class MemberPortalController {
    private final CurrentUserService currentUsers;
    private final MemberService members;
    private final CirculationService circulation;
    private final ReservationService reservations;
    private final FineService fines;
    private final NotificationService notifications;

    public MemberPortalController(CurrentUserService currentUsers, MemberService members,
                                  CirculationService circulation, ReservationService reservations,
                                  FineService fines, NotificationService notifications) {
        this.currentUsers = currentUsers;
        this.members = members;
        this.circulation = circulation;
        this.reservations = reservations;
        this.fines = fines;
        this.notifications = notifications;
    }

    @GetMapping("/account")
    public String account(Model model) {
        UserAccount user = currentUsers.requireCurrentUser();
        Member member = members.getForUser(user.getEmail());
        model.addAttribute("member", member);
        model.addAttribute("loans", circulation.memberLoans(member.getId()));
        model.addAttribute("reservations", reservations.forMember(member.getId()));
        model.addAttribute("balance", fines.balance(member.getId()));
        model.addAttribute("ledger", fines.ledger(member.getId()));
        model.addAttribute("notifications", notifications.latest(user.getId()));
        model.addAttribute("title", "My library");
        return "member/account";
    }

    @PostMapping("/reservations/books/{bookId}")
    public String reserve(@PathVariable Long bookId, RedirectAttributes redirect) {
        try {
            Member member = currentMember();
            var reservation = reservations.place(bookId, member.getId());
            redirect.addFlashAttribute("success", reservation.getStatus().name().equals("READY_FOR_PICKUP")
                    ? "Reserved and ready for pickup." : "You joined the reservation queue.");
        } catch (BusinessRuleException | ResourceNotFoundException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/catalog/books/" + bookId;
    }

    @PostMapping("/reservations/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            reservations.cancel(id, currentMember(), false);
            redirect.addFlashAttribute("success", "Reservation cancelled.");
        } catch (BusinessRuleException | ResourceNotFoundException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/my/account";
    }

    @PostMapping("/loans/{id}/renew")
    public String renew(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            Member member = currentMember();
            var loan = circulation.renew(id, currentUsers.requireCurrentUser(), member.getId());
            redirect.addFlashAttribute("success", "Loan renewed until " + loan.getDueOn() + ".");
        } catch (BusinessRuleException | ResourceNotFoundException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/my/account";
    }

    @PostMapping("/notifications/{id}/read")
    public String markRead(@PathVariable Long id) {
        UserAccount user = currentUsers.requireCurrentUser();
        notifications.markRead(id, user.getId());
        return "redirect:/my/account#notifications";
    }

    private Member currentMember() {
        return members.getForUser(currentUsers.requireCurrentUser().getEmail());
    }
}
