package com.team.cafe.review.service;

import com.team.cafe.review.domain.Review;
import com.team.cafe.review.repository.ReviewRepository;
import com.team.cafe.user.sjhy.SiteUser;
import com.team.cafe.user.sjhy.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ReviewLikeService {
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public boolean isLiked(Long reviewId, Long userId) {
        if (reviewId == null || userId == null) return false;
        return reviewRepository.existsByIdAndLikedUsers_Id(reviewId, userId);
    }

    @Transactional
    public boolean toggle(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰 없음: " + reviewId));
        SiteUser userRef = userRepository.getReferenceById(userId);
        boolean liked = reviewRepository.existsByIdAndLikedUsers_Id(reviewId, userId);
        if (liked) {
            review.getLikedUsers().remove(userRef);
            return false;
        } else {
            review.getLikedUsers().add(userRef);
            return true;
        }
    }

    @Transactional(readOnly = true)
    public long getLikeCount(Long reviewId) {
        return reviewRepository.countLikes(reviewId);
    }
}
