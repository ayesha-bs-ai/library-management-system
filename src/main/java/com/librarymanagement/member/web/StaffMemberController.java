package com.librarymanagement.member.web;

import com.librarymanagement.circulation.CirculationService;
import com.librarymanagement.common.exception.BusinessRuleException;
import com.librarymanagement.fine.FineService;
import com.librarymanagement.member.Member;
import com.librarymanagement.member.MemberService;
import com.librarymanagement.member.MemberStatus;
import com.librarymanagement.member.MemberType;
import com.librarymanagement.reservation.ReservationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/staff/members")
public class StaffMemberController {
    private final MemberService members;
    private final CirculationService circulation;
    private final ReservationService reservations;
    private final FineService fines;

    public StaffMemberController(MemberService members, CirculationService circulation,
                                 ReservationService reservations, FineService fines) {
        this.members = members;
        this.circulation = circulation;
        this.reservations = reservations;
        this.fines = fines;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        Page<Member> result = members.search(q, page);
        model.addAttribute("result", result);
        model.addAttribute("members", result.getContent());
        model.addAttribute("q", q);
        model.addAttribute("title", "Members");
        return "staff/members/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("memberForm", new MemberForm());
        addFormModel(model, false, "Register member");
        return "staff/members/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute MemberForm memberForm, BindingResult binding,
                         Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) return formView(model, false, "Register member");
        try {
            Member member = members.create(memberForm);
            redirect.addFlashAttribute("success", "Member registered successfully.");
            return "redirect:/staff/members/" + member.getId();
        } catch (BusinessRuleException ex) {
            binding.reject("member", ex.getMessage());
            return formView(model, false, "Register member");
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Member member = members.get(id);
        model.addAttribute("member", member);
        model.addAttribute("loans", circulation.memberLoans(id));
        model.addAttribute("reservations", reservations.forMember(id));
        model.addAttribute("balance", fines.balance(id));
        model.addAttribute("ledger", fines.ledger(id));
        model.addAttribute("title", member.getFullName());
        return "staff/members/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("memberForm", MemberForm.from(members.get(id)));
        model.addAttribute("memberId", id);
        addFormModel(model, true, "Edit member");
        return "staff/members/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute MemberForm memberForm,
                         BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) return editView(id, model);
        try {
            members.update(id, memberForm);
            redirect.addFlashAttribute("success", "Member updated.");
            return "redirect:/staff/members/" + id;
        } catch (BusinessRuleException ex) {
            binding.reject("member", ex.getMessage());
            return editView(id, model);
        }
    }

    private String formView(Model model, boolean editing, String title) {
        addFormModel(model, editing, title);
        return "staff/members/form";
    }

    private String editView(Long id, Model model) {
        model.addAttribute("memberId", id);
        return formView(model, true, "Edit member");
    }

    private void addFormModel(Model model, boolean editing, String title) {
        model.addAttribute("memberTypes", MemberType.values());
        model.addAttribute("memberStatuses", MemberStatus.values());
        model.addAttribute("editing", editing);
        model.addAttribute("title", title);
    }
}
