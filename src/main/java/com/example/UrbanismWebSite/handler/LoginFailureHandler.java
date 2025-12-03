package com.example.UrbanismWebSite.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

import javax.naming.AuthenticationException;
import java.io.IOException;

@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, org.springframework.security.core.AuthenticationException exception) throws IOException, ServletException {
        String errorMessage;
        String id = request.getParameter("username").trim();
        String passwd = request.getParameter("password").trim();
        if (exception instanceof UsernameNotFoundException) {
            errorMessage = "존재하지 않는 이메일입니다.";
        } else if (exception instanceof BadCredentialsException) {
            errorMessage = "아이디 또는 비밀번호가 잘못 되었습니다.";
        } else if(id.equals("") || passwd.equals("")){
            errorMessage = "아이디와 비밀번호를 모두 입력하세요";
        } else {
            errorMessage = "로그인에 실패하였습니다. 관리자에게 문의하세요.";
        }

        //FlashMap을 이용하여 일회성 에러 메시지 전달
        SessionFlashMapManager flashMapManager = new SessionFlashMapManager();
        FlashMap flashMap = new FlashMap();
        flashMap.put("error_message", errorMessage); // 키는 원하는 대로 설정
        flashMapManager.saveOutputFlashMap(flashMap, request, response);

        //로그인 페이지로 리다이렉트
        response.sendRedirect("/login");
    }
}
