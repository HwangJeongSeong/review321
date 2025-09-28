package com.team.cafe.review.domain;

import com.team.cafe.list.hj.Cafe;
import com.team.cafe.user.sjhy.SiteUser;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.HashSet;
import java.util.Set;

/**
 * 리뷰 엔티티.
 * - BaseEntity에 created_at 컬럼이 있다고 가정
 * - 사용자 참조는 user로 일원화 (FK: user_id)
 */
@Entity
@Table(
        name = "reviews",
        indexes = {
                @Index(name = "idx_review_cafe_id_created_at", columnList = "cafe_id, created_at"),
                @Index(name = "idx_review_user_id_created_at", columnList = "user_id, created_at")
        }
)
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 어느 카페의 리뷰인지 (FK: cafe_id) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cafe_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_review_cafe"))
    private Cafe cafe;

    /** 작성자 (FK: user_id) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_review_user"))
    private SiteUser user;

    /** 별점 (1.0 ~ 5.0) */
    @DecimalMin(value = "0.5", message = "별점은 최소 0.5 이상이어야 합니다.")
    @DecimalMax(value = "5.0", message = "별점은 최대 5.0 이하여야 합니다.")
    @Column(name = "rating", nullable = false)
    private double rating;

    /** 내용(최소 5자) */
    @Size(min = 5, message = "리뷰 내용은 5자 이상이어야 합니다.")
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 조회수 */
    @Column(name = "view_count", nullable = false)
    private long viewCount = 0L;

    /** 노출/활성 여부 */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** 리뷰 이미지 (정렬 보장) */
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ReviewImage> images = new ArrayList<>();

    /** 좋아요를 누른 회원들 */
    @ManyToMany
    @JoinTable(
            name = "review_likes",
            joinColumns = @JoinColumn(name = "review_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<SiteUser> likedUsers = new HashSet<>();

//    private int likeCount; // hy 추가
//
//    // hy 추가
//    // 엔티티 저장 , 업데이트 전 호출
//    @PrePersist
//    @PreUpdate
//    public void updateLikeCount() {
//        this.likeCount = likedUsers.size();
//    }

    protected Review() { }

    public Review(Cafe cafe, SiteUser user, double rating, String content) {
        this.cafe = Objects.requireNonNull(cafe, "cafe는 null일 수 없습니다.");
        this.user = Objects.requireNonNull(user, "user는 null일 수 없습니다.");
        this.rating = rating;
        this.content = Objects.requireNonNull(content, "content는 null일 수 없습니다.");
    }

    // ---- Getter/Setter ----
    public Long getId() { return id; }

    public Cafe getCafe() { return cafe; }
    public void setCafe(Cafe cafe) { this.cafe = cafe; }

    public SiteUser getUser() { return user; }
    public void setUser(SiteUser user) { this.user = user; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getViewCount() { return viewCount; }
    public void increaseViewCount() { this.viewCount += 1; }


    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public List<ReviewImage> getImages() { return images; }

    public Set<SiteUser> getLikedUsers() { return likedUsers; }

    /** 이미지 추가 (최대 5장) */
    public void addImage(ReviewImage image) {
        Objects.requireNonNull(image, "image는 null일 수 없습니다.");
        if (this.images.size() >= 5) {
            throw new IllegalStateException("리뷰에는 최대 5장의 이미지만 첨부할 수 있습니다.");
        }
        image.setReview(this);
        if (image.getSortOrder() < 0) {
            image.setSortOrder(this.images.size());
        }
        this.images.add(image);
        normalizeSortOrders();
    }

    /** 이미지 제거 */
    public void removeImage(ReviewImage image) {
        if (image == null) return;
        this.images.remove(image);
        image.setReview(null);
        normalizeSortOrders();
    }

    /** 정렬값 보정 (0..N-1) */
    public void normalizeSortOrders() {
        for (int i = 0; i < this.images.size(); i++) {
            this.images.get(i).setSortOrder(i);
        }
    }
}
