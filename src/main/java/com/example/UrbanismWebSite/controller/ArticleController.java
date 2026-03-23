package com.example.UrbanismWebSite.controller;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.example.UrbanismWebSite.dto.ArticleDTO;
import com.example.UrbanismWebSite.dto.ArticleForm;
import com.example.UrbanismWebSite.dto.DateSelectionForm;
import com.example.UrbanismWebSite.exception.BusinessException;
import com.example.UrbanismWebSite.model.Article;
import com.example.UrbanismWebSite.model.MemberUserDetails;
import com.example.UrbanismWebSite.service.ArticleService;
import com.example.UrbanismWebSite.service.S3FileStoreService;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
@Controller
@RequestMapping("/article")
@RequiredArgsConstructor
@Slf4j
public class ArticleController {
    private final ArticleService articleService;
    private final S3FileStoreService s3FileStoreService;

    @GetMapping("/project")
    public String getProjectList() {
        return "project-list";
    }

    //최근 프로젝트 목록들을 조회
    @GetMapping("/list/cur")
    public String getCurList(@PageableDefault(size = 6, sort = "id", direction = Sort.Direction.DESC)
                             Pageable pageable, Model model) {
        Page<ArticleDTO> page = articleService.findByCurrentDate(pageable);
        model.addAttribute("page", page);
        return "article-list-current";
    }

    //과거 게시글들을 조회하기 위한 날짜 선택
    @GetMapping("/list/past")
    public String getSelectDate(@ModelAttribute DateSelectionForm dateSelectionForm,
                                @PageableDefault(size = 6, sort = "id", direction = Sort.Direction.DESC)
                                Pageable pageable, Model model) {
        int year = 0, semester = 0;
        if (dateSelectionForm.getSelectedYear() != null) {
            year = Integer.parseInt(dateSelectionForm.getSelectedYear());
        }
        if (dateSelectionForm.getSelectedSemester() != null) {
            String date = dateSelectionForm.getSelectedSemester();
            semester = Integer.parseInt(date.substring(0, 1));
        }
        Page<ArticleDTO> page = articleService.findByPastDate(pageable, year, semester);
        model.addAttribute("page", page);
        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedSemester", semester);
        return "article-list-past";
    }

    //과거 게시글 조회 선택 시 날짜 선택뷰를 반환
    @GetMapping("/list/select-past-date")
    public String postPastDateInfo() {
        return "select-project-date";
    }

    @GetMapping("/content")
    public String getContent(@RequestParam("id") Long id, Model model) {
        ArticleDTO articleDTO = articleService.findById(id);
        if(articleDTO == null){
            throw new BusinessException(HttpStatus.NO_CONTENT, "해당 프로젝트를 찾을 수 없습니다.");
        }

        model.addAttribute("article", articleDTO);

        return "article-content";
    }


    @GetMapping("/add")
    public String getAdd(@ModelAttribute("article") ArticleForm articleForm,
                         @RequestParam(value = "isCurrent", required = false) boolean isCurrent,
                         @RequestParam(value = "selectedYear", required = false) String year,
                         @RequestParam(value = "selectedSemester", required = false) String semester,
                         Model model) {
        articleForm.setIsCurrent(isCurrent);
        if(!isCurrent){
            model.addAttribute("selectedYear", year);
            model.addAttribute("selectedSemester", semester);
        }
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
        return "redirect:/article/list/cur";
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

    @PostMapping("/delete")
    public String articleDelete(@RequestParam("id") Long id,
                                RedirectAttributes redirectAttributes,
                                ServletResponse response) {
        try{
            articleService.delete(id);
            redirectAttributes.addFlashAttribute("deleteSuccess", Boolean.TRUE);
            return "redirect:/article/project";
        } catch(Exception e){
            response.setContentType("text/html; charset=UTF-8");
            String url = "/article/project";
            try {
                PrintWriter out = response.getWriter();
                out.println("<script>");
                out.println("alert('파일 삭제 도중 에러가 발생했습니다.')");
                out.println("location.href = '" + url + "';");
                out.println("</script>");
                out.flush();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return null;
    }

    /** 파일 다운로드 */
    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> getDownload(@RequestParam("id") Long id,
                                         HttpServletRequest request,
                                         @AuthenticationPrincipal Principal principal) {
            // 1. 사용자 구분 (비회원 vs 회원)에 따라 설정값 정하기
            String sessionKey;
            int downloadLimit;

            if (principal == null) {
                // 비회원
                sessionKey = "GUEST_FILE_DOWNLOAD_COUNT";
                downloadLimit = 5;
            } else {
                // 회원
                sessionKey = "articleFileDownloadCount"; // 키 이름 통일성 있게 수정 추천 (예: USER_FILE_DOWNLOAD_COUNT)
                downloadLimit = 10;
            }

            HttpSession session = request.getSession(true); // 없으면 생성
            Integer count = (Integer) session.getAttribute(sessionKey);

            if (count == null) {
                count = 0;
            }

            if (count >= downloadLimit) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "현재 세션에서 다운로드 가능한 횟수를 초과했습니다.");
            }

            session.setAttribute(sessionKey, count + 1);

            ArticleDTO articleDTO = articleService.findById(id);
            return s3FileStoreService.fileDownload(articleDTO);
    }
}

