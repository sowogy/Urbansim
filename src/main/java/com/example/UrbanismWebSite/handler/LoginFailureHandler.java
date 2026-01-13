package com.example.UrbanismWebSite.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

import java.io.IOException;

@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        HttpSession session = request.getSession();

        // 세션 키 정의
        String KEY_COUNT = "GUEST_LOGIN_ATTEMPT_COUNT"; //로그인 시도 횟수
        String KEY_BLOCK_TIME = "GUEST_LOGIN_BLOCK_TIME";   //로그인 최대 시도 가능 횟수 초과 당시 시간을 저장

        // FlashMap 생성
        SessionFlashMapManager flashMapManager = new SessionFlashMapManager();
        FlashMap flashMap = new FlashMap();

        long currentTime = System.currentTimeMillis();

        //로그인 차단 당시의 시간을 저장
        Long unblockTime = (Long) session.getAttribute(KEY_BLOCK_TIME);

        //로그인 시도 가능 횟수를 초과한 경우
        if (unblockTime != null) {
            if (currentTime < unblockTime) {
                long remainTimeMillis = unblockTime - currentTime;
                // 올림 처리하여 남은 분 계산 (예: 4분 10초 남았으면 5분으로 표기)
                int remainMin = (int) Math.ceil((double) remainTimeMillis / (1000 * 60));

                String errorMessage = "로그인 시도 횟수 초과. " + remainMin + "분 후에 다시 시도해주세요.";

                flashMap.put("error_message", errorMessage);
                flashMapManager.saveOutputFlashMap(flashMap, request, response);

                response.sendRedirect("/login");
                return; // ★ 중요: 여기서 끝내야 밑에 로직을 안 탑니다!
            } else {
                // 차단 시간이 지났음 -> 차단 해제 및 카운트 초기화
                session.removeAttribute(KEY_BLOCK_TIME);
                session.setAttribute(KEY_COUNT, 0);
            }
        }

        //초과 당시 시간이 저장 안돼있는 경우
        Integer count = (Integer) session.getAttribute(KEY_COUNT);
        if (count == null) {
            count = 0;
        }
        count++; // 이번 실패로 1 증가
        session.setAttribute(KEY_COUNT, count);

        //카운트 5회 초과 여부를 확인
        if (count > 5) {
            // 지금으로부터 5분 뒤를 "해제 시간"으로 설정
            // (1000ms * 60s * 5min)
            long blockReleaseTime = currentTime + (5 * 60 * 1000);
            session.setAttribute(KEY_BLOCK_TIME, blockReleaseTime);

            String errorMessage = "로그인 5회 실패. 5분간 로그인이 제한됩니다.";

            flashMap.put("error_message", errorMessage);
            flashMapManager.saveOutputFlashMap(flashMap, request, response);

            response.sendRedirect("/login");
            return; // ★ 중요: 차단 걸었으면 바로 종료
        }

        //아직 로그인 시도가 가능한 경우
        String errorMessage;
        String id = request.getParameter("username");
        String passwd = request.getParameter("password");

        if (id == null || id.trim().isEmpty() || passwd == null || passwd.trim().isEmpty()) {
            errorMessage = "아이디와 비밀번호를 모두 입력하세요.";
        } else if (exception instanceof BadCredentialsException) {
            errorMessage = "아이디 또는 비밀번호가 잘못 되었습니다. (" + count + "/5)";
        } else if (exception instanceof UsernameNotFoundException) {
            errorMessage = "아이디 또는 비밀번호가 잘못 되었습니다.";
        } else {
            errorMessage = "로그인에 실패하였습니다. 관리자에게 문의하세요.";
        }

        flashMap.put("error_message", errorMessage);
        flashMapManager.saveOutputFlashMap(flashMap, request, response);

        response.sendRedirect("/login");
    }
}