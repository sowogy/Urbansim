package com.example.UrbanismWebSite.service;

import com.example.UrbanismWebSite.dto.MemberDTO;
import com.example.UrbanismWebSite.dto.MemberForm;
import com.example.UrbanismWebSite.model.Article;
import com.example.UrbanismWebSite.model.Member;
import com.example.UrbanismWebSite.repository.ArticleRepository;
import com.example.UrbanismWebSite.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final ArticleRepository articleRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean checkPassword(Long id, String password){
        Member member = memberRepository.findById(id).orElseThrow();
        return passwordEncoder.matches(password, member.getPasswd());
    }

    public void updatePassword(Long id, String password){
        Member member = memberRepository.findById(id).orElseThrow();
        member.setPasswd(passwordEncoder.encode(password));
        memberRepository.save(member);
    }
    public MemberDTO create(MemberForm memberForm){
        Member member = Member.builder()
                .name(memberForm.getName())
                .email(memberForm.getEmail())
                .passwd(passwordEncoder.encode(memberForm.getPasswd())) //Password는 암호화 후 저장
                .phone(memberForm.getPhone())
                .build();
        memberRepository.save(member);
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
}
