package com.team.cafe.cafeListImg.hj;

import com.team.cafe.list.hj.Cafe;
import com.team.cafe.review.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "cafe_image",
        indexes = {
                @Index(name = "idx_cafe_image_cafe", columnList = "cafe_id"),
//                @Index(name = "idx_cafe_image_primary_sort", columnList = "is_primary, sort_order")
        }
)
public class CafeImage extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")                 // PK
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "cafe_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_cafe_image_cafe")
    )
    private Cafe cafe;                   // 카페 참조

    @Column(name = "img_url", nullable = true, length = 255)
    private String imgUrl;               // 이미지 URL/경로

//    @Column(name = "is_primary", nullable = false)
//    private boolean primaryImage;        // 대표 이미지 여부 (TINYINT(1) ↔ boolean)

//    @Column(name = "sort_order", nullable = false)
//    private int sortOrder;               // 정렬 우선순위 (낮을수록 먼저)

//    @CreationTimestamp
//    @Column(name = "created_at", nullable = false, updatable = false)
//    private LocalDateTime createdAt;     // 등록 시각


}