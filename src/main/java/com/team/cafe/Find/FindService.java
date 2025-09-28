package com.team.cafe.Find;

import com.team.cafe.user.sjhy.SiteUser;
import com.team.cafe.user.sjhy.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.MessagingException;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FindService {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    // 인증번호 저장
    private final Map<String, String> verificationCodes = new HashMap<>();

    // 아이디 찾기
    // 인증번호 생성 & 이메일 발송
    public void sendVerificationCode(String email )throws MessagingException  {
        // 사용자 존재 여부 확인
        SiteUser user = userRepository.findByEmail(email)
                .orElseThrow();

        // 6자리 랜덤 코드 생성
        String code = String.valueOf((int) (Math.random() * 900000) + 100000);
        verificationCodes.put(email, code);

        // MimeMessage를 사용하여 HTML 메일 발송 준비
//        MimeMessage mimeMessage = mailSender.createMimeMessage();
//        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

        // HTML 템플릿 (간단 버전)
        String html = """
        <!doctype html>
        <html lang="ko">
        <head><meta charset="utf-8"></head>
        <body style="font-family:Arial,'Noto Sans KR',sans-serif; background:#f7f8fa; padding:20px;">
          <div style="max-width:500px; margin:auto; background:#fff; border-radius:8px; box-shadow:0 4px 12px rgba(0,0,0,0.05);">
            <div style="padding:20px; border-bottom:1px solid #eee; font-size:18px; font-weight:600;">Bean Spot 이메일 인증</div>
            <div style="padding:24px; font-size:14px; line-height:1.6;">
              <p><strong>안녕하세요, 고객님.</strong></p>
              <p>아이디 찾기를 위해 아래 인증번호를 입력해 주세요.</p>
              <div style="margin:20px 0; text-align:center; font-size:28px; font-weight:700; color:#0b66ff; letter-spacing:6px; background:#f4f8ff; padding:16px; border-radius:6px;">%s</div>
              <p>본 메일은 발신 전용이므로 회신되지 않습니다.</p>
            </div>
            <div style="padding:16px 20px; border-top:1px solid #eee; font-size:12px; color:#777;">
              Bean Spot · 고객센터 hy991006@gmail.com
            </div>
          </div>
        </body>
        </html>
        """.formatted(code);

        // MimeMessage 사용
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(email);
        helper.setSubject("[Bean Spot] 아이디 찾기 인증 코드");
        helper.setText(html, true); // HTML 모드로 전송
        // 메일 작성
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setTo(email);
//        message.setSubject("[Bean Spot] 아이디 찾기 인증 코드");
//        message.setText("인증 코드: " + code);

        mailSender.send(message);
    }

    // 인증번호 확인
    public String verifyCodeAndFindId(String email, String code) {
        String savedCode = verificationCodes.get(email);

        if (savedCode == null || !savedCode.equals(code)) {
            throw new RuntimeException("인증번호가 일치하지 않습니다.");
        }

        // 인증 성공 → 아이디 반환
        SiteUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("가입된 이메일이 없습니다."));
        return user.getUsername();
    }



    // =======비밀번호 찾기========

    public void sendVerificationCodePW(String username, String email) throws MessagingException {
        // 사용자 존재 여부 확인
        SiteUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("입력하신 아이디가 존재하지 않습니다."));
        if (!user.getEmail().equals(email)) {
            throw new RuntimeException("아이디와 이메일이 일치하지 않습니다.");
        }

        // 6자리 랜덤 코드 생성
        String code = String.valueOf((int) (Math.random() * 900000) + 100000);
        verificationCodes.put(email, code);

        // HTML 템플릿 (간단 버전)
        String html = """
        <!doctype html>
        <html lang="ko">
        <head><meta charset="utf-8"></head>
        <body style="font-family:Arial,'Noto Sans KR',sans-serif; background:#f7f8fa; padding:20px;">
          <div style="max-width:500px; margin:auto; background:#fff; border-radius:8px; box-shadow:0 4px 12px rgba(0,0,0,0.05);">
            <div style="padding:20px; border-bottom:1px solid #eee; font-size:18px; font-weight:600;">Bean Spot 이메일 인증</div>
            <div style="padding:24px; font-size:14px; line-height:1.6;">
              <p><strong>안녕하세요, 고객님.</strong></p>
              <p>비밀번호 재설정을 위해 아래 인증번호를 입력해 주세요.</p>
              <div style="margin:20px 0; text-align:center; font-size:28px; font-weight:700; color:#0b66ff; letter-spacing:6px; background:#f4f8ff; padding:16px; border-radius:6px;">%s</div>
              <p>본 메일은 발신 전용이므로 회신되지 않습니다.</p>
            </div>
            <div style="padding:16px 20px; border-top:1px solid #eee; font-size:12px; color:#777;">
              Bean Spot · 고객센터 hy991006@gmail.com
            </div>
          </div>
        </body>
        </html>
        """.formatted(code);

        // MimeMessage 사용
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(email);
        helper.setSubject("[Bean Spot] 비밀번호 찾기 인증 코드");
        helper.setText(html, true); // HTML 모드로 전송

//        // 메일 작성
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setTo(email);
//        message.setSubject("[Bean Spot] 비밀번호 찾기 인증 코드");
//        message.setText("인증 코드: " + code);

        mailSender.send(message);
    }

    // 인증번호 확인
    public String verifyCodeAndFindPW(String email, String code) {
        String savedCode = verificationCodes.get(email);

        if (savedCode == null || !savedCode.equals(code)) {
            throw new RuntimeException("인증번호가 일치하지 않습니다.");
        }
        verificationCodes.remove(email);

        return userRepository.findByEmail(email).get().getUsername();
    }

    // 새 비밀번호 업데이트
    public void updatePassword(String username, String newPassword) {

        // 비밀번호 유효성 검사
        if (!isValidPassword(newPassword)) {
            throw new RuntimeException("비밀번호는 최소 8자 이상이고, 특수문자를 포함해야 합니다.");
        }

        SiteUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private boolean isValidPassword(String password) {
        String pattern = "^(?=.*[!@#$%^&*()_+\\-={}\\[\\]:;\"'<>,.?/]).{8,}$";
        return password != null && password.matches(pattern);
    }
}