package com.team.cafe.businessuser.sj;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BusinessUserDto {

    // ----- SiteUser 정보 -----
    private String username;
    private String password;
    private String email;
    private String nickname;

    // ----- BusinessUser 정보 -----
    private String companyName;
    private String businessNumber;
    private String representativeName;
    private String representativePhone;
    private String representativeEmail;
    private String zipCode;
    private String streetAdr;
    private String detailAdr;
}
