package com.example.UrbanismWebSite.service;

import com.example.UrbanismWebSite.dto.MemberDTO;
import com.example.UrbanismWebSite.dto.MemberForm;
import com.example.UrbanismWebSite.dto.SocialMemberForm;
import com.example.UrbanismWebSite.model.Article;
import com.example.UrbanismWebSite.model.Authority;
import com.example.UrbanismWebSite.model.Member;
import com.example.UrbanismWebSite.repository.ArticleRepository;
import com.example.UrbanismWebSite.repository.AuthorityRepository;
import com.example.UrbanismWebSite.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {
    private final MemberRepository memberRepository;
    private final ArticleRepository articleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthorityRepository authorityRepository;

    public boolean checkPassword(Long id, String password){
        Member member = memberRepository.findById(id).orElseThrow();
        return passwordEncoder.matches(password, member.getPasswd());
    }

    @Transactional
    public void updatePassword(Long id, String password){
        Member member = memberRepository.findById(id).orElseThrow();
        member.setPasswd(passwordEncoder.encode(password));
        memberRepository.save(member);
    }

    @Transactional
    public MemberDTO create(MemberForm memberForm){
        Member member = Member.builder()
                .name(memberForm.getName())
                .email(memberForm.getEmail())
                .passwd(passwordEncoder.encode(memberForm.getPasswd())) //Password는 암호화 후 저장
                .phone(memberForm.getPhone())
                .build();
        Authority authority = Authority.builder()
                        .authority("ROLE_USER")
                        .member(member)
                        .build();
        memberRepository.save(member);
        authorityRepository.save(authority);
        return mapToMemberDTO(member);
    }

    public String findEmail(String name, String phone){
        return memberRepository.findEmail(name, phone).isPresent()
                ? memberRepository.findEmail(name, phone).get() : null;
    }

    public Member findPasswd(String email, String phone){
        return memberRepository.findPasswd(email, phone).isPresent() ?
                memberRepository.findPasswd(email, phone).get() : null;
    }

    public boolean isEmail(String email){
        return memberRepository.findByEmail(email).isPresent();
    }

    public Optional<MemberDTO> findByEmail(String email){
        return memberRepository.findByEmail(email).map(this::mapToMemberDTO);
    }

    public MemberDTO findById(Long id) {
        return memberRepository.findById(id).map(this::mapToMemberDTO).orElseThrow();
    }

    public Page<MemberDTO> findAll(Pageable pageable){
        return memberRepository.findAll(pageable).map(this::mapToMemberDTO);
    }

    public boolean isPhone(String phone){
        return memberRepository.findByPhone(phone).isPresent();
    }

    @Transactional
    public MemberDTO patch(MemberForm memberForm){
        Member member = memberRepository.findById(memberForm.getId()).orElseThrow();
        if(memberForm.getName() != null){
            member.setName(memberForm.getName());
        }

        if(memberForm.getPasswd() != null){
            member.setPasswd(memberForm.getPasswd());
        }

        if(memberForm.getEmail() != null){
            member.setEmail(memberForm.getEmail());
        }
        if(memberForm.getPhone() != null){
            member.setPhone(memberForm.getPhone());
        }
        memberRepository.save(member);
        return mapToMemberDTO(member);
    }

    @Transactional
    public void deleteById(Long id){
        Member member = memberRepository.findById(id).orElseThrow();
        articleRepository.deleteAllByMember(member);
        memberRepository.delete(member);
    }

    private MemberDTO mapToMemberDTO(Member member){
        return MemberDTO.builder()
                .id(member.getId())
                .name(member.getName())
                .email(member.getEmail())
                .phone(member.getPhone())
                .build();
    }

    //소셜 로그인 유저로부터 입력 받은 추가 정보를 저장
    @Transactional
    public MemberDTO socialUserPatch(SocialMemberForm socialMemberForm){
        Member member = memberRepository.findById(socialMemberForm.getId()).orElseThrow();
        if(member.getPasswd() != null){
            member.setPasswd(passwordEncoder.encode(member.getPasswd())); //소셜 로그인 유저의 비밀번호를 암호화
        }
        if(socialMemberForm.getName() != null){
            member.setName(socialMemberForm.getName());
        }
        if(socialMemberForm.getPhone() != null){
            member.setPhone(socialMemberForm.getPhone());
        }
        memberRepository.save(member);
        return mapToMemberDTO(member);
    }

    public boolean isSocialLogin(long id){
        return memberRepository.existsByIdAndIdentifierIsNotNull(id);
    }
}
