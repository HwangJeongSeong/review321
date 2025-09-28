package com.team.cafe.review.service;

import com.team.cafe.list.hj.Cafe;
import com.team.cafe.list.hj.CafeListRepository;
import com.team.cafe.review.domain.Review;
import com.team.cafe.review.repository.ReviewRepository;
import com.team.cafe.user.sjhy.SiteUser;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class ReviewCafeService {

    private final CafeListRepository cafeListRepository;
    private final ReviewRepository reviewRepository;

    public ReviewCafeService(CafeListRepository cafeListRepository,
                             ReviewRepository reviewRepository) {
        this.cafeListRepository = cafeListRepository;
        this.reviewRepository = reviewRepository;
    }

    // ========================= 카페 리뷰 조회 =========================

    /** 특정 카페의 활성 리뷰 전체 조회 */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<Review> getActiveReviewsByCafe(Long cafeId) {
        Objects.requireNonNull(cafeId, "cafeId is required");
        return reviewRepository.findByCafe_IdAndActiveTrue(cafeId);
    }

    /** 특정 카페의 리뷰 개수 */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public long getReviewCountByCafe(Long cafeId) {
        Objects.requireNonNull(cafeId, "cafeId is required");
        return reviewRepository.countByCafe_IdAndActiveTrue(cafeId);
    }

    // ========================= 카페 리뷰 생성 =========================

    /** 카페 리뷰 생성 */
    public Review createCafeReview(Long cafeId,
                                   SiteUser user,
                                   Double rating,
                                   String content) {
        Objects.requireNonNull(user, "user is required");

        Cafe cafe = cafeListRepository.findById(cafeId)
                .orElseThrow(() -> new IllegalArgumentException("카페를 찾을 수 없습니다. id=" + cafeId));

        if (rating == null || rating < 0.5 || rating > 5.0) {
            throw new IllegalArgumentException("별점은 0.5 ~ 5.0 사이여야 합니다.");
        }
        if (content == null || content.trim().length() < 5) {
            throw new IllegalArgumentException("리뷰 내용은 5자 이상이어야 합니다.");
        }

        // 빌더 대신 명시적 세터 사용 (병합 시 안정성 ↑)
        Review review = new Review(cafe, user, rating, content.trim());

        return reviewRepository.save(review);
    }

    // ========================= 카페 리뷰 활성/비활성 =========================

    /** 리뷰 비활성화 */
    public void deactivateReview(Long reviewId, SiteUser requester) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다. id=" + reviewId));

        boolean isAuthor = review.getUser() != null
                && review.getUser().getId().equals(requester.getId());
        boolean isAdmin = "ADMIN".equalsIgnoreCase(requester.getRole())
                || "ROLE_ADMIN".equalsIgnoreCase(requester.getRole());

        if (!isAuthor && !isAdmin) {
            throw new SecurityException("작성자 또는 관리자만 비활성화할 수 있습니다.");
        }

        review.setActive(false);
        reviewRepository.save(review);
    }

    /** 리뷰 활성화 (관리자 전용) */
    public void activateReview(Long reviewId, SiteUser requester) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다. id=" + reviewId));
        // 어드민 계정이 있을 때;;
        boolean isAdmin = "ADMIN".equalsIgnoreCase(requester.getRole())
                || "ROLE_ADMIN".equalsIgnoreCase(requester.getRole());

        if (!isAdmin) {
            throw new SecurityException("관리자만 활성화할 수 있습니다.");
        }

        review.setActive(true);
        reviewRepository.save(review);
    }
}
