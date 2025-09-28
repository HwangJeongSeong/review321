package com.team.cafe.list.hj;

import com.team.cafe.Menu.Menu;
import com.team.cafe.Menu.MenuService;
import com.team.cafe.bookmark.BookmarkService;
import com.team.cafe.bookmark.LikeBookmarkFacade;
import com.team.cafe.cafeListImg.hj.CafeImageService;
import com.team.cafe.keyword.hj.Keyword;
import com.team.cafe.keyword.hj.KeywordRow;
import com.team.cafe.keyword.hj.KeywordService;
import com.team.cafe.keyword.hj.KeywordType;
import com.team.cafe.like.CafeLikeCount;
import com.team.cafe.like.LikeService;
import com.team.cafe.review.domain.Review;
import com.team.cafe.review.dto.CafeWithRating;
import com.team.cafe.review.repository.ReviewRepository;
import com.team.cafe.review.service.ReviewLikeService;
import com.team.cafe.review.service.ReviewService;
import com.team.cafe.user.sjhy.SiteUser;
import com.team.cafe.user.sjhy.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@RequestMapping("/cafe")
@RequiredArgsConstructor
@Controller
public class CafeListController {
    //더미용으로 개발한 @AuthenticationPrincipal SiteUser user 사용 추후에 제거해야 합니다 !!!
    private final CafeListService cafeListService;
    private final UserService userService;
    private final LikeService likeService;
    private final CafeImageService cafeImageService;
    private final ReviewService reviewService;
    private final LikeBookmarkFacade likeBookmarkFacade;
    private final BookmarkService bookmarkService;
    private final KeywordService keywordService;
    private final MenuService menuService;
    private final ReviewLikeService reviewLikeService;

    @Value("{kakao.api.key}")
    private String kakaoApiKey;

