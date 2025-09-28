package com.team.cafe.Root;

public interface CafeBannerRow {
    Long getId();
    String getCafeName();
    String getPrimaryImageUrl(); // 대표 이미지, 없으면 null
}
