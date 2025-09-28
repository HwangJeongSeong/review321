package com.team.cafe;

import com.team.cafe.test.TestData;
import com.team.cafe.test.TestRepository;
import com.team.cafe.user.sjhy.SiteUser;
import com.team.cafe.user.sjhy.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class CafeApplicationTests {



    // 회원가입 하기 귀찮아서 만든
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void testJpa01() {
        SiteUser users = new SiteUser();
        users.setUsername("hy991006");
        users.setEmail("hy991006@gmail.com");
        users.setPassword(passwordEncoder.encode("1231231!"));
        users.setNickname("어넝");
        this.userRepository.save(users);
    }

}
