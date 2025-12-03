package com.example.UrbanismWebSite.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MailForm {
    @NotBlank(message = "오류 내용을 입력하세요")
    private String description;
}
