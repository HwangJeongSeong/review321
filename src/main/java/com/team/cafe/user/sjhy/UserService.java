package com.team.cafe.user.sjhy;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    // 카카오 로그인/회원가입
    public SiteUser registerOrGetKakaoUser(String kakaoId, String email, String nickname) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    SiteUser user = new SiteUser();
                    user.setUsername(kakaoId);
                    user.setEmail(email);
                    user.setNickname(nickname);
                    user.setPassword(passwordEncoder.encode("KAKAO_PASSWORD"));
                    return userRepository.save(user);
                });
    }

    // 일반 회원가입
    public SiteUser registerUser(SiteUser user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }



    // 사용자 조회
    public SiteUser getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));
    }

    // mypage 저장 메서드
    public SiteUser save(SiteUser user) {
        return userRepository.save(user);
    }


    public SiteUser registerOrGetGoogleUser(String googleId, String email, String nickname) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    SiteUser user = new SiteUser();
                    user.setUsername(googleId);
                    user.setEmail(email);
                    user.setNickname(nickname);
                    user.setPassword(passwordEncoder.encode("GOOGLE_PASSWORD"));
                    return userRepository.save(user);
                });
    }
}
