package com.example.UrbanismWebSite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
//소셜 로그인의 사용자의 상세 정보 저장을 위한 Form
public class SocialMemberForm {
    private Long id;
    @NotBlank(message = "이름을 입력하세요")
    private String name;
    @NotBlank(message = "전화번호를 입력하세요")
    @Pattern(
            regexp = "^010[0-9]{8}$",
            message = "입력 형식에 맞춰 입력해주세요 (예: 01012345678)"
    )
    private String phone;
    @NotBlank(message = "인증 코드를 입력해주세요.")
    private String authenticate_code;
}
