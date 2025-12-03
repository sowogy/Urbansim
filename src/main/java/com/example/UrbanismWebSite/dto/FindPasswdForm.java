package com.example.UrbanismWebSite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class FindPasswdForm {
    @NotBlank(message = "이메일 입력")
    String email;

    @NotBlank(message = "전화번호 입력")
    @Pattern(
            regexp = "^010[0-9]{8}$"
    )
    String phone;
}
