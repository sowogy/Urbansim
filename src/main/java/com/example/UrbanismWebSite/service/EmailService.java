package com.example.UrbanismWebSite.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EmailService {
    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${custom.admin.mail}")
    private String adminMail; //접수된 버그를 전송할 이메일 주소

    @Value("${custom.contact.mail}")
    private String contactMail;

    //생성된 임시비밀번호를 클라이언트 메일로 전송
    @Async
    public void sendTempPasswordNotice(String email, String tempPasswd){
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            mimeMessageHelper.setTo(email); // 메일 수신자
            mimeMessageHelper.setSubject("임시 비밀번호 발급 안내"); // 메일 제목
            mimeMessageHelper.setText(setTempPasswordContext(todayDate(), tempPasswd), true); // 메일 본문 내용, HTML 여부
            javaMailSender.send(mimeMessage);
        } catch (Exception e) {
            log.info("Failed to send Temp Password Email");
            throw new RuntimeException(e);
        }
    }

    //클라이언의 버그 신고 및 문의 사항 전송
    @Async
    public void sendMail(int serviceNum, String description){
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try{
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            if(serviceNum == 1){ //버그 신고 서비스
                mimeMessageHelper.setTo(adminMail);
                mimeMessageHelper.setSubject("Urbanism 사이트 버그 신고 안내");
                mimeMessageHelper.setText(setBugNoticeContext(todayDate(), description));
            }
            else{   //문의사항 접수 서비스
                mimeMessageHelper.setTo(contactMail);
                mimeMessageHelper.setSubject("Urbanism 문의사항 접수 안내");
                mimeMessageHelper.setText(setBugNoticeContext(todayDate(), description));
            }
            javaMailSender.send(mimeMessage);
        } catch(Exception e){
            log.info("Failed to send Bug Notice");
            throw new RuntimeException(e);
        }
    }

    //오늘 날짜를 년-월-일 포맷으로 생성
    public String todayDate(){
        ZonedDateTime todayDate = LocalDateTime.now(ZoneId.of("Asia/Seoul")).atZone(ZoneId.of("Asia/Seoul"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("Y년 M월 d일 H시 m분 s초");
        return todayDate.format(formatter);
    }

    //임시비밀번호 전송을 위한 html 페이지 적용
    public String setTempPasswordContext(String date, String tempPassword) {
        Context context = new Context();
        context.setVariable("date", date);
        context.setVariable("tempPassword", tempPassword);
        return templateEngine.process("temp-password-send", context);
    }

    //버그 전송을 위한 html 페이지 적용
    public String setBugNoticeContext(String date, String description) {
        Context context = new Context();
        context.setVariable("date", date);
        context.setVariable("description", description);
        return templateEngine.process("bug-notice-send", context);
    }
}
