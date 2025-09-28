package com.team.cafe.cafeListImg.hj;

import com.team.cafe.list.hj.Cafe;
import com.team.cafe.list.hj.CafeListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CafeImageService - 카페 이미지 저장/조회
 *
 * - 정적 서빙: spring.web.resources.static-locations=classpath:/static/,file:${app.upload.dir}/
 * - 실제 저장: ${app.upload.dir}/cafes/{cafeId}/{filename}
 * - 공개 URL : /cafes/{cafeId}/{filename}
 */
@Service
@RequiredArgsConstructor
public class CafeImageService {

    private final CafeListRepository cafeListRepository;
    private final CafeImageRepository cafeImageRepository;

    /** application.properties: app.upload.dir=${user.home}/cafe-review/uploads */
    @Value("${app.upload.dir}")
    private String uploadRoot; // 절대경로 루트(= 정적 루트)

    /** 최종 물리 저장 루트: ${app.upload.dir}/cafes */
    private Path physicalRoot() {
        // ✅ uploads를 또 붙이지 않는다! (이미 app.upload.dir 자체가 정적 루트이기 때문)
        return Paths.get(uploadRoot).resolve("cafes");
    }

    /** 목록 페이지용: cafeId -> 대표 이미지 URL(없으면 키 없음) */
    public Map<Long, String> getImageUrlMap(Collection<Long> cafeIds) {
        if (cafeIds == null || cafeIds.isEmpty()) return Map.of();

        List<CafeImage> images = cafeImageRepository.findByCafe_IdIn(cafeIds);

        return images.stream().collect(Collectors.toMap(
                img -> img.getCafe().getId(),
                CafeImage::getImgUrl,
                (a, b) -> a, // 같은 카페 여러 장이면 첫 값 유지
                LinkedHashMap::new
        ));
    }

    /** 상세에서 카페만 필요하면 이 정도면 충분 (필요하면 repo에 fetch join 메서드 추가) */
    public Cafe getDetailImg(Long cafeId) {
        return cafeListRepository.findById(cafeId)
                .orElseThrow(() -> new IllegalArgumentException("no cafe: " + cafeId));
    }

    /** 단일 카페의 모든 이미지 */
    public List<CafeImage> findAllByCafeId(Long cafeId) {
        if (cafeId == null) return List.of();
        return cafeImageRepository.findAllByCafe_Id(cafeId);
    }

    /** 저장: 파일 -> ${app.upload.dir}/cafes/{cafeId}/ , DB URL -> /cafes/{cafeId}/{filename} */
    @Transactional
    public List<CafeImage> saveCafeImages(Long cafeId, List<MultipartFile> files) throws IOException {
        if (cafeId == null) throw new IllegalArgumentException("cafeId가 필요합니다.");
        if (files == null || files.isEmpty()) return List.of();

        Cafe cafe = cafeListRepository.findById(cafeId)
                .orElseThrow(() -> new IllegalArgumentException("카페가 존재하지 않습니다: " + cafeId));

        Path dir = physicalRoot().resolve(String.valueOf(cafeId)); // ${app.upload.dir}/cafes/{cafeId}
        Files.createDirectories(dir);

        List<CafeImage> saved = new ArrayList<>();

        for (MultipartFile f : files) {
            if (f == null || f.isEmpty()) continue;

            String original = Optional.ofNullable(f.getOriginalFilename()).orElse("");
            String ext = StringUtils.getFilenameExtension(original);
            String filename = UUID.randomUUID().toString() + (ext != null && !ext.isBlank() ? "." + ext.toLowerCase() : "");

            Path dest = dir.resolve(filename);
            Files.createDirectories(dest.getParent());
            f.transferTo(dest.toFile());

            // ✅ 정적 루트(file:${app.upload.dir}/)와 1:1 매칭되는 URL
            String url = "/cafes/" + cafeId + "/" + filename;

            CafeImage entity = CafeImage.builder()
                    .cafe(cafe)
                    .imgUrl(url)
                    .build();

            saved.add(cafeImageRepository.save(entity));
        }

        return saved;
    }

    @Transactional
    public void deleteByIds(List<Long> ids) throws IOException {
        if (ids == null || ids.isEmpty()) return;

        List<CafeImage> images = cafeImageRepository.findAllById(ids);

        for (CafeImage img : images) {
            try {
                // 1) 파일 시스템에서 삭제
                String url = Optional.ofNullable(img.getImgUrl()).orElse(""); // 예: /cafes/12/uuid.jpg
                String relative = url.startsWith("/") ? url.substring(1) : url; // cafes/12/uuid.jpg

                // 파일명
                String filename = relative.lastIndexOf('/') >= 0 ? relative.substring(relative.lastIndexOf('/') + 1) : null;

                // cafeId (엔티티에 붙어있으면 그걸 우선 사용)
                Long cafeId = (img.getCafe() != null ? img.getCafe().getId() : null);
                if (cafeId == null) {
                    // URL에서 파싱 (cafes/{cafeId}/{filename})
                    String[] parts = relative.split("/");
                    if (parts.length >= 3 && "cafes".equals(parts[0])) {
                        try { cafeId = Long.parseLong(parts[1]); } catch (NumberFormatException ignore) {}
                    }
                }

                if (filename != null && cafeId != null) {
                    Path filePath = physicalRoot().resolve(String.valueOf(cafeId)).resolve(filename).normalize();
                    Path root = physicalRoot().normalize();

                    // 안전 가드(루트 밖 삭제 방지)
                    if (filePath.startsWith(root)) {
                        Files.deleteIfExists(filePath);
                    }
                }
            } catch (Exception e) {
                // 파일이 없거나 접근 문제는 경고만 남기고 계속 진행 (DB는 어쨌든 정리)
                // 필요시 log level 조정
                System.out.println("이미지 파일 삭제 경고: " + img.getImgUrl() + " / " + e.getMessage());
            }
        }

        // 2) DB에서 레코드 삭제
        cafeImageRepository.deleteAll(images);
    }

    /** 단건 삭제가 필요하면 편의 메서드도 함께 */
    @Transactional
    public void deleteById(Long id) throws IOException {
        if (id == null) return;
        deleteByIds(Collections.singletonList(id));
    }

}
