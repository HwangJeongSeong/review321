package com.team.cafe.user.sjhy;

import com.team.cafe.bookmark.Bookmark;
import com.team.cafe.businessuser.sj.BusinessUser;
import com.team.cafe.review.domain.Review;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;


@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(
        name = "site_user",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_siteuser_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_siteuser_email", columnNames = "email")
        },
        indexes = {
                @Index(name = "idx_siteuser_username", columnList = "username"),
                @Index(name = "idx_siteuser_email", columnList = "email")
        }
)
public class SiteUser implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", columnDefinition = "BIGINT UNSIGNED")
    @Comment("회원 PK")
    private Long id;

    @Column(name = "username", length = 50, nullable = false)
    @Comment("로그인 아이디 (unique)")
    private String username;

    @Column(name = "password", nullable = false)
    @Comment("비밀번호 (암호화 저장)")
    private String password;

    @Column(name = "email", length = 100, nullable = false)
    @Comment("이메일 (unique)")
    private String email;

    @Column(name = "nickname", length = 50)
    @Comment("닉네임")
    private String nickname;

    @Column(name = "role", length = 20, nullable = false)
    @Comment("권한(USER, ADMIN 등)")
    private String role = "USER";

    @Column(name = "last_login")
    @Comment("최근 로그인 일시")
    private LocalDateTime lastLogin;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    @Comment("회원 가입 일시")
    private LocalDateTime createDate;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Comment("회원 정보 수정 일시")
    private LocalDateTime updatedAt;


//    @Column(name = "is_active", nullable = false)
//    @Comment("계정 활성화 여부")
//    private boolean active = true;

    // --- 관계 매핑 ---
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Comment("작성한 리뷰 목록")
    private List<Review> reviews;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Comment("북마크 목록")
    private List<Bookmark> bookmarks;

    // 좋아요(추천)한 리뷰 (Review 쪽의 voters와 연결)
//    @ManyToMany(mappedBy = "voters")
//    @Comment("좋아요 누른 리뷰들")
//    private Set<Review> likedReviews;

    @Column(name = "phone", length = 20)
    @Comment("휴대폰 번호")
    private String phone;

    @Column(name = "rrn", length = 13)
    @Comment("주민등록번호")
    private String rrn;



    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        //  사용자의 권한 목록을 반환
        //  여기서는 ROLE_USER 권한 하나만 부여 (추후 수정 예정)
//        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        // 사업자와 일반 모두
        String roleValue = this.role;
        if (roleValue == null || roleValue.isBlank()) {
            roleValue = UserRole.USER.name();
        }

        String normalizedRole = roleValue.toUpperCase();
        if (!normalizedRole.startsWith("ROLE_")) {
            normalizedRole = "ROLE_" + normalizedRole;
        }

        return List.of(new SimpleGrantedAuthority(normalizedRole));
    }



    //  계정 만료 여부 확인 override
//  true : 만료되지 않음 (로그인 가능)
//  신고기능 여부에 따라 수정 및 삭제 가능
    @Override
    public boolean isAccountNonLocked() { return true; }


    // 일반 회원과 비즈니스 회원 1:1 연결
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private BusinessUser businessUser;
}

