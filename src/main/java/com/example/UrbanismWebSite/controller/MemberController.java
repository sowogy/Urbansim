package com.example.UrbanismWebSite.controller;

import com.example.UrbanismWebSite.dto.MemberDTO;
import com.example.UrbanismWebSite.dto.MemberForm;
import com.example.UrbanismWebSite.dto.SocialMemberForm;
import com.example.UrbanismWebSite.exception.BusinessException;
import com.example.UrbanismWebSite.service.EmailService;
import com.example.UrbanismWebSite.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;
    private final EmailService emailService;

    @GetMapping("/list")
    public String getMemberList(@PageableDefault(size = 10, sort="id"
            ,direction = Sort.Direction.DESC)Pageable pageable, Model model){
        Page<MemberDTO> page = memberService.findAll(pageable);
        model.addAttribute("page", page);
        return "member-list";
    }

    @GetMapping("/edit")
    public String getMemberEdit(@RequestParam("id") Long id,
                                @ModelAttribute("member") MemberForm memberForm){
        MemberDTO memberDTO = memberService.findById(id);
        memberForm.setId(memberDTO.getId());
        memberForm.setName(memberDTO.getName());
        memberForm.setPhone(memberDTO.getPhone());
        memberForm.setEmail(memberDTO.getEmail());
        return "member-edit";
    }

    @PostMapping("/edit")
    @ResponseBody
    public ResponseEntity<?> postMemberEdit(@ModelAttribute("member") MemberForm memberForm){
        if(memberService.isPhone(memberForm.getPhone())){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "이미 사용중인 전화번호입니다.");
        }
        memberService.patch(memberForm);
        return ResponseEntity.ok(Map.of("message", "회원정보가 성공적으로 수정되었습니다."));
    }

    @GetMapping("/delete")
    public String getDelete(@RequestParam("id") Long id){
        memberService.deleteById(id);
        return "redirect:/member/list";
    }
}

