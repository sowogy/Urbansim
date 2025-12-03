package com.example.UrbanismWebSite.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ArticleDTO {
    private Long id;
    private Long member_id;
    private String name;
    private String email;
    private String title;
    private String description;
    private Date created;
    private Date updated;

    // === 파일 관련 필드 ===
    /** 사용자가 입력한 파일 제목 */
    private String fileTitle;

    /** 웹에서 접근 가능한 파일 URL (/uploads/...) */
    private String fileUrl;

    /** 서버에 저장된 실제 파일명(UUID.ext) */
    private String fileStoredName;

    /** 파일 MIME 타입 (예: application/pdf) */
    private String fileContentType;

    /** 파일 크기(byte 단위) */
    private Long fileSize;
}
