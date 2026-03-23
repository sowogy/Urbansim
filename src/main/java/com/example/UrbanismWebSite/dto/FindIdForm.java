package com.example.UrbanismWebSite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FindIdForm {
    @NotBlank(message = "이름을 입력하세요")
    private String name;
    @NotBlank(message = "전화번호를 입력하세요 예)00012345678")
    @Pattern(
            regexp = "^010[0-9]{8}$",
            message = "입력 형식에 맞춰 입력해주세요 (예: 01012345678)"
    )
    private String phone;
}
