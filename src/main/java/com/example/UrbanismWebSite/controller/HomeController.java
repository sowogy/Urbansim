package com.example.UrbanismWebSite.controller;

import com.example.UrbanismWebSite.dto.*;
import com.example.UrbanismWebSite.model.Member;
import com.example.UrbanismWebSite.model.MemberUserDetails;
import com.example.UrbanismWebSite.service.EmailService;
import com.example.UrbanismWebSite.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class HomeController {
    private final MemberService memberService;
    private final EmailService emailService;

    /** URL 접근 시 최초 화면 */
    @GetMapping
    public String getHome(){
        return "forward:/intro";
    }

    /** 로그아웃 */
    @GetMapping("/login")
    public String getLogin(){
        return "login";
    }

    /** 로그아웃 */
    @GetMapping("/logout")
    public String getLogout(){
        return "logout";
    }

    /** 회원가입 */
    @GetMapping("/signup")
    public String getMemberAdd(@ModelAttribute("member") MemberForm memberForm){
        return "signup";
    }

    /** 회원가입 */
    @PostMapping("/signup")
    public String postMemberAdd(@Valid @ModelAttribute("member") MemberForm memberForm,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes){
        boolean hasPhoneOrEmail = false;
        if(memberService.isEmail(memberForm.getEmail()) || memberService.isPhone(memberForm.getPhone())){
            hasPhoneOrEmail = true;
        }
        if(!memberForm.getPasswd().equals(memberForm.getPasswdConfirm())){
            bindingResult.rejectValue("passwdConfirm", "MissMatch", "비밀번호가 다릅니다");
        }
        if(memberForm.getPasswd() == null || memberForm.getPasswd().trim().length() < 8){
            bindingResult.rejectValue("password", "NotBlank", "패스워드를 8자 이상 입력하세요");
        }
        if(hasPhoneOrEmail){
            bindingResult.rejectValue("email", "AlreadyExist", "입력하신 정보로 가입된 이력이 존재합니다.");
        }
        if(bindingResult.hasErrors()){
            return "signup";
        }
        memberService.create(memberForm);
        redirectAttributes.addFlashAttribute("signSuccess", Boolean.TRUE);
        return "redirect:/signup";
    }

    /** 비밀번호 변경 */
    @PostMapping("/password")
    public String postPassword(@Valid @ModelAttribute("password") PasswordForm passwordForm,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal MemberUserDetails memberUserDetails){
        if(!memberService.checkPassword(
                memberUserDetails.getMemberId(), passwordForm.getOld()
        )){
            bindingResult.rejectValue("old", "MissMatch", "비밀번호를 다시 입력해주세요.");
        }
        if(!passwordForm.getNew_password().equals(passwordForm.getNew_passwordConfirm())){
            bindingResult.rejectValue("new_passwordConfirm", "MissMatch", "비밀번호가 틀립니다.");
        }
        if(bindingResult.hasErrors()){
            return "/password";
        }
        memberService.updatePassword(memberUserDetails.getMemberId(), passwordForm.getNew_password());
        return "redirect:/";
    }

    /** 비밀번호 변경 */
    @GetMapping("/password")
    public String getPassword(@ModelAttribute("password")PasswordForm passwordForm){
        return "password";
    }

    /** 비밀번호 찾기 */
    @GetMapping("/findpasswd")
    public String getFindPassword(@ModelAttribute("findPasswdForm") FindPasswdForm findPasswdForm){
        return "findPasswd";
    }

    /** 비밀번호 찾기 */
    @PostMapping("/findpasswd")
    public String postFindPasswd(@Valid @ModelAttribute("findPasswdForm") FindPasswdForm findPasswdForm,
                                 RedirectAttributes redirectAttributes){
        Member member = memberService.findPasswd(findPasswdForm.getEmail(), findPasswdForm.getPhone());
        if( member == null ){
            redirectAttributes.addFlashAttribute("findSuccess", false);
            return "redirect:/findPasswd";
        }
        String tempPasswd = UUID.randomUUID().toString().substring(0, 8);
        memberService.updatePassword(member.getId(), tempPasswd);
        emailService.sendTempPasswordNotice(findPasswdForm.getEmail(), tempPasswd);
        redirectAttributes.addFlashAttribute("findSuccess", true);
        return "redirect:/findPasswd";
    }

    /** 아이디 찾기 */
    @GetMapping("/findid")
    public String getFindId(@ModelAttribute("findIdForm") FindIdForm findIdForm){
        return "findId";
    }

    /** 아이디 찾기 */
    @PostMapping("/findid")
    public String postFindId(@Valid @ModelAttribute("findIdForm")FindIdForm findIdForm,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes){
        String email = memberService.findEmail(findIdForm.getName(), findIdForm.getPhone());
        if( email == null ){
            bindingResult.rejectValue("globalError", "MissMatch", "입력하신 정보와 일치하는 회원정보가 존재하지 않습니다");
        }
        if(bindingResult.hasErrors()){
            return "findId";
        }

        // 성공 시, 찾은 이메일을 Flash Attribute로 추가
        redirectAttributes.addFlashAttribute("foundEmail", email);
        return "redirect:/findId";
    }

    @GetMapping("/intro")
    public String getIntro(){
        return "intro";
    }
}

