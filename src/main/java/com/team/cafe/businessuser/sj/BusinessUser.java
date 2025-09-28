package com.team.cafe.businessuser.sj;

import com.team.cafe.list.hj.Cafe;
import com.team.cafe.user.sjhy.SiteUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "business")
public class BusinessUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private SiteUser user;  // 연결된 일반회원

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "business_number", nullable = false, unique = true)
    private String businessNumber;

    @Column(name = "representative_name")
    private String representativeName;

    @Column(name = "representative_phone")
    private String representativePhone;

    @Column(name = "representative_email")
    private String representativeEmail;



    // 새로 추가된 주소 컬럼들
    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "street_adr", length = 255)
    private String streetAdr;

    @Column(name = "detail_adr", length = 255)
    private String detailAdr;


    // 설명 필드 추가
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", length = 20)
    private String status = "pending";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        var now = java.time.LocalDateTime.now();
        if (this.createdAt == null) this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = java.time.LocalDateTime.now();
    }

    @OneToOne(mappedBy = "businessUser")
    private Cafe cafe;



}
