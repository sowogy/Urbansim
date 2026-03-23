package com.example.UrbanismWebSite.controller;

import com.example.UrbanismWebSite.dto.*;
import com.example.UrbanismWebSite.exception.BusinessException;
import com.example.UrbanismWebSite.model.Member;
import com.example.UrbanismWebSite.model.MemberUserDetails;
import com.example.UrbanismWebSite.service.AuthorityService;
import com.example.UrbanismWebSite.service.DailySignUpCodeService;
import com.example.UrbanismWebSite.service.EmailService;
import com.example.UrbanismWebSite.service.MemberService;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class HomeController {
    private final MemberService memberService;
    private final EmailService emailService;
    private final DailySignUpCodeService codeService;
    private final AuthorityService authorityService;

    /** URL 접근 시 최초 화면 */
    @GetMapping
    public String getHome(){
        return "forward:/intro";
    }

    /** 로그인 */
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
    public String memberAdd(@ModelAttribute("member") MemberForm memberForm,
                               HttpServletRequest request){
        return "signup";
    }

    /** 회원가입 */
    @PostMapping("/signup")
    @ResponseBody
    public ResponseEntity<?> memberAdd(@Valid @ModelAttribute("member") MemberForm memberForm,
                                RedirectAttributes redirectAttributes){
        if(memberService.isEmail(memberForm.getEmail())){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "이미 사용중인 메일입니다.");
        }
        if(memberService.isPhone(memberForm.getPhone())){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "이미 사용중인 전화번호입니다.");
        }
        if(!memberForm.getPasswd().equals(memberForm.getPasswdConfirm())){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "새로 설정헐 비밀번호 정보와 틀립니다.");
        }
        if(memberForm.getPasswd() == null || memberForm.getPasswd().trim().length() < 8){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "비밀번호를 8자 이상 입력해주세요.");
        }
        if(!codeService.verifyCode(memberForm.getAuthenticate_code())){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "인증코드가 틀립니다, 관리자에게 문의해주세요.");
        }
        memberService.create(memberForm);
        redirectAttributes.addFlashAttribute("signSuccess", Boolean.TRUE);
        return ResponseEntity.ok(Map.of("message", "회원가입이 성공적으로 완료되었습니다."));
    }

    /** 비밀번호 변경 */
    @GetMapping("/password")
    public String getPassword(@ModelAttribute("password")PasswordForm passwordForm,
                              @AuthenticationPrincipal MemberUserDetails memberUserDetails){
        if(memberService.isSocialLogin(memberUserDetails.getMemberId())){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "소셜 로그인 사용자는 비밀번호 변경이 불가능합니다.");
        }
        return "password";
    }

    /** 비밀번호 변경 */
    @PostMapping("/password")
    @ResponseBody
    public ResponseEntity<?> postPassword(@Valid @ModelAttribute("password") PasswordForm passwordForm,
                               @AuthenticationPrincipal MemberUserDetails memberUserDetails){
        //기존 패스워드가 올바른지 확인
        if(!memberService.checkPassword(
                memberUserDetails.getMemberId(), passwordForm.getOld()
        )){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "기존 패스워드가 올바르지 않습니다.");
        }
        if(!passwordForm.getNew_password().equals(passwordForm.getNew_passwordConfirm())){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "새로 설정할 패스워드와 일치하지 않습니다.");
        }
        memberService.updatePassword(memberUserDetails.getMemberId(), passwordForm.getNew_password());
        return ResponseEntity.ok(Map.of("message", "패스워드가 성공적으로 변경되었습니다."));
    }

    /** 비밀번호 찾기 */
    @GetMapping("/find-passwd")
    public String getFindPassword(@ModelAttribute("findPasswdForm") FindPasswdForm findPasswdForm){
        return "find-passwd";
    }

    @PostMapping("/find-passwd")
    @ResponseBody // 뷰가 아니라 데이터를 반환한다는 뜻 (RestController가 아니라면 필수)
    public ResponseEntity<?> findPasswd(@Valid @ModelAttribute FindPasswdForm findPasswdForm){
        //전달받은 이메일로 멤버 찾기
        Member member = memberService.findPasswd(findPasswdForm.getEmail(), findPasswdForm.getPhone());

        if (member == null) {
            // GlobalExceptionHandler의 BusinessException이 처리하도록 던짐
            throw new BusinessException(HttpStatus.BAD_REQUEST, "입력하신 정보와 일치하는 회원이 없습니다.");
        }

        // 3. 성공 로직
        String tempPasswd = UUID.randomUUID().toString().substring(0, 8);
        memberService.updatePassword(member.getId(), tempPasswd);
        emailService.sendTempPasswordNotice(findPasswdForm.getEmail(), tempPasswd);

        // 4. 성공 응답 (JSON)
        // Map이나 DTO를 써도 되지만 간단하게 메시지만 보낼 수도 있습니다.
        return ResponseEntity.ok(Map.of("message", "임시 비밀번호가 이메일로 발송되었습니다."));
    }

    /** 아이디 찾기 */
    @GetMapping("/findid")
    public String getFindId(@ModelAttribute("findIdForm") FindIdForm findIdForm){
        return "findId";
    }

    /** 아이디 찾기 */
    @PostMapping("/findid")
    public String findId(@Valid @ModelAttribute("findIdForm")FindIdForm findIdForm,
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

    //홈페이지 인트로 페이지로 이동
    @GetMapping("/intro")
    public String getIntro(){
        return "intro";
    }

    @GetMapping("/socialInfoAdd")
    public String socialInfoAdd(@ModelAttribute("socialMemberForm") SocialMemberForm socialMemberForm){
        return "social-info-add";
    }

    @PostMapping("/socialInfoAdd")
    @ResponseBody
    public ResponseEntity<?> createSocialInfo(@Valid @ModelAttribute("socialMemberForm") SocialMemberForm socialMemberForm,
                                              @AuthenticationPrincipal MemberUserDetails user,
                                              HttpServletRequest request,
                                              HttpServletResponse response){
        if(!codeService.verifyCode(socialMemberForm.getAuthenticate_code())){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "인증 코드가 틀립니다, 관리자에게 문의해주세요.");
        }
        if(user.getMemberId() != null){
            socialMemberForm.setId(user.getMemberId());
            memberService.socialUserPatch(socialMemberForm);
            authorityService.updateAuthorityToUser(user.getMemberId());
        }

        SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        //기존 세션을 반환 받거나 없으면 새로 세션을 만들어서 반환
        HttpSession session = request.getSession();
        // 게시글 파일 다운로드 횟수 제한을 위한 카운트를 세션에 추가
        session.setAttribute("articleFileDownloadCount", 0);
        //공지사항 파일 다운로드 횟수 제한을 위한 카운트를 세션에 추가
        session.setAttribute("noticeFileDownloadCount", 0);

        List<SimpleGrantedAuthority> newAuthorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        MemberUserDetails newMemberUserDetails = new MemberUserDetails(
                socialMemberForm.getName(),
                user.getPassword(),
                user.getMemberId(),
                socialMemberForm.getName(),
                user.getAttributes(), // 소셜 로그인 속성 유지
                newAuthorities
        );

        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                newMemberUserDetails, // Principal 교체
                null,           // 보안상 비밀번호는 보통 null 처리 (이미 인증되었으므로)
                newAuthorities  // 권한 목록
        );

        //새 토큰 저장
        securityContext.setAuthentication(newAuth);
        SecurityContextHolder.setContext(securityContext);

        contextRepository.saveContext(securityContext, request, response);

        return ResponseEntity.ok(Map.of("message", "회원가입이 성공적으로 완료되었습니다."));
    }

    /** 회원가입 인증 코드 변경 */
    @PostMapping("/changeSignUpCode")
    public ResponseEntity<?> changeSignUpCode(){
        codeService.init();
        return ResponseEntity.ok(Map.of("message", "인증 코드가 변경되었습니다."));
    }

    @GetMapping("/connectToSocial")
    public String getConnectTOSocialPage(Member member){
        return "connect-to-social";
    }
}

