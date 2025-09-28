package com.team.cafe.review.controller;

import com.team.cafe.list.hj.CafeListService;
import com.team.cafe.review.service.ImageStorageService;
import com.team.cafe.review.domain.Review;
import com.team.cafe.review.repository.ReviewRepository;
import com.team.cafe.review.service.CurrentUserService;
import com.team.cafe.review.service.ReviewLikeService;
import com.team.cafe.review.service.ReviewService;
import com.team.cafe.user.sjhy.SiteUser;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ReviewController {

    private static final int MAX_IMAGES = 5;

    private final ReviewService reviewService;
    private final CafeListService cafeService;
    private final CurrentUserService currentUserService;
    private final ImageStorageService imageStorageService;
    private final ReviewRepository reviewRepository;
    private final ReviewLikeService reviewLikeService;

    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    public ReviewController(ReviewService reviewService,
                            CafeListService cafeService,
                            CurrentUserService currentUserService,
                            ImageStorageService imageStorageService,
                            ReviewRepository reviewRepository,
                            ReviewLikeService reviewLikeService) {
        this.reviewService = reviewService;
        this.cafeService = cafeService;
        this.currentUserService = currentUserService;
        this.imageStorageService = imageStorageService;
        this.reviewRepository = reviewRepository;
        this.reviewLikeService = reviewLikeService;
    }

    /** 항상 th:object="${form}" 제공 */
    @ModelAttribute("form")
    public CreateReviewForm formBacking() { return new CreateReviewForm(); }

    /* =======================
       리뷰 리스트 “페이지”
       ======================= */
    @GetMapping({"/cafes/{cafeId}/reviews", "/review/list/{cafeId}"})
    public String listPage(@PathVariable Long cafeId,
                           @RequestParam(name = "rpage", defaultValue = "0") int page,
                           @RequestParam(name = "rsize", defaultValue = "10") int size,
                           @RequestParam(name = "keyword", required = false) String keyword,
                           @RequestParam(name = "sort", defaultValue = "latest") String sort,
                           Model model) {

        var cafe = cafeService.getById(cafeId);
        double avgRating = cafeService.getActiveAverageRating(cafeId);
        long reviewCount = cafeService.getActiveReviewCount(cafeId);

        Page<Review> reviews;
        if ("likes".equalsIgnoreCase(sort)) {
            var pageable = PageRequest.of(page, size);
            reviews = reviewService.getActiveReviewsByCafeWithUserImagesOrderByLikes(cafeId, pageable, keyword);
        } else {
            var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            reviews = reviewService.getActiveReviewsByCafeWithUserImages(cafeId, pageable, keyword);
        }

        var meOpt = currentUserService.getCurrentUser();
        meOpt.ifPresent(me -> model.addAttribute("me", me));

        Map<Long, Boolean> likedMap = new HashMap<>();
        Map<Long, Long> likeCountMap = new HashMap<>();
        for (Review rv : reviews.getContent()) {
            likeCountMap.put(rv.getId(), reviewLikeService.getLikeCount(rv.getId()));
            meOpt.ifPresent(me -> likedMap.put(rv.getId(), reviewLikeService.isLiked(rv.getId(), me.getId())));
        }

        List<String> topReviewImages = reviewService.getTopReviewImageUrlsByLikes(cafeId, 10);

        model.addAttribute("cafe", cafe);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("reviewCount", reviewCount);
        model.addAttribute("page", reviews);
        model.addAttribute("likedMap", likedMap);
        model.addAttribute("likeCountMap", likeCountMap);
        model.addAttribute("topReviewImages", topReviewImages);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);

        return "review/list";
    }

    /* =======================
       리뷰 리스트 “프래그먼트”
       ======================= */
    @GetMapping(value = "/cafes/{cafeId}/reviews/section", produces = MediaType.TEXT_HTML_VALUE)
    public String listSection(@PathVariable Long cafeId,
                              @RequestParam(name = "rpage", defaultValue = "0") int page,
                              @RequestParam(name = "rsize", defaultValue = "10") int size,
                              @RequestParam(name = "keyword", required = false) String keyword,
                              @RequestParam(name = "sort", defaultValue = "latest") String sort,
                              Model model) {

        var cafe = cafeService.getById(cafeId);
        double avgRating = cafeService.getActiveAverageRating(cafeId);
        long reviewCount = cafeService.getActiveReviewCount(cafeId);

        Page<Review> reviews;
        if ("likes".equalsIgnoreCase(sort)) {
            var pageable = PageRequest.of(page, size);
            reviews = reviewService.getActiveReviewsByCafeWithUserImagesOrderByLikes(cafeId, pageable, keyword);
        } else {
            var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            reviews = reviewService.getActiveReviewsByCafeWithUserImages(cafeId, pageable, keyword);
        }

        var meOpt = currentUserService.getCurrentUser();
        meOpt.ifPresent(me -> model.addAttribute("me", me));

        Map<Long, Boolean> likedMap = new HashMap<>();
        Map<Long, Long> likeCountMap = new HashMap<>();
        for (Review rv : reviews.getContent()) {
            likeCountMap.put(rv.getId(), reviewLikeService.getLikeCount(rv.getId()));
            meOpt.ifPresent(me -> likedMap.put(rv.getId(), reviewLikeService.isLiked(rv.getId(), me.getId())));
        }

        model.addAttribute("cafe", cafe);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("reviewCount", reviewCount);
        model.addAttribute("page", reviews);
        model.addAttribute("likedMap", likedMap);
        model.addAttribute("likeCountMap", likeCountMap);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);

        return "review/list :: section";
    }

    /* =======================
       위치 인증 폼
       ======================= */
    @GetMapping("/cafes/{cafeId}/reviews/location")
    public String locationCertify(@PathVariable Long cafeId, Model model) {
        currentUserService.getCurrentUserOrThrow();
        var cafe = cafeService.getById(cafeId);
        model.addAttribute("cafe", cafe);
        model.addAttribute("kakaoApiKey", kakaoApiKey);
        // location_certify.html expects the address under the name "address1"
        // so expose the cafe's primary address using that key
        model.addAttribute("address1", cafe.getStreetAdr());
        return "review/location_certify";
    }

    /* =======================
       리뷰 상세 (GET)
       ======================= */
    @GetMapping("/reviews/{id}")
    public String detail(@PathVariable Long id, Model model) {
        reviewService.increaseViewCount(id);
        Review review = reviewRepository.findWithUserAndImagesById(id)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다. id=" + id));

        var meOpt = currentUserService.getCurrentUser();
        meOpt.ifPresent(me -> model.addAttribute("me", me));

        boolean canEdit = meOpt
                .map(me -> review.getUser() != null && review.getUser().getId().equals(me.getId())
                        || "ADMIN".equalsIgnoreCase(me.getRole()) || "ROLE_ADMIN".equalsIgnoreCase(me.getRole()))
                .orElse(false);

        boolean liked = meOpt.map(me -> reviewLikeService.isLiked(id, me.getId())).orElse(false);
        long likeCount = reviewLikeService.getLikeCount(id);

        model.addAttribute("review", review);
        model.addAttribute("canEdit", canEdit);
        model.addAttribute("liked", liked);
        model.addAttribute("likeCount", likeCount);
        return "review/detail";
    }

    /* =======================
       좋아요 토글 (AJAX)
       ======================= */
    @PostMapping(value = "/reviews/{id}/like", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> toggleLike(@PathVariable Long id) {
        var meOpt = currentUserService.getCurrentUser();
        if (meOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        SiteUser me = meOpt.get();
        boolean liked = reviewLikeService.toggle(id, me.getId());
        long count = reviewLikeService.getLikeCount(id);
        return ResponseEntity.ok(Map.of(
                "liked", liked,
                "count", count
        ));
    }

    /* =======================
       작성 폼
       ======================= */
    @GetMapping("/cafes/{cafeId}/reviews/new")
    public String createForm(@PathVariable Long cafeId, Model model) {
        SiteUser me = currentUserService.getCurrentUserOrThrow();
        var cafe = cafeService.getById(cafeId);
        model.addAttribute("cafe", cafe);
        model.addAttribute("mode", "create");
        model.addAttribute("user", me);
        return "review/edit";
    }

    /* =======================
       작성 (일반 제출)
       ======================= */
    @PostMapping("/cafes/{cafeId}/reviews")
    public String create(@PathVariable Long cafeId,
                         @ModelAttribute("form") CreateReviewForm form,
                         BindingResult bindingResult,
                         RedirectAttributes ra,
                         Model model,
                         @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        SiteUser me = currentUserService.getCurrentUserOrThrow();
        var cafe = cafeService.getById(cafeId);

        if (form.getContent() == null || form.getContent().trim().length() < 5) {
            bindingResult.reject("content.tooShort", "리뷰 내용은 5자 이상이어야 합니다.");
        }
        if (form.getRating() == null || form.getRating() < 0.5 || form.getRating() > 5.0) {
            bindingResult.reject("rating.range", "별점은 0.5 ~ 5.0 사이여야 합니다.");
        }

        int urlCount = safeSize(form.getImageUrl());
        int fileCount = safeSize(images);
        if (urlCount + fileCount > MAX_IMAGES) {
            bindingResult.reject("images.tooMany", "이미지는 최대 " + MAX_IMAGES + "장까지만 가능합니다.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("cafe", cafe);
            model.addAttribute("mode", "create");
            model.addAttribute("user", me);
            return "review/edit";
        }

        List<String> urls = normalizeUrls(form.getImageUrl());
        int remaining = MAX_IMAGES - urls.size();
        if (images != null && remaining > 0) {
            int added = 0;
            for (MultipartFile file : images) {
                if (file == null || file.isEmpty()) continue;
                if (added >= remaining) break;
                String url = imageStorageService.store(file);
                urls.add(url);
                added++;
            }
        }

        Review saved = reviewService.createReview(
                cafeId, me, form.getRating(), form.getContent().trim(), urls
        );

        ra.addFlashAttribute("message", "리뷰가 등록되었습니다.");
        return "redirect:/reviews/" + saved.getId();
    }

    /* =======================
       작성 (AJAX)
       ======================= */
    @PostMapping(value = "/cafes/{cafeId}/reviews", headers = "X-Requested-With=XMLHttpRequest")
    @ResponseBody
    public ResponseEntity<?> createAjax(@PathVariable Long cafeId,
                                        @ModelAttribute("form") CreateReviewForm form,
                                        BindingResult bindingResult,
                                        @RequestParam(value = "images", required = false) List<MultipartFile> images) {

        if (form.getContent() == null || form.getContent().trim().length() < 5) {
            bindingResult.reject("content.tooShort", "리뷰 내용은 5자 이상이어야 합니다.");
        }
        if (form.getRating() == null || form.getRating() < 0.5 || form.getRating() > 5.0) {
            bindingResult.reject("rating.range", "별점은 0.5 ~ 5.0 사이여야 합니다.");
        }
        int urlCount = safeSize(form.getImageUrl());
        int fileCount = safeSize(images);
        if (urlCount + fileCount > MAX_IMAGES) {
            bindingResult.reject("images.tooMany", "이미지는 최대 " + MAX_IMAGES + "장까지만 가능합니다.");
        }
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    java.util.Map.of("ok", false, "message", "유효성 오류", "errors", bindingResult.getAllErrors())
            );
        }

        SiteUser me = currentUserService.getCurrentUserOrThrow();

        List<String> urls = normalizeUrls(form.getImageUrl());
        int remaining = MAX_IMAGES - urls.size();
        if (images != null && remaining > 0) {
            int added = 0;
            for (MultipartFile file : images) {
                if (file == null || file.isEmpty()) continue;
                if (added >= remaining) break;
                String url = imageStorageService.store(file);
                urls.add(url);
                added++;
            }
        }

        Review saved = reviewService.createReview(
                cafeId, me, form.getRating(), form.getContent().trim(), urls
        );

        return ResponseEntity.ok(java.util.Map.of("ok", true, "id", saved.getId(), "message", "리뷰가 등록되었습니다."));
    }

    /* =======================
       수정 폼
       ======================= */
    @GetMapping("/reviews/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        SiteUser me = currentUserService.getCurrentUserOrThrow();
        Review review = reviewRepository.findWithUserAndImagesById(id)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다. id=" + id));

        boolean isAuthor = review.getUser() != null && review.getUser().getId().equals(me.getId());
        boolean isAdmin = "ADMIN".equalsIgnoreCase(me.getRole()) || "ROLE_ADMIN".equalsIgnoreCase(me.getRole());
        if (!isAuthor && !isAdmin) throw new SecurityException("작성자 또는 관리자만 수정할 수 있습니다.");

        CreateReviewForm form = new CreateReviewForm();
        form.setRating(review.getRating());
        form.setContent(review.getContent());
        if (review.getImages() != null) {
            List<String> urls = new ArrayList<>();
            review.getImages().forEach(img -> urls.add(img.getImageUrl()));
            form.setImageUrl(urls);
        }

        model.addAttribute("form", form);
        model.addAttribute("review", review);
        model.addAttribute("cafe", review.getCafe());
        model.addAttribute("mode", "edit");
        model.addAttribute("user", me);
        return "review/edit";
    }

    /* =======================
       수정 (POST)
       ======================= */
    @PostMapping("/reviews/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("form") CreateReviewForm form,
                         BindingResult bindingResult,
                         RedirectAttributes ra,
                         Model model,
                         @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        SiteUser me = currentUserService.getCurrentUserOrThrow();
        Review review = reviewRepository.findWithUserAndImagesById(id)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다. id=" + id));

        if (form.getContent() == null || form.getContent().trim().length() < 5) {
            bindingResult.reject("content.tooShort", "리뷰 내용은 5자 이상이어야 합니다.");
        }
        if (form.getRating() == null || form.getRating() < 0.5 || form.getRating() > 5.0) {
            bindingResult.reject("rating.range", "별점은 0.5 ~ 5.0 사이여야 합니다.");
        }

        int urlCount = safeSize(form.getImageUrl());
        int fileCount = safeSize(images);
        if (urlCount + fileCount > MAX_IMAGES) {
            bindingResult.reject("images.tooMany", "이미지는 최대 " + MAX_IMAGES + "장까지만 가능합니다.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("review", review);
            model.addAttribute("cafe", review.getCafe());
            model.addAttribute("mode", "edit");
            model.addAttribute("user", me);
            return "review/edit";
        }

        List<String> urls = normalizeUrls(form.getImageUrl());
        int remaining = MAX_IMAGES - urls.size();
        if (images != null && remaining > 0) {
            int added = 0;
            for (MultipartFile file : images) {
                if (file == null || file.isEmpty()) continue;
                if (added >= remaining) break;
                String url = imageStorageService.store(file);
                urls.add(url);
                added++;
            }
        }

        reviewService.updateReview(id, me, form.getRating(), form.getContent().trim(), urls);
        ra.addFlashAttribute("message", "리뷰가 수정되었습니다.");
        return "redirect:/reviews/" + id;
    }

    /* =======================
       삭제
       ======================= */
    @PostMapping("/reviews/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestHeader(value = "Referer", required = false) String referer,
                         RedirectAttributes ra) {
        SiteUser me = currentUserService.getCurrentUserOrThrow();
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다. id=" + id));
        Long cafeId = review.getCafe().getId();
        reviewService.deleteReview(id, me);
        ra.addFlashAttribute("message", "리뷰가 삭제되었습니다.");
        if (referer != null && !referer.isBlank()) {
            return "redirect:" + referer;
        }
        return "redirect:/cafes/" + cafeId + "/reviews";
    }

    /* =======================
       내부 DTO/헬퍼
       ======================= */
    public static class CreateReviewForm {
        @DecimalMin(value = "0.5", message = "별점은 0.5 이상이어야 합니다.")
        @DecimalMax(value = "5.0", message = "별점은 5.0 이하여야 합니다.")
        private Double rating;

        @NotBlank(message = "내용을 입력하세요.")
        @Size(min = 5, message = "리뷰 내용은 5자 이상이어야 합니다.")
        private String content;

        private List<String> imageUrl = new ArrayList<>();

        public CreateReviewForm() {}
        public Double getRating() { return rating; }
        public void setRating(Double rating) { this.rating = rating; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public List<String> getImageUrl() { return imageUrl; }
        public void setImageUrl(List<String> imageUrl) { this.imageUrl = imageUrl; }
    }

    private static int safeSize(List<?> list) {
        if (list == null) return 0;
        int c = 0;
        for (Object e : list) {
            if (e == null) continue;
            if (e instanceof String s) { if (!s.isBlank()) c++; }
            else if (e instanceof MultipartFile f) { if (!f.isEmpty()) c++; }
            else c++;
        }
        return c;
    }

    private static List<String> normalizeUrls(List<String> src) {
        List<String> out = new ArrayList<>();
        if (src == null) return out;
        for (String s : src) {
            if (s == null) continue;
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
            if (out.size() >= MAX_IMAGES) break;
        }
        return out;
    }
}
