package com.librarymanagement.catalog.web;

import com.librarymanagement.catalog.Book;
import com.librarymanagement.catalog.CatalogService;
import com.librarymanagement.common.exception.BusinessRuleException;
import com.librarymanagement.inventory.CopyCondition;
import com.librarymanagement.inventory.web.CopyForm;
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
@RequestMapping("/staff/books")
public class StaffCatalogController {
    private final CatalogService catalog;

    public StaffCatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        Page<Book> result = catalog.search(q, page, 20);
        model.addAttribute("result", result);
        model.addAttribute("books", result.getContent());
        model.addAttribute("availability", catalog.availabilityFor(result.getContent()));
        model.addAttribute("q", q);
        model.addAttribute("title", "Books and inventory");
        return "staff/books/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("bookForm", new BookForm());
        model.addAttribute("title", "Add a book");
        model.addAttribute("editing", false);
        return "staff/books/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute BookForm bookForm, BindingResult binding,
                         Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) return formView(model, false, "Add a book");
        try {
            Book book = catalog.create(bookForm);
            redirect.addFlashAttribute("success", "Book added to the catalog.");
            return "redirect:/staff/books/" + book.getId();
        } catch (BusinessRuleException ex) {
            binding.reject("book", ex.getMessage());
            return formView(model, false, "Add a book");
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        addDetailModel(id, model, new CopyForm());
        return "staff/books/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("bookForm", BookForm.from(catalog.get(id)));
        model.addAttribute("bookId", id);
        model.addAttribute("editing", true);
        model.addAttribute("title", "Edit book");
        return "staff/books/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute BookForm bookForm,
                         BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) return editView(id, model);
        try {
            catalog.update(id, bookForm);
            redirect.addFlashAttribute("success", "Book details updated.");
            return "redirect:/staff/books/" + id;
        } catch (BusinessRuleException ex) {
            binding.reject("book", ex.getMessage());
            return editView(id, model);
        }
    }

    @PostMapping("/{id}/copies")
    public String addCopy(@PathVariable Long id, @Valid @ModelAttribute CopyForm copyForm,
                          BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            addDetailModel(id, model, copyForm);
            return "staff/books/detail";
        }
        try {
            catalog.addCopy(id, copyForm);
            redirect.addFlashAttribute("success", "Physical copy added.");
            return "redirect:/staff/books/" + id;
        } catch (BusinessRuleException ex) {
            binding.reject("copy", ex.getMessage());
            addDetailModel(id, model, copyForm);
            return "staff/books/detail";
        }
    }

    @PostMapping("/{id}/archive")
    public String archive(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            catalog.archive(id);
            redirect.addFlashAttribute("success", "Book archived.");
            return "redirect:/staff/books";
        } catch (BusinessRuleException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return "redirect:/staff/books/" + id;
        }
    }

    private String formView(Model model, boolean editing, String title) {
        model.addAttribute("editing", editing);
        model.addAttribute("title", title);
        return "staff/books/form";
    }

    private String editView(Long id, Model model) {
        model.addAttribute("bookId", id);
        return formView(model, true, "Edit book");
    }

    private void addDetailModel(Long id, Model model, CopyForm form) {
        Book book = catalog.get(id);
        model.addAttribute("book", book);
        model.addAttribute("copies", catalog.copiesFor(id));
        model.addAttribute("branches", catalog.activeBranches());
        model.addAttribute("conditions", CopyCondition.values());
        model.addAttribute("copyForm", form);
        model.addAttribute("title", book.getTitle());
    }
}
