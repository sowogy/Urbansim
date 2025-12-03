package com.example.UrbanismWebSite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordForm {
    @NotBlank(message = "기존 패스워드를 입력하세요")
    private String old;
    @Size(min=8, max=16, message = "특수문자를 포함한 8자 이상의 영문으로 입력하세요.")
    @NotBlank(message = "새로운 패스워드를 입력하세요")
    @Pattern(
            regexp = "^(?=.*[!@#$%^&*])[a-zA-Z!@#$%^&*]{8,16}$", //영문자로 이루어지되 최소 특수문자 하나 이상을 포함하도록함
            message = "8자 "
    )
    private String new_password;
    @Size(min=8, message="8글자 이상 입력하세요")
    @NotBlank(message = "새로 입력한 패스워드를 확인하세요")
    private String new_passwordConfirm;
}
