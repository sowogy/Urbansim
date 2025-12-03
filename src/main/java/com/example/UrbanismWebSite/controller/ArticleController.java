package com.example.UrbanismWebSite.controller;

import com.amazonaws.Response;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.example.UrbanismWebSite.dto.ArticleDTO;
import com.example.UrbanismWebSite.dto.ArticleForm;
import com.example.UrbanismWebSite.model.Article;
import com.example.UrbanismWebSite.model.MemberUserDetails;
import com.example.UrbanismWebSite.service.ArticleService;
import com.example.UrbanismWebSite.service.S3FileStoreService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
* TODO
 * 컨트롤러 계층에 구현한 모든 로직을 서비스 계층으로 옮기기
 * 컨트롤러 계층은 서비스를 호출만할 뿐 로직 구현은 X
 * 서비스 계층에서 로직 실행 후 예외 발생 시 예외를 던짐
 * 컨트롤러 계층에서는 서비스 계층에서 던진 예외를 받아 처리
* */


@Controller
@RequestMapping("/article")
@RequiredArgsConstructor
@Slf4j
public class ArticleController {
    private final ArticleService articleService;
    private final S3FileStoreService s3FileStoreService;

    @GetMapping("/project")
    public String getProjectList(){
        return "project-list";
    }

    @GetMapping("/list")
    public String getList(@PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC)
                          Pageable pageable, Model model) {
        Page<ArticleDTO> page = articleService.findAll(pageable);
        model.addAttribute("page", page);
        return "article-list";
    }

    @GetMapping("/content")
    public String getContent(@RequestParam("id") Long id, Model model) {
        model.addAttribute("article", articleService.findById(id));
        return "article-content";
    }

    @GetMapping("/add")
    public String getAdd(@ModelAttribute("article") ArticleForm articleForm) {
        return "article-add";
    }

    @PostMapping("/add")
    public String postAdd(@Valid @ModelAttribute("article") ArticleForm articleForm,
                          BindingResult bindingResult,
                          @AuthenticationPrincipal MemberUserDetails memberUserDetails) {
        if (bindingResult != null && articleForm.getDescription().length() > 1000){
            bindingResult.rejectValue("description", "Size", "최대 1000자까지만 작성 가능합니다.");
        }
        MultipartFile file = articleForm.getFile();
        // === 파일 크기 20MB 초과 검증 추가 ===
        if (file != null && !file.isEmpty() && file.getSize() > (20L * 1024 * 1024)) {
            bindingResult.rejectValue("file", "FileTooLarge", "파일 크기가 20MB를 넘습니다");
        }
        if (bindingResult.hasErrors()) {
            return "article-add";
        }
        articleService.create(memberUserDetails.getMemberId(), articleForm);
        return "redirect:/article/list";
    }

    @GetMapping("/edit")
    public String getEdit(@RequestParam("id") Long id,
                          @ModelAttribute("article") ArticleForm articleForm,
                          Model model) {
        ArticleDTO articleDTO = articleService.findById(id);
        articleForm.setId(articleDTO.getId());
        articleForm.setDescription(articleDTO.getDescription());
        articleForm.setTitle(articleDTO.getTitle());

        model.addAttribute("articleDTO", articleDTO);
        return "article-edit";
    }

    @PostMapping("/edit")
    public String postEdit(@Valid @ModelAttribute("article")
                           ArticleForm articleForm,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes
    ) {
            //삭제할 파일의 URL 조회
            MultipartFile file = articleForm.getFile();
            //파일 크기 20MB 초과 시
            if (file != null && !file.isEmpty() && file.getSize() > (20L * 1024 * 1024)) {
                bindingResult.rejectValue("file", "FileTooLarge", "파일 크기가 20MB를 넘습니다");
            }
            if (bindingResult.hasErrors()) {
                return "redirect:/article/content?id=" + articleForm.getId();
            }
            articleService.update(articleForm);
            redirectAttributes.addFlashAttribute("updateSuccess", Boolean.TRUE);
            return "redirect:/article/content?id=" + articleForm.getId();
    }

    @GetMapping("/delete")
    @Transactional(rollbackOn = {AmazonS3Exception.class})
    public String getDelete(@RequestParam("id") Long id,
                            RedirectAttributes redirectAttributes) {
            articleService.delete(id);
            redirectAttributes.addFlashAttribute("deleteSuccess", Boolean.TRUE);
            return "redirect:/article/list";
    }

    /** 로직을 서비스 계층으로 옮기고 컨트롤러에서는 서비스 호출만 하도록 변경 */
    @GetMapping("/download")
    public ResponseEntity<?> getDownload(@RequestParam("id") Long id,
                                         HttpServletRequest request) {
        HttpSession session = request.getSession();
        Integer count = (Integer) session.getAttribute("articleFileDownloadCount");
        if (count == null) count = 0;

        // 5회 초과제한
        if (count >= 5) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "현재 세션에서 다운로드 가능한 횟수를 초과했습니다.");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        ArticleDTO articleDTO = articleService.findById(id);

        // S3 에서 객체 로드
        String path = s3FileStoreService.findPath(articleDTO.getFileUrl());
        S3Object s3Object = s3FileStoreService.downloadFile(path);
        S3ObjectInputStream s3is = s3Object.getObjectContent();
        ObjectMetadata metadata = s3Object.getObjectMetadata();

        // 헤더 구성
        HttpHeaders headers = new HttpHeaders();

        // Content-Type이 비어있는 경우
        String contentType = metadata.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }
        headers.setContentType(MediaType.parseMediaType(contentType));

        if (metadata.getContentLength() > 0) {
            headers.setContentLength(metadata.getContentLength());
        }

        // 파일명: 원본명 사용 (확장자 포함)
        String originalFileName = articleDTO.getFileTitle();

        //첨부 다운로드 헤더
        headers.setContentDisposition(
                ContentDisposition.builder("attachment")
                        .filename(originalFileName, StandardCharsets.UTF_8) // RFC5987
                        .build()
        );

        // InputStream을 Response로 전달
        InputStreamResource body = new InputStreamResource(s3is);

        // 다운로드 카운트 증가
        session.setAttribute("articleFileDownloadCount", count + 1);

        return ResponseEntity.ok()
                .headers(headers)
                .body(body);

        // (선택) 더 안전한 방식:
        // return ResponseEntity.ok()
        //         .headers(headers)
        //         .body((StreamingResponseBody) out -> {
        //             try (S3ObjectInputStream in = s3is; S3Object ignored = s3Object) {
        //                 in.transferTo(out);
        //             }
        //         });
    }

}

