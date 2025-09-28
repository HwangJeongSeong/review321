package com.team.cafe.list.hj;


import com.team.cafe.Menu.Menu;
import com.team.cafe.bookmark.Bookmark;
import com.team.cafe.businessuser.sj.BusinessUser;
import com.team.cafe.cafeListImg.hj.CafeImage;
import com.team.cafe.keyword.hj.CafeKeyword;
import com.team.cafe.review.domain.BaseEntity;
import com.team.cafe.review.domain.Review;
import com.team.cafe.user.sjhy.SiteUser;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@Getter
@Setter
@Entity
@NoArgsConstructor // JPA 기본 생성자
@AllArgsConstructor(access = AccessLevel.PUBLIC)  // 전체 args 생성자
@Builder
public class Cafe extends BaseEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cafe_id")
    @Comment("카페 PK")
    private Long id;

    @Column(unique = true, nullable = true, length = 100)
    private String googlePlaceId;  // 구글 Place ID (외부 연동용)

    @Column(name = "cafe_name", length = 100, nullable = false)
    @Comment("카페 이름")
    private String name;

    @Column(name = "phone_num", length = 30)
    @Comment("전화번호")
    private String phoneNum;

    @Column(name = "site_url", length = 255)
    @Comment("웹/블로그 URL")
    private String siteUrl;

    @Column(name = "address1", length = 255)
    @Comment("기존 주소 (호환용)")
    private String address1;

    @Column(name = "zip_code", length = 20)
    @Comment("우편번호")
    private String zipCode;

    @Column(name = "street_adr", length = 255)
    @Comment("도로명 주소")
    private String streetAdr;

    @Column(name = "detail_adr", length = 255)
    @Comment("상세 주소")
    private String detailAdr;

    @Column(name = "district", length = 60)
    @Comment("구/군")
    private String district;

    @Column(name = "city", length = 60)
    @Comment("시/도")
    private String city;

    @Column(name = "lat", precision = 10, scale = 7)
    @Comment("위도")
    private BigDecimal lat;

    @Column(name = "lng", precision = 10, scale = 7)
    @Comment("경도")
    private BigDecimal lng;

    @Column(name = "open_time")
    @Comment("영업 시작")
    private LocalTime openTime;

    @Column(name = "close_time")
    @Comment("영업 종료")
    private LocalTime closeTime;

    @Column(name = "parking_yn", nullable = false, columnDefinition = "TINYINT(1)")
    @Comment("주차 가능(0/1)")
    private boolean parkingYn;

    @Column(name = "hit_count", nullable = false)
    @Comment("조회 수")
    private int hitCount;

    @Lob
    @Column(name = "intro", columnDefinition = "TEXT")
    @Comment("카페 소개글")
    private String intro;

//    @Column(name = "like_count", nullable = false)
//    @Comment("좋아요 수")
//    private int likeCount;

//    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME")
//    @Comment("생성 시각")
//    private LocalDateTime createdAt;

//    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME")
//    @Comment("수정 시각")
//    private LocalDateTime updatedAt;

    //관계구조

    @ManyToMany
    @JoinTable(
            name = "likedUsers",
            joinColumns = @JoinColumn(name = "cafe_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<SiteUser> likedUsers;

    // Review.cafe (N:1)

    // Bookmark.cafe (N:1)

    // CafeTag.cafe (N:1)  -> Cafe ⟷ Tag 다대다 중간 엔티티
//    @OneToMany(mappedBy = "cafe")
//    private List<CafeKeyword> cafeKeyword;

    @OneToMany(mappedBy = "cafe", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @Comment("리뷰 목록")
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "cafe", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Comment("북마크 목록")
    private List<Bookmark> bookmarks = new ArrayList<>();

    @OneToMany(mappedBy = "cafe", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @Comment("카페 키워드 목록")
    private List<CafeKeyword> cafeKeywords = new ArrayList<>();

    @OneToMany(mappedBy = "cafe", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Comment("카페이미지 목록")
    private List<CafeImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "cafe", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @Comment("메뉴 목록")
    private List<Menu> Menu = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "business_user_id",               // FK 컬럼명 (cafe 테이블)
            referencedColumnName = "id",             // business 테이블의 PK
            columnDefinition = "BIGINT UNSIGNED",    // ★ FK 컬럼 타입을 business.id와 동일하게
            foreignKey = @ForeignKey(name = "fk_cafe_business")
    )
    private BusinessUser businessUser;


}