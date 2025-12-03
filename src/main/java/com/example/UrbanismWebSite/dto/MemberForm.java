package com.example.UrbanismWebSite.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberForm {
    private Long id;
    @NotBlank(message = "이름을 입력하세요")
    private String name;
    @NotBlank(message = "이메일을 입력하세요")
    @Email(message = "이메일 형식이 잘못 되었습니다")
    private String email;
    @NotBlank(message = "비밀번호 설정(특수문자를 포함한 8자 이상의 영문으로 입력하세요)")
    @Pattern(
            regexp = "^(?=.*[!@#$%^&*])[a-zA-Z!@#$%^&*]{8,16}$",
            message = "특수문자를 포함한 8자 이상의 영문으로 입력하세요."
    )
    private String passwd;
    @NotBlank(message = "설정한 비밀번호를 입력하세요")
    private String passwdConfirm;
    @NotBlank(message = "전화번호를 입력하세요")
    @Pattern(
            regexp = "^010[0-9]{8}$",
            message = "입력 형식에 맞춰 입력해주세요 (예: 01012345678)"
    )
    private String phone;
}
