package com.example.UrbanismWebSite.controller;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.example.UrbanismWebSite.dto.ArticleDTO;
import com.example.UrbanismWebSite.dto.ArticleForm;
import com.example.UrbanismWebSite.dto.NoticeDTO;
import com.example.UrbanismWebSite.dto.NoticeForm;
import com.example.UrbanismWebSite.model.MemberUserDetails;
import com.example.UrbanismWebSite.service.ArticleService;
import com.example.UrbanismWebSite.service.NoticeService;
import com.example.UrbanismWebSite.service.S3FileStoreService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/notice")
@RequiredArgsConstructor
@Slf4j
public class NoticeController {
    private final NoticeService noticeService;
    private final S3FileStoreService s3FileStoreService;

    @GetMapping("/list")
    public String getList(@PageableDefault(size = 10, sort="id", direction = Sort.Direction.DESC)
                          Pageable pageable, Model model){
        Page<NoticeDTO> page = noticeService.findAll(pageable);
        model.addAttribute("page", page);
        return "notice-list";
    }

    @GetMapping("/content")
    public String getContent(@RequestParam("id") Long id, Model model){
        model.addAttribute("notice", noticeService.findById(id));
        return "notice-content";
    }

    @GetMapping("/add")
    public String getAdd(@ModelAttribute("notice") NoticeForm noticeForm){
        noticeForm.setDescription("공지사항 입력");
        return "notice-add";
    }

    @PostMapping("/add")
    public String postAdd(@Valid @ModelAttribute("notice") NoticeForm noticeForm,
                          BindingResult bindingResult,
                          @AuthenticationPrincipal MemberUserDetails memberUserDetails){
        if(bindingResult != null && noticeForm.getTitle().contains("tlqkf")){
            bindingResult.rejectValue("title", "SlangDetected", "욕설이 탐지되었습니다.");
        }
        if(bindingResult != null && noticeForm.getDescription().contains("tlqkf")){
            bindingResult.rejectValue("description", "SlangDetected", "욕설이 탐지되었습니다.");
        }
        MultipartFile file = noticeForm.getFile();
        // === 파일 크기 20MB 초과 검증 추가 ===
        if (file != null && !file.isEmpty() && file.getSize() > (20L * 1024 * 1024)) {
            bindingResult.rejectValue("file", "FileTooLarge", "파일 크기가 20MB를 넘습니다");
        }
        if(bindingResult.hasErrors()){
            return "notice-add";
        }
        noticeService.create(memberUserDetails.getMemberId(), noticeForm);
        return "redirect:/notice/list";
    }

    @GetMapping("/edit")
    public String getEdit(@RequestParam("id") Long id,
                          @ModelAttribute("notice") NoticeForm noticeForm,
                          Model model){
        NoticeDTO noticeDTO = noticeService.findById(id);
        noticeForm.setId(noticeDTO.getId());
        noticeForm.setDescription(noticeDTO.getDescription());
        noticeForm.setTitle(noticeDTO.getTitle());

        model.addAttribute("noticeDTO", noticeDTO);
        return "notice-edit";
    }

    @PostMapping("/edit")
    public String postEdit(@Valid @ModelAttribute("notice")
                           NoticeForm noticeForm,
                           BindingResult bindingResult
    ){
        NoticeDTO noticeDTO = noticeService.findById(noticeForm.getId());
        MultipartFile file = noticeForm.getFile();
        // === 파일 크기 20MB 초과 검증 추가 ===
        if (file != null && !file.isEmpty() && file.getSize() > (20L * 1024 * 1024)) {
            bindingResult.rejectValue("file", "FileTooLarge", "파일 크기가 20MB를 넘습니다");
        }
        if(bindingResult.hasErrors()){
            return "notice-edit";
        }

        //파일이 변경됐거나 기존 파일을 삭제하는 경우
        if(noticeForm.getRemoveFile() || file != null){
            String path = s3FileStoreService.findPath(noticeDTO.getFileUrl());
            s3FileStoreService.deleteFile(path);
        }
        noticeService.update(noticeForm);
        return "redirect:/notice/content?id=" + noticeForm.getId();
    }

    @GetMapping("/delete")
    public String getDelete(@RequestParam("id") Long id){
        NoticeDTO noticeDTO = noticeService.findById(id);
        noticeService.delete(id);
        String path = s3FileStoreService.findPath(noticeDTO.getFileUrl());
        s3FileStoreService.deleteFile(path);
        return "redirect:/notice/list";
    }

    @GetMapping("/download")
    public ResponseEntity<?> getDownload(@RequestParam("id") Long id,
                                                           HttpServletRequest request) {
        HttpSession session = request.getSession();
        Integer count = (Integer) session.getAttribute("noticeFileDownloadCount");
        if (count == null) count = 0;

        // 5회 초과제한
        if (count >= 5) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "현재 세션에서 다운로드 가능한 횟수를 초과했습니다.");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        NoticeDTO noticeDTO = noticeService.findById(id);

        // S3 서비스에서 S3Object를 직접 받아오는 것이 더 효율적입니다.
        // 서비스 메서드는 fileKey(URL 아님)를 받도록 수정하는 것을 권장합니다.
        String path = s3FileStoreService.findPath(noticeDTO.getFileUrl()); //파일 URL에서 파일 경로만 추출
        S3Object s3Object = s3FileStoreService.downloadFile(path);
        S3ObjectInputStream s3ObjectInputStream = s3Object.getObjectContent();
        ObjectMetadata metadata = s3Object.getObjectMetadata();

        // HTTP 헤더 생성
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(metadata.getContentType()));
        headers.setContentLength(metadata.getContentLength());

        String originalFileName = noticeDTO.getFileTitle();
        String encodedFileName = URLEncoder.encode(originalFileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        headers.setContentDispositionFormData("attachment", encodedFileName);

        // InputStreamResource 생성
        InputStreamResource inputStreamResource = new InputStreamResource(s3ObjectInputStream);

        // 다운로드 카운트 증가
        session.setAttribute("noticeFileDownloadCount", count + 1);

        return ResponseEntity.ok()
                .headers(headers)
                .body(inputStreamResource); //실제 리소스를 body에 담아서 반환
    }
}

