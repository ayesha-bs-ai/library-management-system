package com.librarymanagement.fine.web;

import com.librarymanagement.auth.CurrentUserService;
import com.librarymanagement.common.exception.BusinessRuleException;
import com.librarymanagement.fine.FineService;
import com.librarymanagement.member.Member;
import com.librarymanagement.member.MemberService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/staff/fines")
public class FineController {
    private final FineService fines;
    private final MemberService members;
    private final CurrentUserService currentUsers;

    public FineController(FineService fines, MemberService members, CurrentUserService currentUsers) {
        this.fines = fines;
        this.members = members;
        this.currentUsers = currentUsers;
    }

    @GetMapping
    public String ledger(@RequestParam(required = false) Long memberId, Model model) {
        PaymentForm form = new PaymentForm();
        if (memberId != null) form.setMemberId(memberId);
        if (!model.containsAttribute("paymentForm")) model.addAttribute("paymentForm", form);
        if (memberId != null) {
            Member member = members.get(memberId);
            model.addAttribute("selectedMember", member);
            model.addAttribute("balance", fines.balance(memberId));
            model.addAttribute("entries", fines.ledger(memberId));
        } else {
            model.addAttribute("entries", fines.recent());
        }
        model.addAttribute("title", "Fines and payments");
        return "staff/fines/list";
    }

    @PostMapping("/payments")
    public String payment(@Valid @ModelAttribute PaymentForm paymentForm, BindingResult binding,
                          RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            redirect.addFlashAttribute("error", "Enter a valid member, amount, and payment note.");
            return redirect(paymentForm.getMemberId());
        }
        try {
            fines.recordPayment(paymentForm.getMemberId(), paymentForm.getAmount(), paymentForm.getDescription(), currentUsers.requireCurrentUser());
            redirect.addFlashAttribute("success", "Payment recorded.");
        } catch (BusinessRuleException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return redirect(paymentForm.getMemberId());
    }

    @PostMapping("/waivers")
    public String waiver(@Valid @ModelAttribute PaymentForm paymentForm, BindingResult binding,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            redirect.addFlashAttribute("error", "Enter a valid member, amount, and reason.");
            return redirect(paymentForm.getMemberId());
        }
        try {
            fines.recordWaiver(paymentForm.getMemberId(), paymentForm.getAmount(), paymentForm.getDescription(), currentUsers.requireCurrentUser());
            redirect.addFlashAttribute("success", "Waiver recorded.");
        } catch (BusinessRuleException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return redirect(paymentForm.getMemberId());
    }

    private String redirect(Long memberId) {
        return "redirect:/staff/fines" + (memberId == null ? "" : "?memberId=" + memberId);
    }
}