    @GetMapping("/list")
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "8") int size,
                       @RequestParam(required = false) String kw,
                       @RequestParam(defaultValue = "createdAt") String sort,
                       @RequestParam(defaultValue = "desc") String dir,
                       @RequestParam(required = false) Boolean parking,  // true면 가능만
                       @RequestParam(required = false) Boolean openNow,  // true면 영업중만
                       @RequestParam(name = "keyList", required = false) List<Long> keyList,
                       Model model
    ) {
        // 기존 코드 : var paging = cafeListService.getCafes(kw, page, size, sort, dir, parking, openNow);
        Page<CafeMatchDto> paging = cafeListService.getCafes(kw, page, size, sort, dir, parking, openNow, keyList);
        
        //키워드 렌더링용
        List<Keyword> all = keywordService.findAllOrderByTypeAndName(); // 네 엔티티/레포에 맞춰 사용
        Map<KeywordType, List<Keyword>> keywordsByType  = new LinkedHashMap<>();
        for (Keyword k : all) {
            KeywordType key = k.getType();
            keywordsByType.computeIfAbsent(key, t -> new ArrayList<>()).add(k);
        }

        // 이번 페이지의 카페 ID들만 모아서
        List<Long> ids = paging.getContent().stream()
               .map(CafeMatchDto::getId)
               .toList();

        // 대표 이미지 URL 맵 생성
        Map<Long, String> imageMap = cafeImageService.getImageUrlMap(ids);
        // 좋아요 갯수 ids로 가져오기
        List<CafeLikeCount> likeCount = likeService.findLikeCountsByCafeIds(ids);
        // 별점평균 갸져오기 ids로
        List<CafeWithRating> ratingAvg = cafeListService.getCafesWithAvgRating(ids);

        //map으로 리턴
        Map<Long, Long> likeCountMap = likeCount.stream()
                .collect(Collectors.toMap(CafeLikeCount::getCafeId, CafeLikeCount::getCnt));

        //map으로 리턴
        Map<Long, Double> ratingAvgMap = ratingAvg.stream()
                .collect(Collectors.toMap(
                        CafeWithRating::getId,
                        r -> r.getAvgRating() != null ? r.getAvgRating() : 0.0
                ));

        model.addAttribute("paging", paging);
        model.addAttribute("kw", kw);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        model.addAttribute("parking", parking);
        model.addAttribute("openNow", openNow);
        model.addAttribute("imageMap", imageMap);
        model.addAttribute("likeCountMap", likeCountMap);
        model.addAttribute("ratingAvgMap", ratingAvgMap);
        //키워드 모델
        model.addAttribute("keywordsByType", keywordsByType);
        model.addAttribute("selectedKeys", keyList);

        return "cafe/cafe_list";
    }


    /** 카페 상세 + 같은 페이지에서 리뷰 작성/리스트 */
    // hy 추가했어요
    @GetMapping("/detail/{cafeId}")
    public String detail(@PathVariable Long cafeId,
                         @RequestParam(name = "rpage", defaultValue = "0") int reviewPage,
                         @RequestParam(name = "rsize", defaultValue = "5") int reviewSize,
                         @RequestParam(name = "sort", required = false, defaultValue = "createdAt") String sort,
                         Principal principal,
                         HttpSession session,
                         Model model) {
        //기존코드 주석
        Cafe cafe = cafeListService.getById(cafeId);
        var images = cafeImageService.findAllByCafeId(cafeId);
        model.addAttribute("images", images); // 이미지 담을 모델 추가했습니다
        List<KeywordRow> detailKeyword = keywordService.findKeywordRowsByCafeId(cafeId);
        List<Menu> menus = menuService.findForDetail(cafeId);
        boolean bookmarked = false;

        cafeListService.increaseViewOncePerSession(cafeId, session);

        // 로그인 사용자
        SiteUser loginUser = null;
        if (principal != null) {
            loginUser = userService.getUser(principal.getName());
        }

        boolean liked = false;
        if (loginUser != null) {
            liked = likeService.isLiked(cafeId, loginUser.getId());
            //북마크
            bookmarked = bookmarkService.existsByUser_IdAndCafe_Id(loginUser.getId(), cafeId);
         
        }


        long likeCount = likeService.getLikeCount(cafeId);
        Boolean openNow = null;
        openNow = cafeListService.isOpenNow(cafe);


        // 상단 배지용 통계
        double avgRating = cafeListService.getActiveAverageRating(cafeId);
        long reviewCount = cafeListService.getActiveReviewCount(cafeId);

        // 리뷰 페이지 (작성자/이미지 N+1 방지 메서드 사용)
        Pageable pageable = PageRequest.of(reviewPage, reviewSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviews = reviewService.getActiveReviewsByCafeWithUserImages(cafeId, pageable);


        // 최신 리뷰 4개 hy
        List<Review> latestReviews;

        if ("hit".equals(sort)) {
            // 인기순
            latestReviews = reviewService.findTop4ByCafe_IdAndActiveTrueOrderByLikesDesc(cafeId);
        } else {
            // 최신순 (기본값)
            latestReviews = reviewService.findTop4ByCafe_IdAndActiveTrueOrderByCreatedAtDesc(cafeId);
        }

        // latestReviews에 있는 각 리뷰의 좋아요 수를 가져와서 맵에 저장
        Map<Long, Long> likeCountMap = new HashMap<>();
        for (Review review : latestReviews) {
            likeCountMap.put(review.getId(), reviewLikeService.getLikeCount(review.getId()));
        }

        // 로그인한 사용자가 각 리뷰에 좋아요를 눌렀는지 확인하는 맵 생성
        Map<Long, Boolean> likedMap = new HashMap<>();
        if (loginUser != null) {
            for (Review review : latestReviews) {
                likedMap.put(review.getId(), reviewLikeService.isLiked(review.getId(), loginUser.getId()));
            }
        }
        // 콘솔에서 리뷰 개수 확인 hy
        System.out.println("리뷰 개수: " + latestReviews.size());
        // 콘솔에서 현재 카페 id 확인 hy
        System.out.println("현재 카페 ID: " + cafeId);
        // 콘솔 확인용 hy
        System.out.println("sort 파라미터: " + sort);



        model.addAttribute("cafe", cafe);
        model.addAttribute("liked", liked);
        model.addAttribute("likeCount", likeCount);
        model.addAttribute("openNow", openNow);
        model.addAttribute("bookmarked", bookmarked);
        model.addAttribute("detailKeyword", detailKeyword);
        model.addAttribute("menus", menus);

        model.addAttribute("avgRating", avgRating);
        model.addAttribute("reviewCount", reviewCount);

        model.addAttribute("reviewSectionUrl",
                String.format("/cafe/detail/%d/reviews/section?rpage=%d&rsize=%d", cafeId, reviewPage, reviewSize));

        // 템플릿에서 ${reviews}로 사용
        model.addAttribute("reviews", reviews);



        // 템플릿에서 ${latestReviews}로 사용 hy
        model.addAttribute("latestReviews", latestReviews);
        model.addAttribute("sort", sort);
        model.addAttribute("likeCountMap", likeCountMap);
        model.addAttribute("likedMap", likedMap);

        // 분리한 템플릿 경로와 일치
        return "cafe/cafe_detail";
    }

    @GetMapping("/map/{cafeId}")
    public String map(@PathVariable Long cafeId, Model model) {
        Cafe cafe = cafeListService.getById(cafeId);
        model.addAttribute("cafe", cafe);
        model.addAttribute("kakaoApiKey", kakaoApiKey);
        return "cafe/cafe_map";
    }

    // Ajax 컨트롤러
    // @PreAuthorize("isAuthenticated()")
    // 상태 변경은 POST로
    @PostMapping(value = "/like/{cafeId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody  // 이 메서드만 JSON으로
    public ResponseEntity<?> toggle(@PathVariable Long cafeId, Principal principal) {
        if (principal == null) {
            // 비로그인 → 401
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SiteUser user = userService.getUser(principal.getName());

        // toggle이 true/false(현재 상태) 반환하도록 만들면 최고
        //boolean liked = likeService.toggle(cafeId, user.getId());
        boolean likedNow = likeBookmarkFacade.toggleAndSync(cafeId, user.getId(), true);

        long count = likeService.getLikeCount(cafeId);

        // 북마크 정책이 “좋아요 ON == 북마크 보장”이면 결과는 likedNow와 동일하게 안내 가능
        return ResponseEntity.ok(Map.of(
                "count", count,
                "liked", likedNow,
                "bookmarked", likedNow   // 정책상 보장됨(OFF도 유지 정책이면 exists 쿼리로 반환)
        ));
    }
//    @GetMapping(value = "/like/status/{cafeId}", produces = MediaType.APPLICATION_JSON_VALUE)
//    @ResponseBody
//    public ResponseEntity<?> checkStatus(@PathVariable Long cafeId, Principal principal) {
//        if (principal == null) {
//            // 비로그인 → 기본값: false
//            return ResponseEntity.ok(Map.of(
//                    "count", likeService.getLikeCount(cafeId),
//                    "liked", false,
//                    "bookmarked", false
//            ));
//        }
//
//        SiteUser user = userService.getUser(principal.getName());
//
//        // 현재 로그인 유저가 북마크했는지 확인
//        boolean likedNow = likeService.isLikedByUser(cafeId, user.getId());
//
//        long count = likeService.getLikeCount(cafeId);
//
//        return ResponseEntity.ok(Map.of(
//                "count", count,
//                "liked", likedNow,
//                "bookmarked", likedNow
//        ));
//    }
}