package com.example.UrbanismWebSite.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class RoleGuestFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        //현재 요청한 URI 확인
        String requestUrl = request.getRequestURI();

        //현재 권한을 가져옴
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication != null && authentication.getAuthorities().stream().anyMatch(
                auth -> "ROLE_GUEST".equals(auth.getAuthority())
        )){
            if(!isAllowedPath(requestUrl)){
                response.sendRedirect("/socialInfoAdd");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
    private boolean isAllowedPath(String uri){
        return  uri.startsWith("/socialInfoAdd") ||           // 스타일 깨지면 안 되니까
                uri.startsWith("/css/") ||           // 스타일 깨지면 안 되니까
                uri.startsWith("/js/") ||            // 스크립트
                uri.startsWith("/image/") ||
                uri.startsWith("/error");
    }

}
