package com.team.cafe.signup.sj;

import com.team.cafe.user.sjhy.SiteUser;
import com.team.cafe.user.sjhy.UserRepository;
import com.team.cafe.user.sjhy.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class SignupService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    public String register(String username, String email, String password, String nickname, String phone) {

        // 아이디 중복 체크
        if (userRepository.existsByUsername(username)) {
            return "이미 사용중인 아이디입니다.";
        }

        // 이메일 중복 체크
        if (userRepository.existsByEmail(email)) {
            return "이미 가입된 이메일입니다.";
        }

        // 닉네임 중복 체크
        if (userRepository.existsByNickname(nickname)) {
            return "이미 사용중인 닉네임입니다.";
        }

        // 비밀번호 유효성 검사
        if (!isValidPassword(password)) {
            return "비밀번호는 최소 8자 이상이어야 하고, 특수문자를 포함해야 합니다.";
        }

        // 정상 회원가입 처리
        SiteUser user = new SiteUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setNickname(nickname);
        user.setPassword(passwordEncoder.encode(password));
        user.setPhone(phone);
        user.setRole(UserRole.USER.name());
        user.setCreateDate(LocalDateTime.now());
        userRepository.save(user);

        return null; // null이면 정상 가입
    }

    /** 비밀번호 검증: 최소 8자, 특수문자 포함 */
    private boolean isValidPassword(String password) {
        String pattern = "^(?=.*[!@#$%^&*()_+\\-={}\\[\\]:;\"'<>,.?/]).{8,}$";
        return password != null && password.matches(pattern);
    }

    // ================== AJAX 중복 체크용 ==================
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }
}
