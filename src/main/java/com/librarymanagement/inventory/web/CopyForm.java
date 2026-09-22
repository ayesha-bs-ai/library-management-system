package com.librarymanagement.inventory.web;

import com.librarymanagement.inventory.CopyCondition;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class CopyForm {
    @NotBlank
    @Size(max = 60)
    private String barcode;

    @NotBlank
    @Size(max = 60)
    private String accessionNumber;

    @NotNull
    private Long branchId;

    @Size(max = 80)
    private String shelfLocation;

    @NotNull
    private CopyCondition condition = CopyCondition.GOOD;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate acquiredOn = LocalDate.now();

    @DecimalMin(value = "0.00")
    private BigDecimal purchasePrice;
}
