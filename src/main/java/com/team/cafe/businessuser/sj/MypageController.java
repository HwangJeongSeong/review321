package com.team.cafe.businessuser.sj;

import com.team.cafe.Menu.Menu;
import com.team.cafe.Menu.MenuService;
import com.team.cafe.bookmark.BookmarkRepository;
import com.team.cafe.cafeListImg.hj.CafeImageService;
import com.team.cafe.list.hj.Cafe;
import com.team.cafe.list.hj.CafeListRepository;
import com.team.cafe.list.hj.CafeListService;
import com.team.cafe.review.service.ReviewService;
import com.team.cafe.user.sjhy.SiteUser;
import com.team.cafe.user.sjhy.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Controller
public class MypageController {

    /* =========================
       Constants (Session Keys)
       ========================= */
    private static final String REAUTH_TOKEN_KEY = "MYPAGE_REAUTH_TOKEN";
    private static final String MANAGE_PASS_ONCE = "MANAGE_PASS_ONCE";

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final BusinessUserRepository businessUserRepository;
    private final CafeImageService cafeImageService;
    private final CafeListRepository cafeListRepository;
    private final CafeListService cafeListService;
    private final ReviewService reviewService;
    private final BookmarkRepository bookmarkRepository;
    private final MenuService menuService;

    /* =========================
       Reauth Token
       ========================= */
    private record ReauthToken(String nonce, String allowExactUrl) {}

