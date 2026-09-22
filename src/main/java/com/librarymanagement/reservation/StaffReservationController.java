package com.librarymanagement.reservation;

import com.librarymanagement.common.exception.BusinessRuleException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/staff/reservations")
public class StaffReservationController {
    private final ReservationService reservations;

    public StaffReservationController(ReservationService reservations) {
        this.reservations = reservations;
    }

    @GetMapping
    public String queue(Model model) {
        model.addAttribute("reservations", reservations.activeQueue());
        model.addAttribute("title", "Reservation queue");
        return "staff/reservations/list";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            reservations.cancel(id, null, true);
            redirect.addFlashAttribute("success", "Reservation cancelled.");
        } catch (BusinessRuleException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/staff/reservations";
    }
}
