package com.example.UrbanismWebSite.config;

import com.example.UrbanismWebSite.filter.GuestSessionFilter;
import com.example.UrbanismWebSite.filter.RoleGuestFilter;
import com.example.UrbanismWebSite.handler.CustomOAuth2SuccessHandler;
import com.example.UrbanismWebSite.handler.LoginFailureHandler;
import com.example.UrbanismWebSite.handler.LoginSuccessHandler;
import com.example.UrbanismWebSite.model.Authority;
import com.example.UrbanismWebSite.model.Member;
import com.example.UrbanismWebSite.model.MemberUserDetails;
import com.example.UrbanismWebSite.repository.AuthorityRepository;
import com.example.UrbanismWebSite.repository.MemberRepository;
import com.example.UrbanismWebSite.service.CustomOAuth2UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;
import java.util.Optional;

import static org.springframework.web.servlet.function.RequestPredicates.headers;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final LoginFailureHandler loginFailureHandler;
    private final LoginSuccessHandler loginSuccessHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/article/list/**","/article/content", "/article/content/**","/signup",
                                "/css/**", "/js/**", "/image/**", "/health", "/uploads/**", "/uploads", "/login/kakao"
                                ,"/oauth2/**", "/callback", "/intro", "/findid", "/find-passwd", "/notice/list/**",
                                "/notice/content/**", "/tailwind/**", "/image/image/**", "/article/project", "/mail/**", "/check-memory", "/error", "/article/download").permitAll()
                        .requestMatchers("/member/**", "/signUpCode", "/notice/add").hasAuthority("ROLE_ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new GuestSessionFilter(), UsernamePasswordAuthenticationFilter.class) //비로그인 사용자 세션 값 관리를 위한 필터 적용
                .addFilterAfter(new RoleGuestFilter(), UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                //.httpBasic(Customizer.withDefaults())
                .formLogin(form -> form.loginPage("/login") //일반 폼 로그인 시 처리
                        //.defaultSuccessUrl("/")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll())
                .oauth2Login(oauth -> oauth.loginPage("/login") //소셜 로그인 시 처리
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(customOAuth2SuccessHandler)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .permitAll()
                );
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(
            MemberRepository memberRepository,
            AuthorityRepository authorityRepository
    ){
        return new UserDetailsService(){
            @Override
            @Transactional //DB 세션 종료 방지
            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                Member member = memberRepository.findByEmail(username).orElseThrow();
                List<Authority> authorities = authorityRepository.findByMember(member);
                return new MemberUserDetails(member, authorities);
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
