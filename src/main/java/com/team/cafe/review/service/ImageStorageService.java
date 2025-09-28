package com.team.cafe.review.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 리뷰 이미지 업로드 전용 스토리지 서비스.
 *
 * - 업로드 파일을 로컬 디스크(app.upload.dir)로 저장
 * - 접근 가능한 공개 URL을 반환 (예: /uploads/2025-09-07/uuid.jpg)
 * - 정적 리소스 매핑은 application-*.properties의
 *   spring.web.resources.static-locations=classpath:/static/,file:${app.upload.dir}/
 *   설정을 따른다.
 *
 * 사용 예)
 *   String url = imageStorageService.store(file);  // DB에는 이 url만 저장
 */
@Service
public class ImageStorageService {

    /**
     * 실제 파일이 저장될 로컬 디렉터리(절대경로).
     * 예) C:/uploads 또는 /var/www/cafe-uploads
     * application-*.properties 에서
     *   app.upload.dir=C:/uploads
     * 처럼 지정해 주세요.
     */
    @Value("${app.upload.dir}")
    private String uploadRootDir;

    /**
     * 공개 URL의 루트 경로(prefix).
     * 보통 정적 리소스 매핑과 맞춰 /uploads 로 두면
     * 브라우저에서 https://domain/uploads/... 로 접근 가능.
     */
    @Value("${app.upload.url-prefix:/uploads}")
    private String publicUrlPrefix;

    /**
     * 이미지 파일 저장 후, 브라우저에서 접근 가능한 URL을 반환.
     * - 일자별 서브폴더(yyyy-MM-dd)로 정리
     * - 파일명은 UUID 기반으로 충돌 방지
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        // 원본 확장자 추출 (png/jpg/webp 등)
        String originalName = Objects.requireNonNull(file.getOriginalFilename(), "파일명이 없습니다.");
        String ext = getExtension(originalName);

        // MIME 기본 검증(선택) - 이미지인지 대략 체크
        String contentType = file.getContentType();
        if (contentType != null && !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }

        // yyyy-MM-dd 하위 폴더
        String dateFolder = LocalDate.now().toString();
        Path targetDir = Paths.get(uploadRootDir, dateFolder);

        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            throw new IllegalStateException("업로드 디렉터리를 생성할 수 없습니다: " + targetDir, e);
        }

        // 고유 파일명
        String filename = UUID.randomUUID().toString().replace("-", "");
        if (!ext.isBlank()) {
            filename += "." + ext;
        }

        Path targetPath = targetDir.resolve(filename).normalize();

        try {
            // 덮어쓰기 방지: 존재하면 새 이름 생성 (아주 드물게 UUID 충돌 대비)
            if (Files.exists(targetPath)) {
                filename = UUID.randomUUID().toString().replace("-", "") + (ext.isBlank() ? "" : "." + ext);
                targetPath = targetDir.resolve(filename).normalize();
            }

            // 저장
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 파일 권한(옵션, OS별 필요 시)
            // Files.setPosixFilePermissions(targetPath, PosixFilePermissions.fromString("rw-r--r--"));

        } catch (IOException e) {
            throw new IllegalStateException("파일 업로드에 실패했습니다.", e);
        }

        // 공개 URL 생성: prefix + /yyyy-MM-dd/uuid.jpg
        return buildPublicUrl(dateFolder, filename);
    }

    /**
     * 필요 시 URL 기반으로 실제 파일도 삭제.
     * (리뷰 이미지 삭제/교체 등에 사용)
     */
    public void deleteByUrl(String url) {
        if (url == null || url.isBlank()) return;

        // url 예: /uploads/2025-09-07/uuid.jpg
        // publicUrlPrefix 제거 후 상대경로 얻기
        String prefix = ensureStartsWithSlash(publicUrlPrefix);
        if (!url.startsWith(prefix)) {
            // 우리 시스템이 만든 URL이 아니면 무시
            return;
        }
        String relative = url.substring(prefix.length()); // "/2025-09-07/uuid.jpg"
        Path filePath = Paths.get(uploadRootDir, relative).normalize();

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // 로깅만 하고 예외 전파 안 함 (비가역적 상황 고려)
        }
    }

    // --------- private helpers ---------

    private String getExtension(String filename) {
        String clean = StringUtils.cleanPath(filename);
        int dot = clean.lastIndexOf('.');
        if (dot < 0) return "";
        String ext = clean.substring(dot + 1).toLowerCase();
        // 길이/문자 검증(간단)
        if (ext.length() > 10) return "";
        return ext.replaceAll("[^a-z0-9]", "");
    }

    private String trimSlashes(String s) {
        String r = s;
        while (r.startsWith("/")) r = r.substring(1);
        while (r.endsWith("/")) r = r.substring(0, r.length() - 1);
        return r;
    }

    private String ensureStartsWithSlash(String s) {
        if (s == null || s.isBlank()) return "/";
        return s.startsWith("/") ? s : "/" + s;
    }

    private String removeTrailingSlash(String s) {
        if (s == null || s.isEmpty()) return "";
        String result = s;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private boolean isAbsoluteUrl(String value) {
        if (value == null) return false;
        int colonIndex = value.indexOf(':');
        if (colonIndex <= 0) return false;
        for (int i = 0; i < colonIndex; i++) {
            char c = value.charAt(i);
            if (!(Character.isLetter(c) || c == '+' || c == '-' || c == '.')) {
                return false;
            }
        }
        return true;
    }

    private String buildPublicUrl(String... pathSegments) {
        String prefix = publicUrlPrefix != null ? publicUrlPrefix.trim() : "";
        String path = Arrays.stream(pathSegments)
                .filter(segment -> segment != null && !segment.isBlank())
                .map(this::trimSlashes)
                .filter(segment -> !segment.isBlank())
                .collect(Collectors.joining("/"));

        if (prefix.isEmpty()) {
            return path.isEmpty() ? "/" : "/" + path;
        }

        boolean absolute = isAbsoluteUrl(prefix) || prefix.startsWith("//");
        String normalizedPrefix = absolute
                ? removeTrailingSlash(prefix)
                : ensureStartsWithSlash(trimSlashes(prefix));

        if (path.isEmpty()) {
            return normalizedPrefix.isEmpty() ? "/" : normalizedPrefix;
        }

        String separator = normalizedPrefix.endsWith("/") ? "" : "/";
        return normalizedPrefix + separator + path;
    }
}
