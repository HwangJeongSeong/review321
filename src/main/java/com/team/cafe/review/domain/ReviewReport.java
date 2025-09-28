package com.team.cafe.review.domain;

import com.team.cafe.user.sjhy.SiteUser;
import jakarta.persistence.*;
import lombok.*;

/**
 * ReviewReport 엔티티
 * - 사용자가 리뷰를 신고했을 때 그 정보를 저장하는 테이블
 * - 신고 사유와 처리 여부까지 관리
 */
@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // PK (자동 증가 ID)
    private Long id;

    // 어떤 리뷰에 대한 신고인가?
    // 여러 신고(Report)가 하나의 리뷰에 달릴 수 있으므로 N:1 관계
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Review review;

    // 누가 신고했는가?
    // 여러 신고가 한 사용자를 참조할 수 있으므로 N:1 관계
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private SiteUser reporter;

    // 신고 사유 (필수, 최대 1000자까지 허용)
    @Column(nullable = false, length = 1000)
    private String reason;

    // 신고가 처리되었는지 여부 (기본값 false)
    @Builder.Default
    private boolean resolved = false;
}
