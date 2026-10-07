package com.filevault.controller;

import com.filevault.dto.chat.ChatMessageRequest;
import com.filevault.dto.chat.ChatMessageResponse;
import com.filevault.entity.Admin;
import com.filevault.repository.AdminRepository;
import com.filevault.service.chat.AdminChatbotService;
import com.filevault.service.chat.PublicChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private PublicChatbotService publicChatbotService;

    @Autowired
    private AdminChatbotService adminChatbotService;

    @Autowired
    private AdminRepository adminRepository;

    @PostMapping("/public")
    public ResponseEntity<ChatMessageResponse> processPublicChat(@RequestBody ChatMessageRequest request) {
        ChatMessageResponse response = publicChatbotService.processPublicQuery(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> processAdminChat(@RequestBody ChatMessageRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Admin authentication required");
        }

        Admin admin = adminRepository.findByEmail(auth.getName()).orElse(null);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin identity not found");
        }

        ChatMessageResponse response = adminChatbotService.processAdminQuery(admin.getId(), request);
        return ResponseEntity.ok(response);
    }
}
