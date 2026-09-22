package com.librarymanagement.reporting;

import com.librarymanagement.circulation.CirculationService;
import com.librarymanagement.circulation.Loan;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff/reports")
public class ReportController {
    private final DashboardService dashboard;
    private final CirculationService circulation;

    public ReportController(DashboardService dashboard, CirculationService circulation) {
        this.dashboard = dashboard;
        this.circulation = circulation;
    }

    @GetMapping
    public String reports(Model model) {
        model.addAttribute("snapshot", dashboard.snapshot());
        model.addAttribute("activeLoans", circulation.activeLoans());
        model.addAttribute("title", "Reports");
        return "staff/reports/index";
    }

    @GetMapping(value = "/active-loans.csv", produces = "text/csv")
    public ResponseEntity<byte[]> activeLoansCsv() {
        StringBuilder csv = new StringBuilder("Loan ID,Member Number,Member Name,Barcode,Title,Checkout Time,Due Date,Status\n");
        for (Loan loan : circulation.activeLoans()) {
            csv.append(loan.getId()).append(',')
                    .append(cell(loan.getMember().getMembershipNumber())).append(',')
                    .append(cell(loan.getMember().getFullName())).append(',')
                    .append(cell(loan.getCopy().getBarcode())).append(',')
                    .append(cell(loan.getCopy().getBook().getTitle())).append(',')
                    .append(cell(loan.getCheckedOutAt().toString())).append(',')
                    .append(cell(loan.getDueOn().toString())).append(',')
                    .append(loan.getStatus()).append('\n');
        }
        byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename("active-loans-" + LocalDate.now() + ".csv").build());
        return ResponseEntity.ok().headers(headers).body(body);
    }

    private String cell(String value) {
        if (value == null) return "";
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
