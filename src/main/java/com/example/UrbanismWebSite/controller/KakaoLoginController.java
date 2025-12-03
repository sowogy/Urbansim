package com.example.UrbanismWebSite.controller;

import com.example.UrbanismWebSite.dto.KakaoUserInfoDTO;
import com.example.UrbanismWebSite.service.KakaoService;
import com.example.UrbanismWebSite.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Controller
@RequiredArgsConstructor
public class KakaoLoginController {
    private final KakaoService kakaoService;
    private final MemberService memberService;
    private final UserDetailsService userDetailsService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository(); // 추가

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String redirect_uri;

    @GetMapping("/callback")
    public String callback(@RequestParam("code") String code,
                           HttpServletRequest request,
                           HttpServletResponse response,
                           RedirectAttributes redirectAttributes) {
        String accessToken = kakaoService.getAccessTokenFromKakao(code);
        KakaoUserInfoDTO kakaoUserInfoDTO = kakaoService.getUserInfo(accessToken);

        //Member DB에 이메일 정보가 존재하지 않으면 회원가입 페이지로 이동
        String userEmail = kakaoUserInfoDTO.getKakaoAccount().getEmail();
        if (!memberService.isEmail(userEmail)) {
            redirectAttributes.addFlashAttribute("success", Boolean.FALSE);
            return "redirect:/login";
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        // (선택) 요청 정보 디테일 부여
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // SecurityContext 생성 및 설정
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        request.getSession(true);

        // 명시적으로 세션/컨텍스트 저장
        securityContextRepository.saveContext(context, request, response);

        return "redirect:/";
    }

    // 카카오 로그인 진입점
    @GetMapping("/login/kakao")
    public String kakaoLogin() {
        String location = "https://kauth.kakao.com/oauth/authorize"
                + "?response_type=code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + URLEncoder.encode(redirect_uri, StandardCharsets.UTF_8);

        return "redirect:" + location;
    }
}


