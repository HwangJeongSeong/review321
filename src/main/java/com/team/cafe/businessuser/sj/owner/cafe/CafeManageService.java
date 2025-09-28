package com.team.cafe.businessuser.sj.owner.cafe;

import com.team.cafe.businessuser.sj.*;
import com.team.cafe.user.sjhy.SiteUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CafeManageService {

    private final BusinessUserRepository businessUserRepository;

    /**
     * 사업장 정보 저장(업서트)
     * - 사업자 회원가입 시 이미 생성된 BusinessUser row를 찾아 필드를 채워넣습니다.
     * - 없다면(비정상 케이스) 새로 만들어 붙입니다.
     */
    @Transactional
    public BusinessUser createBusiness(
                                        SiteUser user,
                                        String companyName,
                                        String businessNumber,
                                        String representativeName,
                                        String representativeEmail,
                                        String representativePhone,
                                        String address,
                                        String description
    ) {
        // ✅ 기존 row 가져오기 (정상 케이스: 가입 시 이미 1줄 존재)
        BusinessUser bu = businessUserRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    BusinessUser nb = new BusinessUser();
                    nb.setUser(user);
                    return nb;
                });

        // ✅ 사업자번호 중복 체크 (자기 자신 제외)
        if (businessNumber != null && !businessNumber.isBlank()) {
            Optional<BusinessUser> existing = businessUserRepository.findByBusinessNumber(businessNumber);
            if (existing.isPresent() && (bu.getId() == null || !existing.get().getId().equals(bu.getId()))) {
                throw new DuplicateBusinessNumberException("중복 사업자번호: " + businessNumber);
            }
        }

        // ✅ 필드 채우기(업데이트)
        if (companyName != null)           bu.setCompanyName(companyName);
        if (businessNumber != null)        bu.setBusinessNumber(businessNumber);
        if (representativeName != null)    bu.setRepresentativeName(representativeName);
        if (representativeEmail != null)   bu.setRepresentativeEmail(representativeEmail);
        if (representativePhone != null)   bu.setRepresentativePhone(representativePhone);
        if (description != null)           bu.setDescription(description); // 엔티티에 description 필드 있어야 함

        return businessUserRepository.save(bu);
    }

    /**
     * (선택) 분리된 프로필 저장 메서드 – 엔티티에 해당 필드가 있을 때만 사용
     */
    @Transactional
    public BusinessUser saveProfile(
            SiteUser user,
            String companyName,
            String address,
            String phone,        // 엔티티에 없으면 사용 X
            String website,      // 엔티티에 없으면 사용 X
            String instagram,    // 엔티티에 없으면 사용 X
            String description,
            boolean wifi,        // 엔티티에 없으면 사용 X
            boolean outlet,      // 엔티티에 없으면 사용 X
            boolean parking,     // 엔티티에 없으면 사용 X
            String businessNumber,
            String representativeName,
            String representativeEmail
    ) {
        BusinessUser bu = businessUserRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("사업자 회원 정보가 없습니다."));

        if (businessNumber != null && !businessNumber.isBlank()) {
            Optional<BusinessUser> existing = businessUserRepository.findByBusinessNumber(businessNumber);
            if (existing.isPresent() && !existing.get().getId().equals(bu.getId())) {
                throw new DuplicateBusinessNumberException("중복 사업자번호: " + businessNumber);
            }
        }

        if (companyName != null)         bu.setCompanyName(companyName);
        if (description != null)         bu.setDescription(description);
        if (representativeName != null)  bu.setRepresentativeName(representativeName);
        if (representativeEmail != null) bu.setRepresentativeEmail(representativeEmail);
        if (businessNumber != null)      bu.setBusinessNumber(businessNumber);

        // 아래는 엔티티에 필드 추가 후 사용
        // if (phone != null) bu.setPhone(phone);
        // if (website != null) bu.setWebsite(website);
        // if (instagram != null) bu.setInstagram(instagram);
        // bu.setWifi(wifi);
        // bu.setOutlet(outlet);
        // bu.setParking(parking);

        return businessUserRepository.save(bu);
    }
}
