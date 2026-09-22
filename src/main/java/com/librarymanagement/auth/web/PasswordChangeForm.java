package com.librarymanagement.auth.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordChangeForm {
    @NotBlank
    private String currentPassword;

    @NotBlank
    @Size(min = 12, max = 72)
    private String newPassword;

    @NotBlank
    private String confirmPassword;
}
