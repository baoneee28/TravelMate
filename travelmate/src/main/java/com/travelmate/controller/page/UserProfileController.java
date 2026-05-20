package com.travelmate.controller.page;

import com.travelmate.entity.User;
import com.travelmate.repository.UserRepository;
import com.travelmate.security.CustomUserDetails;
import com.travelmate.service.FileStorageService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserProfileController {

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public UserProfileController(UserRepository userRepository, FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", user);
        return "user/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String fullName,
            @RequestParam(required = false, defaultValue = "") String phone,
            @RequestParam(required = false) MultipartFile avatarFile,
            RedirectAttributes ra) {
        User user = userRepository.findByEmail(userDetails.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String trimmedName = fullName.trim();
        if (!trimmedName.isEmpty()) {
            user.setName(trimmedName);
            user.setShortName(trimmedName);
        }
        user.setPhone(phone.trim().isEmpty() ? null : phone.trim());

        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                String url = fileStorageService.storeAvatar(avatarFile);
                if (url != null) user.setAvatarUrl(url);
            } catch (Exception e) {
                ra.addFlashAttribute("profileError", "Không thể tải ảnh lên: " + e.getMessage());
                return "redirect:/profile";
            }
        }

        userRepository.save(user);

        // Refresh SecurityContext để header phản ánh thay đổi ngay (tên, avatar)
        CustomUserDetails refreshed = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                refreshed, userDetails.getPassword(), refreshed.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        ra.addFlashAttribute("profileSuccess", true);
        return "redirect:/profile";
    }
}
