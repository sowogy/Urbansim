package com.example.UrbanismWebSite.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** 회원가입 시 사용될 8자리 UUID 코드를 생성
 *  매일 자정마다 갱신되도록 설정
 * */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailySignUpCodeService {
    private final StringRedisTemplate redisTemplate;
    private final String SIGN_UP_CODE = "SIGN_UP_CODE";

    /** 서버 최초 실행 시 회원가입 코드를 생성함*/
    @PostConstruct
    public void init(){
        try{
            log.info("최초 회원가입 코드 생성");
            signUpCode();
        } catch(Exception e){
            log.error("회원가입 코드 생성 실패 ", e);
        }
    }

    /** 매일 자정마다 인증 코드를 갱신 */
    @Scheduled(cron = "0 0 0 * * *")
    public void signUpCode(){
        String code = generateCode();
        redisTemplate.opsForValue().set(SIGN_UP_CODE, code);
        log.info("코드 갱신 : " +  code);
    }

    /** 입력받은 인증 코드가 유효한지 확인 */
    public boolean verifyCode(String input){
        String currentCode = redisTemplate.opsForValue().get(SIGN_UP_CODE);
        return currentCode.equals(input);
    }

    /** 현재 인증 코드를 반환 */
    public String getSIGN_UP_CODE(){
        return redisTemplate.opsForValue().get(SIGN_UP_CODE);
    }

    /** 8자리 UUID 인증 코드를 생성*/
    public String generateCode(){
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
