package com.team.cafe.businessuser.sj;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 사업자 회원가입 주소 검색 팝업 전용 컨트롤러
 */
@Controller
public class BusinessUserJusoPopupController {

    /**
     * 주소 검색 팝업 페이지
     */
    @GetMapping("/jusoPopup")
    public String jusoPopup() {
        return "signup/jusoPopup"; // templates/signup/jusoPopup.html
    }
}
