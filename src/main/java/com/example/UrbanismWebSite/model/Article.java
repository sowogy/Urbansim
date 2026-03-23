package com.example.UrbanismWebSite.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.cglib.core.Local;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    @LastModifiedDate
    private Date updated;
    @CreatedDate
    private Date created;
    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    /** 사용자가 붙인 '파일 제목' (게시글 내에서 보일 표시용 제목) */
    private String fileTitle;

    /** 저장된 파일의 공개 URL (/uploads/...) */
    private String fileUrl;

    /** 저장 파일명(UUID.ext) — 필요하면 관리자용 조회에 유용 */
    private String fileStoredName;

    /** MIME 타입 (예: application/pdf) */
    private String fileContentType;

    /** 파일 크기(byte) */
    private Long fileSize;
}

