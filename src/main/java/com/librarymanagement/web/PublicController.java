package com.librarymanagement.web;

import com.librarymanagement.catalog.Book;
import com.librarymanagement.catalog.CatalogService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PublicController {
    private final CatalogService catalog;

    public PublicController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/")
    public String home(Model model) {
        Page<Book> featured = catalog.search("", 0, 8);
        model.addAttribute("books", featured.getContent());
        model.addAttribute("availability", catalog.availabilityFor(featured.getContent()));
        model.addAttribute("title", "Discover your next book");
        return "public/home";
    }

    @GetMapping("/catalog")
    public String catalog(@RequestParam(defaultValue = "") String q,
                          @RequestParam(defaultValue = "0") int page, Model model) {
        Page<Book> result = catalog.search(q, page, 12);
        model.addAttribute("result", result);
        model.addAttribute("books", result.getContent());
        model.addAttribute("availability", catalog.availabilityFor(result.getContent()));
        model.addAttribute("q", q);
        model.addAttribute("title", q.isBlank() ? "Browse catalog" : "Search results");
        return "public/catalog";
    }

    @GetMapping("/catalog/books/{id}")
    public String book(@PathVariable Long id, Model model) {
        Book book = catalog.get(id);
        model.addAttribute("book", book);
        model.addAttribute("copies", catalog.copiesFor(id));
        model.addAttribute("availableCount", catalog.availableCopies(id));
        model.addAttribute("title", book.getTitle());
        return "public/book-detail";
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) return "redirect:/dashboard";
        return "auth/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        boolean member = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MEMBER"));
        return member ? "redirect:/my/account" : "redirect:/staff/dashboard";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/403";
    }
}
