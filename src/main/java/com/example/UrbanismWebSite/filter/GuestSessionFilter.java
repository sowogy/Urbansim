package com.example.UrbanismWebSite.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class GuestSessionFilter extends OncePerRequestFilter {
    public static String GUEST_SESSION_ID = "GUEST_SESSION_ID"; //비로그인 사용자 세션 관리용 ID
    public static String GUEST_FILE_DOWNLOAD_COUNT = "GUEST_ARTICLE_DOWNLOAD_COUNT"; //비로그인 사용자의 파일 다운로드 횟수 관리
    public static String GUEST_LOGIN_ATTEMPT_COUNT = "GUEST_LOGIN_ATTEMPT_COUNT"; //비로그인 사용자의 로그인 시도 횟수 관리
    public static String GUEST_LOGIN_ATTEMPT_OVER_TIME = "GUEST_LOGIN_ATTEMPT_TIME"; //로그인 시도 횟수 초과 시 로그인 제한 시간 저장
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException{
        HttpSession session = request.getSession(true);

        session.setMaxInactiveInterval(1800); //30분간 유지
        if(session.getAttribute(GUEST_SESSION_ID) == null){
            String id = UUID.randomUUID().toString();
            session.setAttribute(GUEST_SESSION_ID, id);
            session.setAttribute(GUEST_FILE_DOWNLOAD_COUNT, 0);
            session.setAttribute(GUEST_LOGIN_ATTEMPT_COUNT, 0);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException{
        String path = request.getRequestURI();
        return path.startsWith("/css") ||
                path.startsWith("/image") ||
                path.startsWith("/js") ||
                path.startsWith("/uploads") ||
                path.startsWith("/tailwind") ||
                path.equals("/error") ||
                path.equals("favicon.ico");
    }
}

