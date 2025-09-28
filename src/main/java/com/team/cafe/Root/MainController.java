package com.team.cafe.Root;

import com.team.cafe.cafeListImg.hj.CafeImageService;
import com.team.cafe.list.hj.Cafe;
import com.team.cafe.list.hj.CafeListService;
import com.team.cafe.list.hj.CafeMatchDto;
import com.team.cafe.review.dto.CafeWithRating;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class MainController {
    private final MainService mainService;
    private final CafeImageService cafeImageService;
    private final CafeListService cafeListService;

    @GetMapping("/")
    public String root() {
        return "redirect:/main";
    }

    @GetMapping("/main")
    public String mainPage(Model model) {

        //오늘의 인기순 카페의 랜덤 List
        List<CafeBannerRow> todayCafe = mainService.getTodayRecommended(50, 5);
        //인기순 카페 List
        List<Cafe> topCafes = mainService.getTop16ByViews();
        //리뷰많은카페 List
        List<CafeReviewSummary> topReviews = mainService.getTop4ByReviews(4);
        // 이번 페이지의 카페 ID들만 모아서
        List<Long> ids = topCafes.stream()
                .map(Cafe::getId)
                .toList();

        // 대표 이미지 URL 맵 생성
        Map<Long, String> imageMap = cafeImageService.getImageUrlMap(ids);
        // 별점평균 갸져오기 ids로
        List<CafeWithRating> ratingAvg = cafeListService.getCafesWithAvgRating(ids);
        //map으로 리턴
        Map<Long, Double> ratingAvgMap = ratingAvg.stream()
                .collect(Collectors.toMap(
                        CafeWithRating::getId,
                        r -> r.getAvgRating() != null ? r.getAvgRating() : 0.0
                ));

        model.addAttribute("topCafes", topCafes);
        model.addAttribute("imageMap", imageMap);
        model.addAttribute("ratingAvgMap", ratingAvgMap);
        model.addAttribute("todayCafe", todayCafe);
        model.addAttribute("topReviews", topReviews);
        return "mainPage";
    }

}
