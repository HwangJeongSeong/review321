package com.team.cafe.review.repository;


import com.team.cafe.review.domain.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ReviewReportRepository
 * - ReviewReport 엔티티에 대한 CRUD 및 커스텀 쿼리 메서드를 제공
 * - "사용자가 특정 리뷰를 이미 신고했는지" 같은 체크를 쉽게 할 수 있음
 */
public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    /**
     * 특정 리뷰(reviewId)를 특정 사용자(userId)가 이미 신고했는지 여부 확인
     *
     * 메서드 이름 규칙:
     * - existsByReview_IdAndReporter_Id → Review 엔티티의 id와 Reporter(SiteUser)의 id를 조건으로 검색
     *
     * SQL 예시:
     * SELECT EXISTS (
     *   SELECT 1 FROM review_report
     *   WHERE review_id = ? AND reporter_id = ?
     * );
     *
     * 반환값:
     * - true → 이미 신고 기록 있음
     * - false → 아직 신고하지 않음
     */
    boolean existsByReview_IdAndReporter_Id(Long reviewId, Long userId);
}
