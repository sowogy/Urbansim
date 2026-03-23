package com.example.UrbanismWebSite.handler;

import com.example.UrbanismWebSite.dto.SocialMemberForm;
import com.example.UrbanismWebSite.model.Authority;
import com.example.UrbanismWebSite.model.Member;
import com.example.UrbanismWebSite.repository.AuthorityRepository;
import com.example.UrbanismWebSite.repository.MemberRepository;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import javax.swing.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final MemberRepository memberRepository;
    private final AuthorityRepository authorityRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();

        //oAuth2User에서 attributes 객체 가져오기
        Map<String, Object> attributes = oAuth2User.getAttributes();

        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");

        // 이메일 추출
        String email = (String) kakaoAccount.get("email");

        Member member = memberRepository.findByEmail(email).orElse(null);
        if(member != null){
            List<Authority> authority = authorityRepository.findByMember(member);
            boolean isGuest = authority.stream()
                    .anyMatch(auth -> "ROLE_GUEST".equals(auth.getAuthority()));
            /** 권한이 ROLE_GUEST일 경우 추가 정보 입력이 필요하므로 해당 페이지로 리다이렉트 */
            if(isGuest){
                response.sendRedirect("/socialInfoAdd");
                return;
            }
            /** 권한은 ROLE_USER이지만 Identifier 컬럼이 비어있는 경우
             *  일반 회원가입 유저가 가입한 이메일과 같은 이메일을 사용하는 카카오 계정으로 로그인한 경우
             * */
            if(member.getIdentifier() == null){
                RequestDispatcher dispatcher = request.getRequestDispatcher("connect-to-social");
                dispatcher.forward(request, response);
            }
        }

        /** 회원가입이 완료된 회원일 경우 intro 페이지로 리다이렉트 */
        //기존 세션을 반환 받거나 없으면 새로 세션을 만들어서 반환
        HttpSession session = request.getSession();

        // 게시글 파일 다운로드 횟수 제한을 위한 카운트를 세션에 추가
        session.setAttribute("articleFileDownloadCount", 0);

        //공지사항 파일 다운로드 횟수 제한을 위한 카운트를 세션에 추가
        session.setAttribute("noticeFileDownloadCount", 0);

        response.sendRedirect("/intro");  // 로그인 후 리다이렉트할 페이지
    }
}
