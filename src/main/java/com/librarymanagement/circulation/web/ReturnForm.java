package com.librarymanagement.circulation.web;

import com.librarymanagement.inventory.CopyCondition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReturnForm {
    @NotBlank
    @Size(max = 60)
    private String barcode;

    @NotNull
    private CopyCondition condition = CopyCondition.GOOD;
}
