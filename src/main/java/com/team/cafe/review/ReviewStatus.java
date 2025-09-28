package com.team.cafe.review;

/**
 * ReviewStatus 열거형(enum)
 * - 리뷰의 현재 상태를 표현하는 상수 집합
 * - DB에는 문자열(예: "ACTIVE", "REPORTED", "DELETED")로 저장됨
 * - 코드 가독성이 좋아지고, 상태값을 안전하게 관리 가능
 */
public enum ReviewStatus {
    ACTIVE,   // 정상적으로 보여지는 리뷰 (기본값)
    REPORTED, // 신고가 접수된 상태 (관리자 검토 필요)
    DELETED;  // 삭제된 리뷰 (사용자 요청/관리자 조치)
    // ⚠️ 여기까지 enum 상수 → 마지막에는 반드시 세미콜론(;) 붙여야 메서드 선언 가능

    /**
     * 상태에 대한 한국어 설명을 반환
     * - 뷰(Thymeleaf)나 API 응답에서 사용자 친화적으로 표시 가능
     */
    public String getDescription() {
        return switch (this) {
            case ACTIVE -> "정상 게시됨";
            case REPORTED -> "신고 접수됨";
            case DELETED -> "삭제됨";
        };
    }
}
