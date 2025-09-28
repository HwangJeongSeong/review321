package com.team.cafe;

import com.team.cafe.businessuser.sj.BusinessUserRepository;
import com.team.cafe.list.hj.CafeListRepository;
import com.team.cafe.user.sjhy.SiteUser;
import com.team.cafe.user.sjhy.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final UserService userService;
    private final BusinessUserRepository businessUserRepository;
    private final CafeListRepository cafeListRepository;

    private SiteUser currentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object p = auth.getPrincipal();
        if ("anonymousUser".equals(p)) return null;
        String username = (p instanceof UserDetails ud) ? ud.getUsername() : String.valueOf(p);
        return userService.getUser(username);
    }

    @ModelAttribute("user")
    public SiteUser injectUser() {
        return currentUserOrNull();
    }

    @ModelAttribute
    public void injectBizCafeFlags(Model model) {
        SiteUser u = currentUserOrNull();
        boolean isBusiness = false;
        boolean hasCafe = false;
        Long cafeId = null;

        if (u != null) {
            var buOpt = businessUserRepository.findByUserId(u.getId());
            isBusiness = buOpt.isPresent();

            if (isBusiness) {
                var bu = buOpt.get();
                if (bu.getCafe() != null) {
                    hasCafe = true;
                    cafeId = bu.getCafe().getId();
                }
            }
        }
        model.addAttribute("isBusiness", isBusiness);
        model.addAttribute("hasCafe", hasCafe);
        model.addAttribute("cafeId", cafeId);
    }

}

