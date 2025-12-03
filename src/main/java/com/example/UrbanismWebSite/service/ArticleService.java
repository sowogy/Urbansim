package com.example.UrbanismWebSite.service;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.example.UrbanismWebSite.dto.ArticleDTO;
import com.example.UrbanismWebSite.dto.ArticleForm;
import com.example.UrbanismWebSite.model.Article;
import com.example.UrbanismWebSite.model.Member;
import com.example.UrbanismWebSite.repository.ArticleRepository;
import com.example.UrbanismWebSite.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final MemberRepository memberRepository;
    private final S3FileStoreService fileStoreService;

    @PersistenceContext
    private EntityManager em;

    private ArticleDTO mapTOArticleDTO(Article article){
        return ArticleDTO.builder()
                .id(article.getId())
                .title(article.getTitle())
                .description(article.getDescription())
                .created(article.getCreated())
                .updated(article.getUpdated())
                .member_id(article.getMember().getId())
                .name(article.getMember().getName())
                .email(article.getMember().getEmail())
                .fileTitle(article.getFileTitle())
                .fileUrl(article.getFileUrl())
                .fileStoredName(article.getFileStoredName())
                .fileContentType(article.getFileContentType())
                .fileSize(article.getFileSize())
                .build();
    }

    public ArticleDTO create(Long memberId, ArticleForm articleForm){
        Member member = memberRepository.findById(memberId).orElseThrow();

        //파일이 없는 경우
        Article article = Article.builder()
                .title(articleForm.getTitle())
                .description(articleForm.getDescription())
                .member(member)
                .build();

        // 파일 처리 (단일 파일만)
        MultipartFile file = articleForm.getFile();
        if (file != null && !file.isEmpty()) {
            try {
                // FileStorageService는 여러 파일도 처리 가능하도록 1개 리스트로 전달
                var storedList = fileStoreService.storeAll(List.of(file), "article");
                var f = storedList.get(0);

                // Article 엔티티에 메타데이터 저장
                article.setFileTitle(file.getOriginalFilename()); // 원본 파일명을 제목으로 사용
                article.setFileUrl(f.publicUrl());                // 파일이 S3 버킷에 저장된 경로
                article.setFileStoredName(f.storedName());        // UUID.ext
                article.setFileContentType(f.contentType());      // MIME 타입
                article.setFileSize(f.size());                    // 파일 크기(byte)

            } catch (Exception e) {
                throw new RuntimeException("파일 저장 중 오류가 발생했습니다.", e);
            }
        }
        articleRepository.save(article);
        return mapTOArticleDTO(article);
    }

    public Page<ArticleDTO> findAll(Pageable pageable){
        return articleRepository.findAll(pageable).map(this::mapTOArticleDTO);
    }

    public ArticleDTO findById(Long id){
        return articleRepository.findById(id).map(this::mapTOArticleDTO).orElseThrow();
    }

    @Transactional(rollbackFor = Exception.class)
    public ArticleDTO update(ArticleForm articleForm) {
        Article article = articleRepository.findById(articleForm.getId()).orElseThrow();

        // 기본 필드 갱신
        article.setTitle(articleForm.getTitle());
        article.setDescription(articleForm.getDescription());

        // 파일 삭제/교체/유지 분기
        boolean remove = Boolean.TRUE.equals(articleForm.getRemoveFile());
        MultipartFile newFile = articleForm.getFile();

        String oldStored = article.getFileStoredName();

        try{
            if (remove) {
                // 1) 삭제: 메타데이터 초기화
                article.setFileTitle(null);
                article.setFileUrl(null);
                article.setFileStoredName(null);
                article.setFileContentType(null);
                article.setFileSize(null);

                //DB 실핼 중 에러 발생 여부 확인
                articleRepository.save(article);
                em.flush();

                fileStoreService.deleteFile(oldStored); //S3 버킷에서 파일 삭제
            } else if (newFile != null && !newFile.isEmpty()) {
                // 2) 교체: 새 파일 저장 후 메타데이터 교체
                var storedList = fileStoreService.storeAll(List.of(newFile), "article");
                var f = storedList.get(0);

                article.setFileTitle(newFile.getOriginalFilename()); // 표시용: 원본 파일명
                article.setFileUrl(f.publicUrl());                   // /uploads/...
                article.setFileStoredName(f.storedName());           // UUID.ext
                article.setFileContentType(f.contentType());         // MIME
                article.setFileSize(f.size());                       // bytes

                //DB 실핼 중 에러 발생 여부 확인
                articleRepository.save(article);
                em.flush();

                fileStoreService.deleteFile(oldStored); //기존 파일을 삭제함
            }
        } catch(Exception e){
            throw new RuntimeException("업데이트 도중 오류가 발생했습니다.", e);
        }

        // 3) 둘 다 아니면: 그대로 유지 (메타데이터 변경 없음)
        articleRepository.save(article);
        return mapTOArticleDTO(article);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        try{
            Article article = articleRepository.findById(id).orElseThrow();
            articleRepository.delete(article);
            fileStoreService.deleteFile(article.getFileStoredName());
        } catch(Exception e){
            throw new AmazonS3Exception("s3 파일 삭제 실패", e);
        }
    }

}
