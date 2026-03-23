package com.example.UrbanismWebSite.service;

import com.example.UrbanismWebSite.model.Authority;
import com.example.UrbanismWebSite.model.Member;
import com.example.UrbanismWebSite.repository.AuthorityRepository;
import com.example.UrbanismWebSite.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthorityService {
    private final AuthorityRepository authorityRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void updateAuthorityToUser(long memberId){
        Member member = memberRepository.findById(memberId).orElseThrow();
        if(member != null){
            List<Authority> auth = authorityRepository.findByMember(member);
            boolean isGuest = auth.stream().anyMatch(
                    curAuth -> "ROLE_GUEST".equals(curAuth.getAuthority())
            );
            if(isGuest){
                Authority deleteAuth = auth.get(0);
                authorityRepository.deleteById(deleteAuth.getId());
                Authority authority = Authority.builder().
                        member(member).
                        authority("ROLE_USER").build();
                authorityRepository.save(authority);
            }
        }
    }
}
