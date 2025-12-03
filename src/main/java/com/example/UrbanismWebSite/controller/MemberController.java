package com.example.UrbanismWebSite.controller;

import com.example.UrbanismWebSite.dto.MemberDTO;
import com.example.UrbanismWebSite.dto.MemberForm;
import com.example.UrbanismWebSite.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

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
        memberForm.setEmail(memberDTO.getEmail());
        return "member-edit";
    }

    @PostMapping("/edit")
    public String postMemberEdit(@Valid @ModelAttribute("member")
                                 MemberForm memberForm, BindingResult bindingResult){
        if(bindingResult.hasErrors()){
            return "/member-edit";
        }
        memberService.patch(memberForm);
        return "redirect:/member/list";
    }

    @GetMapping("/delete")
    public String getDelete(@RequestParam("id") Long id){
        memberService.deleteById(id);
        return "redirect:/member/list";
    }
}

