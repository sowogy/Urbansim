package com.example.UrbanismWebSite.model;

import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Map;

@Data
public class MemberUserDetails implements UserDetails, OAuth2User {
    private String username; //메일 정보를 저장
    private String password; //비밀번호를 저장
    private List<SimpleGrantedAuthority> authorities; //해당 회원의 권한들을 저장

    //추가정보
    private String displayName; //화면 표시 이름
    private Long memberId; //회원 고유 ID

    //소셜 로그인의 UserDetail 정보를 저장
    private Map<String, Object> attributes;

    //기본 생성자
    public MemberUserDetails(Member member, List<Authority> authorities){
        this.username = member.getEmail();
        this.displayName = member.getName();
        this.password = member.getPasswd();
        this.memberId = member.getId();
        this.authorities = authorities.stream()
                .map(authority -> new SimpleGrantedAuthority(authority.getAuthority()))
                .toList();
    }

    //소셜 로그인 추가 정보 입력 후 인증 토큰 초기화를 위한 생성자
    public MemberUserDetails(String username, String password, Long memberId,
                             String displayName, Map<String, Object> attributes,
                             List<SimpleGrantedAuthority> authorities) {
        this.username = username;
        this.password = password;
        this.memberId = memberId;
        this.displayName = displayName;
        this.attributes = attributes;
        this.authorities = authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        // 1. 일반 로그인(Form Login)인 경우: 생성자에서 세팅한 username(이메일) 반환
        if (this.username != null) {
            return this.username;
        }

        // 2. 소셜 로그인(OAuth2)인 경우: attributes에서 식별자 반환
        if (attributes != null && attributes.containsKey("id")) {
            return String.valueOf(attributes.get("id"));
        }

        return null; // 예외 상황
    }

    @Override
    public String getName() {
        return displayName;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
