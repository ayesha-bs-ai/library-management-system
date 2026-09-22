package com.librarymanagement.circulation.web;

import com.librarymanagement.auth.CurrentUserService;
import com.librarymanagement.circulation.CirculationService;
import com.librarymanagement.circulation.Loan;
import com.librarymanagement.common.exception.BusinessRuleException;
import com.librarymanagement.common.exception.ResourceNotFoundException;
import com.librarymanagement.inventory.CopyCondition;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/staff/circulation")
public class CirculationController {
    private final CirculationService circulation;
    private final CurrentUserService currentUsers;

    public CirculationController(CirculationService circulation, CurrentUserService currentUsers) {
        this.circulation = circulation;
        this.currentUsers = currentUsers;
    }

    @GetMapping
    public String desk(Model model) {
        if (!model.containsAttribute("checkoutForm")) model.addAttribute("checkoutForm", new CheckoutForm());
        if (!model.containsAttribute("returnForm")) model.addAttribute("returnForm", new ReturnForm());
        addModel(model);
        return "staff/circulation/desk";
    }

    @PostMapping("/checkout")
    public String checkout(@Valid @ModelAttribute CheckoutForm checkoutForm, BindingResult binding,
                           RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            redirect.addFlashAttribute("org.springframework.validation.BindingResult.checkoutForm", binding);
            redirect.addFlashAttribute("checkoutForm", checkoutForm);
            redirect.addFlashAttribute("error", "Enter both a member number and copy barcode.");
            return "redirect:/staff/circulation";
        }
        try {
            Loan loan = circulation.checkout(checkoutForm.getMemberNumber(), checkoutForm.getBarcode(), currentUsers.requireCurrentUser());
            redirect.addFlashAttribute("success", loan.getCopy().getBook().getTitle() + " issued to " + loan.getMember().getFullName() + ". Due " + loan.getDueOn() + ".");
        } catch (BusinessRuleException | ResourceNotFoundException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            redirect.addFlashAttribute("checkoutForm", checkoutForm);
        }
        return "redirect:/staff/circulation";
    }

    @PostMapping("/return")
    public String returnBook(@Valid @ModelAttribute ReturnForm returnForm, BindingResult binding,
                             RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            redirect.addFlashAttribute("error", "Enter a valid copy barcode and condition.");
            redirect.addFlashAttribute("returnForm", returnForm);
            return "redirect:/staff/circulation";
        }
        try {
            var result = circulation.returnBook(returnForm.getBarcode(), returnForm.getCondition(), currentUsers.requireCurrentUser());
            String message = result.loan().getCopy().getBook().getTitle() + " returned successfully.";
            if (result.fine().signum() > 0) message += " Overdue charge: " + result.fine() + ".";
            redirect.addFlashAttribute("success", message);
        } catch (BusinessRuleException | ResourceNotFoundException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            redirect.addFlashAttribute("returnForm", returnForm);
        }
        return "redirect:/staff/circulation";
    }

    @PostMapping("/loans/{id}/renew")
    public String renew(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            Loan loan = circulation.renew(id, currentUsers.requireCurrentUser(), null);
            redirect.addFlashAttribute("success", "Loan renewed until " + loan.getDueOn() + ".");
        } catch (BusinessRuleException | ResourceNotFoundException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/staff/circulation";
    }

    private void addModel(Model model) {
        model.addAttribute("activeLoans", circulation.activeLoans());
        model.addAttribute("conditions", CopyCondition.values());
        model.addAttribute("title", "Circulation desk");
    }
}
