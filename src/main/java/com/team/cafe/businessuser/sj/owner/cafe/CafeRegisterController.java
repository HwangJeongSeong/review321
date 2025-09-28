package com.team.cafe.businessuser.sj.owner.cafe;

import com.team.cafe.businessuser.sj.BusinessUser;
import com.team.cafe.businessuser.sj.BusinessUserRepository;
import com.team.cafe.cafeListImg.hj.CafeImage;
import com.team.cafe.cafeListImg.hj.CafeImageService;
import com.team.cafe.list.hj.Cafe;
import com.team.cafe.list.hj.CafeListRepository;
import com.team.cafe.user.sjhy.SiteUser;
import com.team.cafe.user.sjhy.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/mypage/cafe")
public class CafeRegisterController {

    private final CafeListRepository cafeListRepository;
    private final CafeImageService cafeImageService;
    private final UserService userService;
    private final BusinessUserRepository businessUserRepository;

    /** 모든 뷰에서 ${user} 사용할 수 있도록 주입 */
    @ModelAttribute("user")
    public SiteUser injectUser(Principal principal) {
        if (principal == null) return null;
        return userService.getUser(principal.getName());
    }

    /* ========== 등록 폼 ========== */
    @GetMapping("/register")
    public String showCafeRegister(Principal principal,
                                   HttpServletResponse response,
                                   Model model) {
        if (principal == null) return "redirect:/user/login";
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");

        // 현재 로그인 사용자 및 사업자 정보 주입 (폼 프리필용)
        SiteUser siteUser = userService.getUser(principal.getName());
        if (siteUser == null) return "redirect:/user/login";
        BusinessUser biz = businessUserRepository.findByUserId(siteUser.getId()).orElse(null);

        model.addAttribute("mode", "create");
        model.addAttribute("form", new CafeRegisterRequest());
        model.addAttribute("biz", biz); // 템플릿에서 ${biz.*} 로 사용

        return "mypage/cafe-register";
    }

