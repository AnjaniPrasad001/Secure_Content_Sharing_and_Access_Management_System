package com.filevault.controller;

import com.filevault.entity.*;
import com.filevault.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class EngagementController {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private FileViewEventRepository fileViewEventRepository;

    @Autowired
    private FileLikeRepository fileLikeRepository;

    @Autowired
    private AdminSubscriptionRepository adminSubscriptionRepository;

    @PostMapping("/files/{fileId}/view-event")
    public ResponseEntity<?> recordViewEvent(@PathVariable Long fileId, HttpServletRequest httpRequest) {
        Optional<File> fileOpt = fileRepository.findById(fileId);
        if (fileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        File file = fileOpt.get();
        file.setViewCount(file.getViewCount() + 1);
        fileRepository.save(file);

        Long currentUserId = getCurrentUserId();
        String clientIp = httpRequest.getRemoteAddr();

        FileViewEvent event = FileViewEvent.builder()
                .file(file)
                .userId(currentUserId)
                .viewerIp(clientIp)
                .build();
        fileViewEventRepository.save(event);

        Map<String, Object> resp = new HashMap<>();
        resp.put("fileId", fileId);
        resp.put("viewCount", file.getViewCount());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/files/{fileId}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long fileId) {
        User user = getAuthenticatedUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User authentication required to like content");
        }
        Optional<File> fileOpt = fileRepository.findById(fileId);
        if (fileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        File file = fileOpt.get();

        Optional<FileLike> existingLike = fileLikeRepository.findByFileIdAndUserId(fileId, user.getId());
        boolean isLiked;
        if (existingLike.isPresent()) {
            fileLikeRepository.delete(existingLike.get());
            isLiked = false;
        } else {
            FileLike like = FileLike.builder()
                    .file(file)
                    .user(user)
                    .build();
            fileLikeRepository.save(like);
            isLiked = true;
        }

        Long totalLikes = fileLikeRepository.countByFileId(fileId);

        Map<String, Object> resp = new HashMap<>();
        resp.put("fileId", fileId);
        resp.put("liked", isLiked);
        resp.put("likesCount", totalLikes);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/admin/{adminId}/subscribe")
    public ResponseEntity<?> toggleSubscribe(@PathVariable Long adminId) {
        User user = getAuthenticatedUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User authentication required to subscribe");
        }
        Optional<Admin> adminOpt = adminRepository.findById(adminId);
        if (adminOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Optional<AdminSubscription> existingSub = adminSubscriptionRepository.findByAdminIdAndUserId(adminId, user.getId());
        boolean isSubscribed;
        if (existingSub.isPresent()) {
            adminSubscriptionRepository.delete(existingSub.get());
            isSubscribed = false;
        } else {
            AdminSubscription sub = AdminSubscription.builder()
                    .admin(adminOpt.get())
                    .user(user)
                    .build();
            adminSubscriptionRepository.save(sub);
            isSubscribed = true;
        }

        Long totalSubs = adminSubscriptionRepository.countByAdminId(adminId);

        Map<String, Object> resp = new HashMap<>();
        resp.put("adminId", adminId);
        resp.put("subscribed", isSubscribed);
        resp.put("subscriberCount", totalSubs);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/files/{fileId}/like/status")
    public ResponseEntity<?> getLikeStatus(@PathVariable Long fileId) {
        User user = getAuthenticatedUser();
        boolean isLiked = false;
        if (user != null) {
            isLiked = fileLikeRepository.existsByFileIdAndUserId(fileId, user.getId());
        }
        Long likesCount = fileLikeRepository.countByFileId(fileId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("liked", isLiked);
        resp.put("likesCount", likesCount);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/admin/{adminId}/subscribe/status")
    public ResponseEntity<?> getSubscriptionStatus(@PathVariable Long adminId) {
        User user = getAuthenticatedUser();
        boolean isSubscribed = false;
        if (user != null) {
            isSubscribed = adminSubscriptionRepository.existsByAdminIdAndUserId(adminId, user.getId());
        }
        Long subscriberCount = adminSubscriptionRepository.countByAdminId(adminId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("subscribed", isSubscribed);
        resp.put("subscriberCount", subscriberCount);
        return ResponseEntity.ok(resp);
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    private Long getCurrentUserId() {
        User u = getAuthenticatedUser();
        return u != null ? u.getId() : null;
    }
}
