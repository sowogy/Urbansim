package com.example.UrbanismWebSite.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

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

        // 세션 키 정의
        String KEY_COUNT = "GUEST_LOGIN_ATTEMPT_COUNT"; //로그인 시도 횟수
        String KEY_BLOCK_TIME = "GUEST_LOGIN_BLOCK_TIME";   //로그인 최대 시도 가능 횟수 초과 당시 시간을 저장

        // FlashMap 생성
        SessionFlashMapManager flashMapManager = new SessionFlashMapManager();
        FlashMap flashMap = new FlashMap();

        //에러 메시지 저장 변수
        String errorMessage;

        //로그인 조건 미충족 시 강제 로그아웃을 위한 Handler 생성
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.setInvalidateHttpSession(false);

        Long blockedTime = (Long) session.getAttribute(KEY_BLOCK_TIME);
        //로그인 차단 시간 세션값이 존재하는 경우
        if(blockedTime != null){
            long curTime = System.currentTimeMillis();
            long pastTime = curTime - blockedTime;
            long limitTime = 5 * 60 * 1000; //최대 5분까지 로그인 제한
            if(pastTime < limitTime){
                long remainingTime = limitTime - pastTime;
                int minute = (int) Math.ceil((double) remainingTime / (1000 * 60));
                errorMessage = "로그인 시도 횟수 초과. " + minute + "분 후에 다시 시도해주세요.";
                flashMap.put("error_message", errorMessage);
                flashMapManager.saveOutputFlashMap(flashMap, request, response);
                logoutHandler.logout(request, response, authentication);
                response.sendRedirect("/login");
                return;
            }
        }

        if(blockedTime != null){
            session.removeAttribute(KEY_BLOCK_TIME);
        }

        Integer count = (Integer) session.getAttribute(KEY_COUNT);
        if(count != null){
            session.removeAttribute(KEY_COUNT);
        }

        // 게시글 파일 다운로드 횟수 제한을 위한 카운트를 세션에 추가
        session.setAttribute("articleFileDownloadCount", 0);

        //공지사항 파일 다운로드 횟수 제한을 위한 카운트를 세션에 추가
        session.setAttribute("noticeFileDownloadCount", 0);

        response.sendRedirect("/");
    }
}
