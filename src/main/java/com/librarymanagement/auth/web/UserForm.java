package com.librarymanagement.auth.web;

import com.librarymanagement.auth.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserForm {
    @NotBlank
    @Size(max = 120)
    private String displayName;

    @NotBlank
    @Email
    @Size(max = 190)
    private String email;

    @NotNull
    private UserRole role = UserRole.LIBRARIAN;

    @NotBlank
    @Size(min = 10, max = 72)
    private String temporaryPassword;
}
