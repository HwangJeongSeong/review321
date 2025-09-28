package com.team.cafe.cafeListImg.hj;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

// 업로드 매핑 추가했습니다.
@Controller
@RequiredArgsConstructor
@RequestMapping("/cafe")
public class CafeImageController {

    private final CafeImageService cafeImageService;

    // 사진 업로드
    @PostMapping("/{cafeId}/images")
    public String uploadImages(@PathVariable Long cafeId,
                               @RequestParam("photos") List<MultipartFile> photos,
                               RedirectAttributes ra) {
        System.out.println("[DEBUG] cafeId=" + cafeId + ", photos.size=" + (photos==null? "null": photos.size()));

        try {
            cafeImageService.saveCafeImages(cafeId, photos);
            ra.addFlashAttribute("toast", "이미지 업로드 완료");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "이미지 업로드 실패: " + e.getMessage());
        }
        return "redirect:/mypage/cafe/manage?cafeId=" + cafeId;
    }
}

