package com.filevault.controller;

import com.filevault.dto.chat.ChartDataDto;
import com.filevault.entity.Admin;
import com.filevault.repository.AdminRepository;
import com.filevault.service.chat.AdminChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

    @Autowired
    private AdminChatbotService adminChatbotService;

    @Autowired
    private AdminRepository adminRepository;

    @GetMapping("/overview")
    public ResponseEntity<?> getOverviewAnalytics() {
        Admin admin = getAuthenticatedAdmin();
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Admin authentication required");
        }
        var response = adminChatbotService.processAdminQuery(admin.getId(), 
                com.filevault.dto.chat.ChatMessageRequest.builder().message("Show analytics overview").build());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/time-series")
    public ResponseEntity<?> getTimeSeriesAnalytics(@RequestParam(defaultValue = "daily") String type) {
        Admin admin = getAuthenticatedAdmin();
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Admin authentication required");
        }
        boolean isCategory = "category".equalsIgnoreCase(type);
        var response = adminChatbotService.processAdminQuery(admin.getId(), 
                com.filevault.dto.chat.ChatMessageRequest.builder().message(isCategory ? "Show category graph" : "Show trend graph").build());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/content-performance")
    public ResponseEntity<?> getContentPerformance() {
        Admin admin = getAuthenticatedAdmin();
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Admin authentication required");
        }
        var response = adminChatbotService.processAdminQuery(admin.getId(), 
                com.filevault.dto.chat.ChatMessageRequest.builder().message("Show content performance").build());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/engagement")
    public ResponseEntity<?> getEngagementAnalytics() {
        Admin admin = getAuthenticatedAdmin();
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Admin authentication required");
        }
        var response = adminChatbotService.processAdminQuery(admin.getId(), 
                com.filevault.dto.chat.ChatMessageRequest.builder().message("Compare my content performance this month and last month").build());
        return ResponseEntity.ok(response);
    }

    private Admin getAuthenticatedAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return adminRepository.findByEmail(auth.getName()).orElse(null);
    }
}
