package com.example.UrbanismWebSite.controller;

import com.example.UrbanismWebSite.dto.MailForm;
import com.example.UrbanismWebSite.exception.BusinessException;
import com.example.UrbanismWebSite.service.DailySignUpCodeService;
import com.example.UrbanismWebSite.service.EmailService;
import com.example.UrbanismWebSite.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/mail")
@RequiredArgsConstructor
@Slf4j
public class MailController {
    private final EmailService emailService;
    private final DailySignUpCodeService codeService;
    private final MemberService memberService;

    @GetMapping("/bugNotice")
    public String getBugNotice(@ModelAttribute("bugNotice") MailForm mailForm)
    {
        return "bug-notice-add";
    };

    @PostMapping("/bugNotice")
    public String postBugNotice(@Valid @ModelAttribute("bugNotice") MailForm mailForm,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes){
        String bugNotice = mailForm.getDescription();
        if(bugNotice.length() > 500){
            bindingResult.rejectValue("Size", "글자수를 초과하였습니다.");
        }
        if(bindingResult.hasErrors()){
            redirectAttributes.addFlashAttribute("result", Boolean.FALSE);
            return "bug-notice-add";
        }
        emailService.sendMail(1, bugNotice);
        redirectAttributes.addFlashAttribute("result", Boolean.TRUE);
        return "redirect:/mail/bugNotice";
    }

    //문의 사항 접수폼 전달
    @GetMapping("/question")
    public String getQuestion(@ModelAttribute("question") MailForm mailForm){
        return "question-add";
    }

    //문의 사항 접수 후 메일로 전송
    @PostMapping("/question")
    public String postQuestion(@Valid @ModelAttribute("question") MailForm mailForm,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes){
        String question = mailForm.getDescription();
        if(question.length() > 500){
            bindingResult.rejectValue("Size", "글자수를 초과하였습니다.");
        }
        if(bindingResult.hasErrors()){
            redirectAttributes.addFlashAttribute("result", Boolean.FALSE);
            return "question-add";
        }
        emailService.sendMail(2, question);
        redirectAttributes.addFlashAttribute("result", Boolean.TRUE);
        return "redirect:/mail/question";
    }

    @GetMapping("/signUpCode")
    public String getSignUpCode(Model model){
        String authenticate_code = codeService.getSIGN_UP_CODE();
        model.addAttribute("authenticate_code", authenticate_code);
        return "sign-up-code";
    }

    /** 회원가입 시 이메일 인증*/
    @GetMapping("/emailAuthenticationCode")
    public ResponseEntity<?> emailCheck(@RequestParam("email") String email,
                                        HttpServletRequest request){
        if(memberService.isEmail(email)){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "이미 사용중인 이메일입니다.");
        }
        //기존 세션을 반환 받거나 없으면 새로 세션을 만들어서 반환
        HttpSession session = request.getSession();

        String code = UUID.randomUUID().toString().substring(0, 8);
        session.setAttribute("authenticationCode", code); //인증 코드 저장
        session.setAttribute("authenticationCodeSendTime", System.currentTimeMillis()); //세션 저장 시간 저장
        emailService.sendEmailAuthenticationCode(email, code);
        return ResponseEntity.ok(Map.of("message", "이메일로 전송된 인증번호를 입력해주세요."));
    }

    @GetMapping("/emailAuthentication")
    public ResponseEntity<?> emailAuthenticate(@RequestParam("authenticationCode") String code,
                                               HttpServletRequest request){
        HttpSession session = request.getSession();
        long validTime = 3 * 60 * 1000; //3분동안 유효
        String savedCode = (String) session.getAttribute("authenticationCode");
        Long savedTime = (Long) session.getAttribute("authenticationCodeSendTime");
        if (savedCode == null || savedTime == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "인증 코드 재발급 후 다시 시도해주세요.");
        }
        long currentTime = System.currentTimeMillis();
        if(currentTime - savedTime > validTime){
            session.removeAttribute("authenticationCode");
            session.removeAttribute("authenticationCodeSendTime");
            throw new BusinessException(HttpStatus.BAD_REQUEST, "코드 유효 시간이 초과되었습니다.");
        }
        if(emailService.emailAuthenticationCodeCheck(savedCode, code)){
            return ResponseEntity.ok(Map.of("message", "이메일 인증이 완료되었습니다."));
        } else{
            throw new BusinessException(HttpStatus.BAD_REQUEST, "인증코드가 일치하지 않습니다.");
        }
    }
}
