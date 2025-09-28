package com.team.cafe.review.domain;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * 리뷰 이미지 엔티티.
 * - Review 와 N:1
 * - 리뷰당 정렬 순서(sortOrder) 보장
 */
@Entity
@Table(
        name = "review_images",
        indexes = {
                @Index(name = "idx_review_image_review_id_sort", columnList = "review_id, sort_order")
        }
)
public class ReviewImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 리뷰 (지연 로딩) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_review_image_review"))
    private Review review;

    /** 이미지 접근 경로(URL) */
    @Column(name = "image_url", length = 500, nullable = false)
    private String imageUrl;

    /** 정렬 순서 (0..N-1) */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    // ---- 기본 생성자 ----
    public ReviewImage() {
    }

    // ---- 편의 생성자 ----
    public ReviewImage(Review review, String imageUrl, int sortOrder) {
        this.review = Objects.requireNonNull(review, "review는 null일 수 없습니다.");
        this.imageUrl = Objects.requireNonNull(imageUrl, "imageUrl은 null일 수 없습니다.");
        this.sortOrder = Math.max(0, sortOrder);
    }

    // ---- Getter/Setter ----
    public Long getId() { return id; }

    public Review getReview() { return review; }
    public void setReview(Review review) { this.review = review; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = Math.max(0, sortOrder); }

    // ---- equals/hashCode (id 기준) ----
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReviewImage)) return false;
        ReviewImage that = (ReviewImage) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
