package com.team.cafe.api; // 원하는 패키지로

import com.team.cafe.cafeListImg.hj.CafeImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.NoSuchElementException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage/cafe")
public class CafeImageApiController {

    private final CafeImageService cafeImageService; // ✅ 올바른 임포트 확인!

    @DeleteMapping("/photo/{photoId}")
    @Transactional
    public ResponseEntity<Void> deletePhoto(@PathVariable Long photoId) {
        try {
            log.info("DELETE photo {}", photoId);
            cafeImageService.deleteById(photoId); // ✅ IOException 처리됨
            return ResponseEntity.noContent().build(); // 204
        } catch (NoSuchElementException | IllegalArgumentException e) {
            log.warn("Photo not found: {}", photoId);
            return ResponseEntity.notFound().build(); // 404
        } catch (IOException e) {
            log.error("File delete failed for photo {}", photoId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        } catch (Exception e) {
            log.error("Unexpected error on delete photo {}", photoId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        }
    }
}
