package com.example.UrbanismWebSite.service;

import com.example.UrbanismWebSite.model.Authority;
import com.example.UrbanismWebSite.model.Member;
import com.example.UrbanismWebSite.model.MemberUserDetails;
import com.example.UrbanismWebSite.repository.AuthorityRepository;
import com.example.UrbanismWebSite.repository.MemberRepository;
import jakarta.websocket.OnClose;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final MemberRepository memberRepository;
    private final AuthorityRepository authorityRepository;

    /**
     * 카카오 로그인 성공 후 사용자 정보를 가져와 DB에 저장/업데이트하는 핵심 메서드
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 기본 OAuth2UserService를 통해 사용자 정보(Map 형태)를 가져옵니다.
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // OAuth2 등록 ID (예: kakao)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 소셜 서버에서 받은 사용자 정보 Map
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 사용자 정보 획득에 사용되는 속성 키 (카카오의 경우 'id' 필드)
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        //attributes Map에서 userNameAttributeName에 해당하는 키값 반환
        Object userIdValue = attributes.get(userNameAttributeName);

        //유저의 카카오 고유 ID 반환
        String userId = String.valueOf(userIdValue);

        // 2. 카카오 API 응답 구조에 맞게 이메일과 닉네임 등을 추출
        OAuth2Attributes oauth2Attributes = extractAttributes(registrationId, attributes, userId);

        // 3. DB 처리: 회원가입 또는 정보 업데이트
        Member member = saveOrUpdate(oauth2Attributes);
        List<Authority> authority = authorityRepository.findByMember(member);

        // 4. Spring Security Context에 MemberUserDetails 객체를 반환 (핵심 수정)
        MemberUserDetails memberUserDetails = new MemberUserDetails(member, authority);

        // OAuth2User의 원본 속성(attributes)을 MemberUserDetails에 저장합니다.
        memberUserDetails.setAttributes(attributes); // <--- MemberUserDetails에 setter가 필요

        // 4. Spring Security가 인증을 완료하도록 OAuth2User 객체를 반환합니다.
        return memberUserDetails;
    }

    /**
     * OAuth2Attributes: 카카오 API 응답에서 필요한 속성을 추출하여 담는 내부 DTO
     */
    @Getter
    private static class OAuth2Attributes {
        private final String email;
        private final String provider;
        private final String identifier;

        @Builder
        public OAuth2Attributes(String email, String provider, String identifier) {
            this.email = email;
            this.provider = provider;
            this.identifier = identifier;
        }
    }

    /**
     * 소셜 서버의 응답 Map에서 이메일과 닉네임을 추출하는 로직
     */
    private OAuth2Attributes extractAttributes(String registrationId, Map<String, Object> attributes, String userId) {
        if ("kakao".equals(registrationId)) {
            // 카카오의 경우 'kakao_account' 키 아래에 이메일과 'profile' 정보가 있습니다.
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");

            // 이메일 추출
            String email = (String) kakaoAccount.get("email");

            //소셜 플랫폼 추출
            String provider = registrationId;

            // 플랫폼과 고유 ID 조합으로 식별자 생성 -> kakao_userId...
            String identifier = "kakao_" + userId;

            return OAuth2Attributes.builder()
                    .email(email)
                    .provider(provider)
                    .identifier(identifier)
                    .build();
        }

        // 다른 소셜 서비스가 있다면 여기에 분기 로직을 구현합니다.
        throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 서비스입니다: " + registrationId);
    }

    /**
     * DB에서 회원을 찾아 없으면 저장(회원가입), 있으면 업데이트(로그인)를 처리.
     */
    private Member saveOrUpdate(OAuth2Attributes attributes) {
        String identifier = attributes.getIdentifier();
        String email = attributes.getEmail();
        String provider = attributes.getProvider();
        Member member = memberRepository.findByEmail(email).orElse(null);

        // 회원 정보가 없는 소셜 로그인인 경우, identifier만 저장하고 기본 권한 부여
        if (member == null) {
            // 신규 회원은 identifier만 저장하고, 기본 권한 부여
            log.info("[ New Member Sign-up ] Member identifier: {}", identifier);
            member = Member.builder()
                    .name("temp")
                    .phone("temp")
                    .passwd(provider + "_" + UUID.randomUUID()) //소셜 플랫폼의 경우 UUID 비밀번호 발급
                    .identifier(identifier)
                    .email(email)
                    .build();
            memberRepository.save(member);

            // 기본 권한 ROLE_USER 부여
            Authority authority = Authority.builder()
                    .member(member)
                    .authority("ROLE_GUEST")  //이름 및 전화번호 정보를 입력하기 전까지 GUEST 권한 부여
                    .build();
            authorityRepository.save(authority);
        }

        // DB에 저장된 혹은 새로 저장된 member 반환
        return member;
    }
}
