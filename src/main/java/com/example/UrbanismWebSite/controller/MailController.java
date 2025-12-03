package com.example.UrbanismWebSite.controller;

import com.example.UrbanismWebSite.dto.MailForm;
import com.example.UrbanismWebSite.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/mail")
@RequiredArgsConstructor
@Slf4j
public class MailController {
    private final EmailService emailService;

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
}
