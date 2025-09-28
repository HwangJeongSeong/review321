package com.team.cafe.review.repository;

import com.team.cafe.review.domain.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    /**
     * 특정 리뷰에 속한 이미지들을 sortOrder 오름차순으로 조회.
     */
    List<ReviewImage> findByReview_IdOrderBySortOrderAsc(Long reviewId);

    /**
     * (선택) 리뷰별 이미지 개수
     */
    long countByReview_Id(Long reviewId);

    /**
     * (선택) 특정 리뷰의 모든 이미지를 일괄 삭제
     * - 파생 쿼리 delete는 @Transactional 경계 안에서 실행하는 것이 안전
     */
    @Transactional
    void deleteByReview_Id(Long reviewId);

    /**
     * (선택) 첫 번째(가장 작은 sortOrder) 이미지 - 썸네일 용도로 유용
     */
    Optional<ReviewImage> findTopByReview_IdOrderBySortOrderAsc(Long reviewId);

    /**
     * (선택) 동일 URL 중복 방지/검사용
     */
    boolean existsByReview_IdAndImageUrl(Long reviewId, String imageUrl);
}