    /* ========== 등록 저장 ========== */
    @PostMapping(value = "/register", consumes = "multipart/form-data")
    @Transactional
    public String registerCafe(@ModelAttribute("form") CafeRegisterRequest req,
                               @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
                               RedirectAttributes ra,
                               Principal principal) {
        if (principal == null) return "redirect:/user/login";

        try {
            // 1) 현재 사업자 조회
            SiteUser user = userService.getUser(principal.getName());
            BusinessUser business = businessUserRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new IllegalStateException("사업자 회원이 아닙니다."));

            // 2) 이미 보유시 재등록 방지(1:1 정책)
            if (business.getCafe() != null) {
                ra.addFlashAttribute("toast", "이미 등록된 카페가 있습니다.");
                return "redirect:/mypage/cafe/manage?cafeId=" + business.getCafe().getId();
            }

            Cafe cafe = new Cafe();
            cafe.setName(req.getName());
            cafe.setPhoneNum(req.getPhoneNum());
            cafe.setSiteUrl(req.getSiteUrl());
            cafe.setZipCode(req.getZipCode());
            cafe.setStreetAdr(req.getStreetAdr());
            cafe.setDetailAdr(req.getDetailAdr());
            cafe.setDistrict(req.getDistrict());
            cafe.setCity(req.getCity());
            cafe.setOpenTime(req.getOpenTime());
            cafe.setCloseTime(req.getCloseTime());
            cafe.setParkingYn(req.isParkingYn());
            cafe.setHitCount(0);
            cafe.setIntro(req.getIntro()); // ✅ 소개 저장

            cafe.setBusinessUser(business);
            cafe = cafeListRepository.save(cafe);

            business.setCafe(cafe);
            businessUserRepository.save(business);

            if (photos != null && !photos.isEmpty()) {
                cafeImageService.saveCafeImages(cafe.getId(), photos);
            }

            ra.addFlashAttribute("toast", "사업장 등록 완료");
            return "redirect:/mypage/cafe/manage?cafeId=" + cafe.getId();

        } catch (IOException e) {
            log.error("Image save failed", e);
            ra.addFlashAttribute("error", "이미지 저장 실패: " + e.getMessage());
            return "redirect:/mypage/cafe/register";
        } catch (Exception e) {
            log.error("Register failed", e);
            ra.addFlashAttribute("error", "등록 중 오류: " + e.getMessage());
            return "redirect:/mypage/cafe/register";
        }
    }

    /* ========== 수정 폼(프리필) ========== */
    @GetMapping("/edit/{cafeId}")
    public String showEdit(@PathVariable Long cafeId,
                           Principal principal,
                           HttpServletResponse response,
                           Model model) {
        if (principal == null) return "redirect:/user/login";
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");

        Cafe cafe = cafeListRepository.findById(cafeId)
                .orElseThrow(() -> new IllegalArgumentException("카페 없음: " + cafeId));

        // 현재 로그인 사용자 및 사업자 정보 주입 (프리필용)
        SiteUser siteUser = userService.getUser(principal.getName());
        if (siteUser == null) return "redirect:/user/login";
        BusinessUser biz = businessUserRepository.findByUserId(siteUser.getId()).orElse(null);

        // 폼 바인딩 값 미리 채우기
        CafeRegisterRequest form = new CafeRegisterRequest();
        form.setName(cafe.getName());
        form.setPhoneNum(cafe.getPhoneNum());
        form.setSiteUrl(cafe.getSiteUrl());
        form.setZipCode(cafe.getZipCode());
        form.setStreetAdr(cafe.getStreetAdr());
        form.setDetailAdr(cafe.getDetailAdr());
        form.setDistrict(cafe.getDistrict());
        form.setCity(cafe.getCity());
        form.setOpenTime(cafe.getOpenTime());
        form.setCloseTime(cafe.getCloseTime());
        form.setParkingYn(cafe.isParkingYn());
        form.setIntro(cafe.getIntro()); // ✅ 소개 프리필

        List<CafeImage> photos = cafeImageService.findAllByCafeId(cafeId);

        model.addAttribute("mode", "edit");
        model.addAttribute("cafe", cafe);
        model.addAttribute("form", form);
        model.addAttribute("photos", photos);
        model.addAttribute("photoCount", photos.size());
        model.addAttribute("biz", biz); // ✅ 템플릿에서 ${biz.*} 로 사용

        return "mypage/cafe-register"; // 같은 템플릿 재사용
    }

    /* ========== 수정 저장 ========== */
    @PostMapping(value = "/update", consumes = "multipart/form-data")
    @Transactional
    public String updateCafe(@RequestParam("cafeId") Long cafeId, // ← 폼의 hidden과 이름 맞추기
                             @ModelAttribute("form") CafeRegisterRequest req,
                             @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
                             RedirectAttributes ra,
                             Principal principal) {
        if (principal == null) return "redirect:/user/login";

        try {
            Cafe cafe = cafeListRepository.findById(cafeId)
                    .orElseThrow(() -> new IllegalArgumentException("카페 없음: " + cafeId));

            // 업데이트
            cafe.setName(req.getName());
            cafe.setPhoneNum(req.getPhoneNum());
            cafe.setSiteUrl(req.getSiteUrl());
            cafe.setZipCode(req.getZipCode());
            cafe.setStreetAdr(req.getStreetAdr());
            cafe.setDetailAdr(req.getDetailAdr());
            cafe.setDistrict(req.getDistrict());
            cafe.setCity(req.getCity());
            cafe.setOpenTime(req.getOpenTime());
            cafe.setCloseTime(req.getCloseTime());
            cafe.setParkingYn(req.isParkingYn());
            cafe.setIntro(req.getIntro());

            cafeListRepository.save(cafe);

            // 새 사진 추가(삭제는 별도 기능로 분리 권장)
            if (photos != null && !photos.isEmpty()) {
                cafeImageService.saveCafeImages(cafeId, photos);
            }

            ra.addFlashAttribute("toast", "사업장 정보가 수정되었습니다.");
            return "redirect:/mypage/cafe/manage?cafeId=" + cafeId;

        } catch (IOException e) {
            log.error("Image save failed", e);
            ra.addFlashAttribute("error", "이미지 저장 실패: " + e.getMessage());
            return "redirect:/mypage/cafe/edit/" + cafeId;
        } catch (Exception e) {
            log.error("Update failed", e);
            ra.addFlashAttribute("error", "수정 중 오류: " + e.getMessage());
            return "redirect:/mypage/cafe/edit/" + cafeId;
        }
    }
}