    /* =========================
       Common helpers
       ========================= */
    private SiteUser currentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object p = auth.getPrincipal();
        if ("anonymousUser".equals(p)) return null;
        String username = (p instanceof UserDetails ud) ? ud.getUsername() : String.valueOf(p);
        return userService.getUser(username);
    }

    @ModelAttribute("user")
    public SiteUser injectUser() { return currentUserOrNull(); }

    private void setNoCache(HttpServletResponse res) {
        res.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        res.setHeader("Pragma", "no-cache");
        res.setHeader("Expires", "0");
    }

    private boolean isSafeInternalPath(String url) {
        try {
            URI u = URI.create(url);
            return !u.isAbsolute() && url.startsWith("/");
        } catch (Exception e) {
            return false;
        }
    }

    private String stripReauthParam(String internalUrl) {
        if (internalUrl == null || internalUrl.isBlank()) return internalUrl;
        String[] parts = internalUrl.split("\\?", 2);
        if (parts.length == 1) return internalUrl;

        String path = parts[0];
        String filtered = Arrays.stream(parts[1].split("&"))
                .filter(p -> !p.toLowerCase().startsWith("reauth="))
                .collect(Collectors.joining("&"));
        return filtered.isEmpty() ? path : (path + "?" + filtered);
    }

    private String normalizedCurrentWithoutReauth(HttpServletRequest req) {
        String rawPath = req.getRequestURI();
        String pathNoSemi = rawPath.split(";", 2)[0];
        if (pathNoSemi.length() > 1 && pathNoSemi.endsWith("/")) {
            pathNoSemi = pathNoSemi.substring(0, pathNoSemi.length() - 1);
        }
        String qs = req.getQueryString();
        if (qs != null && !qs.isEmpty()) {
            String filtered = Arrays.stream(qs.split("&"))
                    .filter(p -> !p.toLowerCase().startsWith("reauth="))
                    .collect(Collectors.joining("&"));
            return filtered.isEmpty() ? pathNoSemi : (pathNoSemi + "?" + filtered);
        }
        return pathNoSemi;
    }

    private String normalizeBasePath(HttpServletRequest req, String basePath) {
        String ctx = (req.getContextPath() == null) ? "" : req.getContextPath();
        String withoutSemicolon = basePath.split(";", 2)[0];

        int q = withoutSemicolon.indexOf('?');
        String path = (q >= 0) ? withoutSemicolon.substring(0, q) : withoutSemicolon;
        String query = (q >= 0) ? withoutSemicolon.substring(q) : "";

        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        if (!ctx.isEmpty() && path.startsWith(ctx)) {
            return path + query;
        }
        return ctx + path + query;
    }

    private ReauthToken getReauthToken(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) return null;
        Object o = s.getAttribute(REAUTH_TOKEN_KEY);
        return (o instanceof ReauthToken t) ? t : null;
    }
    private void clearReauthToken(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s != null) s.removeAttribute(REAUTH_TOKEN_KEY);
    }

    private boolean requireReauthConsume(HttpServletRequest req) {
        String nonceParam = req.getParameter("reauth");
        if (nonceParam == null || nonceParam.isBlank()) return false;
        ReauthToken t = getReauthToken(req);
        if (t == null) return false;

        String curr = normalizedCurrentWithoutReauth(req);
        if (!safeUrlEqualsIgnoreReauthAndCtx(req, t.allowExactUrl(), curr)) {
            return false;
        }
        if (!t.nonce().equals(nonceParam)) return false;

        clearReauthToken(req);
        return true;
    }

    private boolean safeUrlEqualsIgnoreReauthAndCtx(HttpServletRequest req, String a, String b) {
        try {
            String ctx = (req.getContextPath() == null) ? "" : req.getContextPath();

            UrlParts ua = parseAndNormalizeUrl(a, ctx);
            UrlParts ub = parseAndNormalizeUrl(b, ctx);

            if (!Objects.equals(ua.path, ub.path)) return false;
            return Objects.equals(ua.params, ub.params);
        } catch (Exception e) {
            return a.equals(b);
        }
    }

    private static class UrlParts {
        final String path;
        final Map<String, List<String>> params;
        UrlParts(String p, Map<String, List<String>> q) { this.path = p; this.params = q; }
    }

    private UrlParts parseAndNormalizeUrl(String raw, String ctx) {
        String noSemi = raw.split(";", 2)[0];

        String path, qs;
        int q = noSemi.indexOf('?');
        if (q >= 0) { path = noSemi.substring(0, q); qs = noSemi.substring(q + 1); }
        else { path = noSemi; qs = ""; }

        if (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);
        if (ctx != null && !ctx.isEmpty() && !path.startsWith(ctx)) {
            path = ctx + (path.startsWith("/") ? path : ("/" + path));
        }

        Map<String, List<String>> map = new TreeMap<>();
        if (!qs.isEmpty()) {
            for (String kv : qs.split("&")) {
                if (kv.isBlank()) continue;
                int idx = kv.indexOf('=');
                String k = idx >= 0 ? kv.substring(0, idx) : kv;
                String v = idx >= 0 ? kv.substring(idx + 1) : "";
                k = urlDecode(k);
                if ("reauth".equalsIgnoreCase(k)) continue;
                v = urlDecode(v);
                map.computeIfAbsent(k, _k -> new ArrayList<>()).add(v);
            }
        }
        for (List<String> vs : map.values()) Collections.sort(vs);
        return new UrlParts(path, map);
    }

    private String urlDecode(String s) {
        try {
            return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) { return s; }
    }

    private boolean consumeManagePassOnce(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) return false;
        Object flag = s.getAttribute(MANAGE_PASS_ONCE);
        if (!Boolean.TRUE.equals(flag)) return false;
        s.removeAttribute(MANAGE_PASS_ONCE);
        return true;
    }

    private String redirectToVerifyWithContinue(HttpServletRequest req) {
        String target = normalizedCurrentWithoutReauth(req);
        String encoded = URLEncoder.encode(target, StandardCharsets.UTF_8);
        return "redirect:/mypage/verify_password?continue=" + encoded;
    }

    /* =========================
       My Page - Main
       ========================= */
    @GetMapping("/mypage")
    public String mypage(HttpServletResponse response) {
        SiteUser user = currentUserOrNull();
        if (user == null) return "redirect:/user/login";
        setNoCache(response);
        return "mypage/mypage-main";
    }

    /* =========================
       Account (Protected by reauth)
       ========================= */
    @GetMapping("/mypage/account")
    public String accountPage(HttpServletRequest request, HttpServletResponse response) {
        SiteUser user = currentUserOrNull();
        if (user == null) return "redirect:/user/login";
        if (!requireReauthConsume(request)) return redirectToVerifyWithContinue(request);
        setNoCache(response);
        return "mypage/account";
    }



    @GetMapping("/mypage/verify_password")
    public String verifyPasswordPage(@RequestParam(value = "continue", required = false) String cont,
                                     @RequestParam(value = "error", required = false) String errorParam,
                                     HttpServletRequest request,
                                     HttpServletResponse response,
                                     Model model) {
        SiteUser user = currentUserOrNull();
        if (user == null) return "redirect:/user/login";

        // 1) 이 페이지에서만큼은 전/타 흐름의 잔여 인증 예외 및 범용 error 키 무시
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(REAUTH_TOKEN_KEY);
            session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION); // SPRING_SECURITY_LAST_EXCEPTION 제거
        }
        // (레이아웃/인터셉터 등이 model에 얹어놓은 범용 error 키 무력화)
        model.addAttribute("error", null);

        setNoCache(response);
        model.addAttribute("continueUrl", cont);

        // 2) 비번 검증 실패 상황에서만 전용 키로 메시지 세팅
        if (errorParam != null) {
            model.addAttribute("verifyPwError", "비밀번호가 올바르지 않습니다.");
        }

        return "mypage/verify_password";
    }


    @PostMapping("/mypage/verify_password")
    public String verifyPassword(@RequestParam String password,
                                 @RequestParam(value = "continue", required = false) String cont,
                                 HttpServletRequest request,
                                 RedirectAttributes ra) {
        SiteUser user = currentUserOrNull();
        if (user == null) return "redirect:/user/login";

        if (!passwordEncoder.matches(password, user.getPassword())) {
            // 전용 키만 사용
            ra.addFlashAttribute("verifyPwError", "비밀번호가 올바르지 않습니다.");
            if (cont != null) ra.addFlashAttribute("continueUrl", cont);
            return "redirect:/mypage/verify_password";
        }

        String base = (cont != null && isSafeInternalPath(cont)) ? cont : "/mypage/account";
        String baseNoReauth = stripReauthParam(base);
        String allowExactUrl = normalizeBasePath(request, baseNoReauth);

        String nonce = UUID.randomUUID().toString();
        request.getSession(true).setAttribute(REAUTH_TOKEN_KEY, new ReauthToken(nonce, allowExactUrl));

        String sep = (baseNoReauth.contains("?")) ? "&" : "?";
        return "redirect:" + baseNoReauth + sep + "reauth=" + URLEncoder.encode(nonce, StandardCharsets.UTF_8);
    }


    @PostMapping("/mypage/account/update")
    @Transactional
    public String updateAccount(@RequestParam String nickname,
                                @RequestParam String email,
                                HttpServletRequest request) {
        SiteUser user = currentUserOrNull();
        if (user == null) return "redirect:/user/login";


        // 1) SiteUser 저장
        user.setNickname(nickname);
        user.setEmail(email);
        userService.save(user);

        // 2) BusinessUser 동기화
        businessUserRepository.findByUserId(user.getId()).ifPresent(biz -> {
            biz.setRepresentativeName(nickname);
            biz.setRepresentativeEmail(email);
            businessUserRepository.save(biz);
        });

        return "redirect:/mypage";
    }


    @PostMapping("/mypage/account/update-password")
    @ResponseBody
    public String updatePassword(HttpServletRequest request,
                                 @RequestParam String newPassword) throws ServletException {
        SiteUser user = currentUserOrNull();
        if (user == null) return "fail";


        user.setPassword(passwordEncoder.encode(newPassword));
        userService.save(user);
        request.logout();
        return "success";
    }

    @PostMapping("/mypage/account/update-phone")
    @ResponseBody
    @Transactional
    public String updatePhone(@AuthenticationPrincipal UserDetails principal,
                              @RequestParam String phone) {
        if (principal == null) return "fail"; // 필요시 401로 변경
        SiteUser user = userService.getUser(principal.getUsername());

        if (user == null) return "fail";

        user.setPhone(phone);
        userService.save(user);

        // 2) 비즈니스 유저 대표번호도 함께 업데이트
        businessUserRepository.findByUserId(user.getId()).ifPresent(biz -> {
            biz.setRepresentativePhone(phone);
            businessUserRepository.save(biz); // ← 엔티티 말고 레포지토리로 save
        });




        return "success";
    }

    /* =========================
       Cafe Manage (Protected)
       ========================= */
    @GetMapping("/mypage/cafe/manage")
    public String cafeManage(HttpServletRequest request,
                             HttpServletResponse response,
                             @RequestParam(name = "cafeId", required = false) Long cafeId,
                             Model model) {
        SiteUser user = currentUserOrNull();
        if (user == null) return "redirect:/user/login";

        // 등록 직후 1회 패스 소비 → 없으면 재인증 필요
        if (!consumeManagePassOnce(request)) {
            if (!requireReauthConsume(request)) {
                return redirectToVerifyWithContinue(request);
            }
        }
        setNoCache(response);

        // 사업자 정보
        var business = businessUserRepository.findByUserId(user.getId()).orElse(null);
        model.addAttribute("business", business);

        // 1) 볼 카페 결정
        if (cafeId == null) {
            var businessUser = businessUserRepository.findByUserId(user.getId()).orElse(null);
            if (business != null && business.getCafe() != null) {
                cafeId = business.getCafe().getId();
            } else {
                // (보조) 그래도 없으면 가장 최근 카페 1개
                Cafe latest = cafeListRepository.findTopByOrderByIdDesc();
                cafeId = (latest != null) ? latest.getId() : null;
            }
        }

        // 2) 카페/사진 로드
        if (cafeId != null) {
            Cafe cafe = cafeListRepository.findById(cafeId).orElse(null);
            if (cafe != null) {
                var photos = cafeImageService.findAllByCafeId(cafeId);
                model.addAttribute("cafe", cafe);
                model.addAttribute("photos", photos);
                model.addAttribute("photoCount", photos.size());
                model.addAttribute("isRegistered", true);
            } else {
                model.addAttribute("photos", List.of());
                model.addAttribute("photoCount", 0);
                model.addAttribute("isRegistered", false);
            }
        } else {
            model.addAttribute("photos", List.of());
            model.addAttribute("photoCount", 0);
            model.addAttribute("isRegistered", false);
        }

        if (cafeId != null) {
            double avgRating = cafeListService.getActiveAverageRating(cafeId);
            long reviewCount = cafeListService.getActiveReviewCount(cafeId);

            var pageable = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "createdAt"));
            var recentPage = reviewService.getActiveReviewsByCafeWithUserImages(cafeId, pageable, null);
            var recentReviews = recentPage.getContent();

            model.addAttribute("avgRating", avgRating);
            model.addAttribute("reviewCount", reviewCount);
            model.addAttribute("recentReviews", recentReviews);
        } else {
            model.addAttribute("avgRating", null);
            model.addAttribute("reviewCount", 0);
            model.addAttribute("recentReviews", java.util.List.of());
        }

        if (cafeId != null) {

            long bookmarkCount = bookmarkRepository.countByCafe_Id(cafeId);
            model.addAttribute("bookmarkCount", bookmarkCount);

        } else {
            model.addAttribute("bookmarkCount", 0L);
        }

        try {
            List<Menu> allMenus = menuService.findForDetail(cafeId);  // 정렬: sortOrder→name
            List<Menu> menusLimited = (allMenus != null && allMenus.size() > 5)
                    ? allMenus.subList(0, 5)
                    : allMenus;
            model.addAttribute("menusLimited", menusLimited);
        } catch (Exception e) {
            model.addAttribute("menusLimited", java.util.List.of());
        }

        return "mypage/cafe_manage";
    }
}
