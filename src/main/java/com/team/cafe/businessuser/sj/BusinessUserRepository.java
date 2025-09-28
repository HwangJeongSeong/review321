package com.team.cafe.businessuser.sj;

import com.team.cafe.user.sjhy.SiteUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusinessUserRepository extends JpaRepository<BusinessUser, Long> {
    // 기본
    boolean existsByBusinessNumber(String businessNumber);
    Optional<BusinessUser> findByRepresentativeEmail(String email);

    // 편의 메서드 (서비스에서 쓰는 것만 남기기)
    Optional<BusinessUser> findByUser(SiteUser user);
    Optional<BusinessUser> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    Optional<BusinessUser> findByBusinessNumber(String businessNumber);
}

