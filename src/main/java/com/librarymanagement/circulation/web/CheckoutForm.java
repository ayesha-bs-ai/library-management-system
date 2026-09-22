package com.librarymanagement.circulation.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutForm {
    @NotBlank
    @Size(max = 60)
    private String memberNumber;

    @NotBlank
    @Size(max = 60)
    private String barcode;
}
