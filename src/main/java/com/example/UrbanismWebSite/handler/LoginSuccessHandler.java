package com.example.UrbanismWebSite.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 로그인 성공 시 수행할 Handler
 *  로그인 성공 시 파일 다운 횟수 제한을 위한 카운트를 세션에 추가
 * */

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        //기존 세션을 반환 받거나 없으면 새로 세션을 만들어서 반환
        HttpSession session = request.getSession();

        // 게시글 파일 다운로드 횟수 제한을 위한 카운트를 세션에 추가
        session.setAttribute("articleFileDownloadCount", 0);

        //공지사항 파일 다운로드 횟수 제한을 위한 카운트를 세션에 추가
        session.setAttribute("noticeFileDownloadCount", 0);

        response.sendRedirect("/");
    }
}
