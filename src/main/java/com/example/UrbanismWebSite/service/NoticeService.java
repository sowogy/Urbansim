package com.example.UrbanismWebSite.service;

import com.example.UrbanismWebSite.dto.ArticleDTO;
import com.example.UrbanismWebSite.dto.ArticleForm;
import com.example.UrbanismWebSite.dto.NoticeDTO;
import com.example.UrbanismWebSite.dto.NoticeForm;
import com.example.UrbanismWebSite.model.Article;
import com.example.UrbanismWebSite.model.Member;
import com.example.UrbanismWebSite.model.Notice;
import com.example.UrbanismWebSite.repository.ArticleRepository;
import com.example.UrbanismWebSite.repository.MemberRepository;
import com.example.UrbanismWebSite.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {
    private final NoticeRepository noticeRepository;
    private final MemberRepository memberRepository;
    private final S3FileStoreService fileStoreService;

    private NoticeDTO mapToNoticeDTO(Notice notice){
        return NoticeDTO.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .description(notice.getDescription())
                .created(notice.getCreated())
                .updated(notice.getUpdated())
                .member_id(notice.getMember().getId())
                .name(notice.getMember().getName())
                .email(notice.getMember().getEmail())
                .fileTitle(notice.getFileTitle())
                .fileUrl(notice.getFileUrl())
                .fileStoredName(notice.getFileStoredName())
                .fileContentType(notice.getFileContentType())
                .fileSize(notice.getFileSize())
                .build();
    }

    public NoticeDTO create(Long memberId, NoticeForm noticeForm){
        Member member = memberRepository.findById(memberId).orElseThrow();

        //파일이 없는 경우
        Notice notice = Notice.builder()
                .title(noticeForm.getTitle())
                .description(noticeForm.getDescription())
                .member(member)
                .build();

        // 파일 처리 (단일 파일만)
        MultipartFile file = noticeForm.getFile();
        if (file != null && !file.isEmpty()) {
            try {
                // FileStorageService는 여러 파일도 처리 가능하도록 1개 리스트로 전달
                var storedList = fileStoreService.storeAll(List.of(file), "notice");
                var f = storedList.get(0);

                // Notice 엔티티에 메타데이터 저장
                notice.setFileTitle(file.getOriginalFilename()); // 원본 파일명을 제목으로 사용
                notice.setFileUrl(f.publicUrl());                // /uploads/... 접근 URL
                notice.setFileStoredName(f.storedName());        // UUID.ext
                notice.setFileContentType(f.contentType());      // MIME 타입
                notice.setFileSize(f.size());                    // 파일 크기(byte)

            } catch (Exception e) {
                throw new RuntimeException("파일 저장 중 오류가 발생했습니다.", e);
            }
        }
        noticeRepository.save(notice);
        return mapToNoticeDTO(notice);
    }

    public Page<NoticeDTO> findAll(Pageable pageable){
        return noticeRepository.findAll(pageable).map(this::mapToNoticeDTO);
    }

    public NoticeDTO findById(Long id){
        return noticeRepository.findById(id).map(this::mapToNoticeDTO).orElseThrow();
    }

    public NoticeDTO update(NoticeForm noticeForm) {
        Notice notice = noticeRepository.findById(noticeForm.getId()).orElseThrow();

        // 기본 필드 갱신
        notice.setTitle(noticeForm.getTitle());
        notice.setDescription(noticeForm.getDescription());

        // 파일 삭제/교체/유지 분기
        boolean remove = Boolean.TRUE.equals(noticeForm.getRemoveFile());
        MultipartFile newFile = noticeForm.getFile();

        if (remove) {
            // 1) 삭제: 메타데이터 초기화 (필요하면 디스크 파일도 삭제)
            // String oldStored = article.getFileStoredName();
            notice.setFileTitle(null);
            notice.setFileUrl(null);
            notice.setFileStoredName(null);
            notice.setFileContentType(null);
            notice.setFileSize(null);

            // TODO(선택): 실제 디스크 파일 삭제를 원하면 fileStoredName/URL을 이용해 Files.deleteIfExists(...)
            // try { Files.deleteIfExists(Path.of(...)); } catch (IOException ignore) {}
        } else if (newFile != null && !newFile.isEmpty()) {
            // 2) 교체: 새 파일 저장 후 메타데이터 교체
            try {
                var storedList = fileStoreService.storeAll(List.of(newFile), "notice");
                var f = storedList.get(0);

                notice.setFileTitle(newFile.getOriginalFilename()); // 표시용: 원본 파일명
                notice.setFileUrl(f.publicUrl());                   // /uploads/...
                notice.setFileStoredName(f.storedName());           // UUID.ext
                notice.setFileContentType(f.contentType());         // MIME
                notice.setFileSize(f.size());                       // bytes

                // TODO(선택): 이전 파일 실제 삭제 처리도 가능 (위에서 oldStored 보관 후 삭제)

            } catch (Exception e) {
                throw new RuntimeException("파일 저장 중 오류가 발생했습니다.", e);
            }
        }
        // 3) 둘 다 아니면: 그대로 유지 (메타데이터 변경 없음)

        noticeRepository.save(notice);
        return mapToNoticeDTO(notice);
    }


    public void delete(Long id){
        Notice notice = noticeRepository.findById(id).orElseThrow();
        noticeRepository.delete(notice);
    }
}

