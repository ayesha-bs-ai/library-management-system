package com.librarymanagement.auth.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileForm {
    @NotBlank(message = "Display name is required")
    @Size(max = 120, message = "Display name too long")
    private String displayName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email")
    @Size(max = 190)
    private String email;

    public static ProfileForm from(com.librarymanagement.auth.UserAccount user) {
        ProfileForm form = new ProfileForm();
        form.setDisplayName(user.getDisplayName());
        form.setEmail(user.getEmail());
        return form;
    }
}
