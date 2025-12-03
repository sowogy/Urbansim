package com.example.UrbanismWebSite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleForm {
    private Long id;
    @NotBlank(message = "게시글의 제목을 입력하세요")
    private String title;
    @NotBlank(message = "게시글의 내용을 입력하세요")
    @Size(max=1000, message="1000자까지 입력 가능")
    private String description;

    //파일 저장
    private MultipartFile file;

    //파일 삭제 여부 저장
    private Boolean removeFile;
}
