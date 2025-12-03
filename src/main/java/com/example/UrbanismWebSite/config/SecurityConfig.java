package com.example.UrbanismWebSite.config;

import com.example.UrbanismWebSite.handler.LoginFailureHandler;
import com.example.UrbanismWebSite.handler.LoginSuccessHandler;
import com.example.UrbanismWebSite.model.Authority;
import com.example.UrbanismWebSite.model.Member;
import com.example.UrbanismWebSite.model.MemberUserDetails;
import com.example.UrbanismWebSite.repository.AuthorityRepository;
import com.example.UrbanismWebSite.repository.MemberRepository;
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

import java.util.List;

import static org.springframework.web.servlet.function.RequestPredicates.headers;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final LoginFailureHandler loginFailureHandler;
    private final LoginSuccessHandler loginSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/article/list", "/article/list/**","/article/content", "/article/content/**","/signup",
                                "/css/**", "/js/**", "/image/**", "/health", "/uploads/**", "/uploads", "/login/kakao"
                                ,"/oauth2/**", "/callback", "/intro", "/findid", "/findpasswd", "/notice/list/**",
                                "/notice/content/**", "/tailwind/**", "/image/image/**", "/article/project  ").permitAll()
                        .requestMatchers("/member/**").hasAuthority("ROLE_ADMIN")
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                //.httpBasic(Customizer.withDefaults())
                .formLogin(form -> form.loginPage("/login")
                        //.defaultSuccessUrl("/")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
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
