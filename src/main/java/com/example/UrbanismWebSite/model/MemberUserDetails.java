package com.example.UrbanismWebSite.model;

import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

@Data
public class MemberUserDetails implements UserDetails {
    private String username;
    private String password;
    private List<SimpleGrantedAuthority> authorities;

    //Extra
    private String displayName;
    private Long memberId;

    public MemberUserDetails(Member member, List<Authority> authorities){
        this.username = member.getEmail();
        this.displayName = member.getName();
        this.password = member.getPasswd();
        this.memberId = member.getId();
        this.authorities = authorities.stream()
                .map(authority -> new SimpleGrantedAuthority(authority.getAuthority()))
                .toList();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
