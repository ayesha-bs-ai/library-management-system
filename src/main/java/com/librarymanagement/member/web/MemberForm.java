package com.librarymanagement.member.web;

import com.librarymanagement.member.Member;
import com.librarymanagement.member.MemberStatus;
import com.librarymanagement.member.MemberType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class MemberForm {
    @NotBlank
    @Size(max = 40)
    private String membershipNumber;

    @NotBlank
    @Size(max = 150)
    private String fullName;

    @NotBlank
    @Email
    @Size(max = 190)
    private String email;

    @Size(max = 40)
    private String phone;

    @Size(max = 500)
    private String address;

    @NotNull
    private MemberType type = MemberType.GENERAL;

    @NotNull
    private MemberStatus status = MemberStatus.ACTIVE;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate joinedOn = LocalDate.now();

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiresOn = LocalDate.now().plusYears(1);

    private boolean createPortalAccount = true;

    @Size(max = 72)
    private String temporaryPassword;

    public static MemberForm from(Member member) {
        MemberForm form = new MemberForm();
        form.setMembershipNumber(member.getMembershipNumber());
        form.setFullName(member.getFullName());
        form.setEmail(member.getEmail());
        form.setPhone(member.getPhone());
        form.setAddress(member.getAddress());
        form.setType(member.getType());
        form.setStatus(member.getStatus());
        form.setJoinedOn(member.getJoinedOn());
        form.setExpiresOn(member.getExpiresOn());
        form.setCreatePortalAccount(member.getUserAccount() != null);
        return form;
    }
}
