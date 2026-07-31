package com.filevault.controller;

import com.filevault.dto.AccessGrantRequest;
import com.filevault.dto.FileResponse;
import com.filevault.dto.PhoneNumberRequest;
import com.filevault.entity.AccessControl;
import com.filevault.entity.Admin;
import com.filevault.service.AccessControlService;
import com.filevault.service.AdminService;
import com.filevault.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    @Autowired
    private AdminService adminService;
    
    @Autowired
    private AccessControlService accessControlService;
    
    @Autowired
    private PaymentService paymentService;
    
    @GetMapping("/profile/{adminId}")
    public ResponseEntity<?> getAdminProfile(@PathVariable Long adminId) {
        try {
            Admin admin = adminService.getAdminById(adminId);
            return new ResponseEntity<>(admin, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Get admin profile error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Admin not found");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        }
    }

    // Backwards-compatible alias: allow GET /api/admin/{adminId} to return profile
    @GetMapping("/{adminId}")
    public ResponseEntity<?> getAdminProfileAlias(@PathVariable Long adminId) {
        try {
            Admin admin = adminService.getAdminById(adminId);
            return new ResponseEntity<>(admin, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Get admin profile (alias) error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Admin not found");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        }
    }
    
    @PutMapping("/profile/{adminId}")
    public ResponseEntity<?> updateAdminProfile(
            @PathVariable Long adminId,
            @RequestBody Map<String, String> profileData) {
        
        try {
            Admin updatedAdmin = adminService.updateAdminProfile(
                    adminId,
                    profileData.get("firstName"),
                    profileData.get("lastName"),
                    profileData.get("phoneNumber")
            );
            return new ResponseEntity<>(updatedAdmin, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Update admin profile error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update profile");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    @PutMapping("/phone-number/{adminId}")
    public ResponseEntity<?> updateAdminPhoneNumber(
            @PathVariable Long adminId,
            @RequestBody PhoneNumberRequest phoneNumberRequest) {
        
        try {
            Admin updatedAdmin = adminService.updateAdminPhoneNumber(adminId, phoneNumberRequest.getPhoneNumber());
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Phone number updated successfully");
            response.put("admin", updatedAdmin);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.error("Invalid phone number: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid phone number format");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Update admin phone number error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update phone number");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/{adminId}/files")
    public ResponseEntity<?> getAdminFiles(@PathVariable Long adminId) {
        try {
            List<FileResponse> files = adminService.getAdminFiles(adminId);
            return new ResponseEntity<>(files, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Get admin files error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to retrieve files");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/{adminId}/dashboard")
    public ResponseEntity<?> getAdminDashboard(@PathVariable Long adminId) {
        try {
            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("totalEarnings", adminService.getTotalEarnings(adminId));
            dashboard.put("totalFilesUploaded", adminService.getTotalFilesUploaded(adminId));
            dashboard.put("totalAccessesByAdmin", adminService.getTotalAccessesByAdmin(adminId));
            
            return new ResponseEntity<>(dashboard, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Get admin dashboard error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to retrieve dashboard data");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    @PostMapping("/{adminId}/access/grant")
    public ResponseEntity<?> grantAccess(
            @PathVariable Long adminId,
            @RequestBody AccessGrantRequest accessGrantRequest) {
        
        try {
            AccessControl accessControl = accessControlService.grantAccess(
                    accessGrantRequest.getFileId(),
                    accessGrantRequest.getUserId(),
                    adminId
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Access granted successfully");
            response.put("accessControl", accessControl);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Grant access error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to grant access");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    @PostMapping("/{adminId}/access/revoke")
    public ResponseEntity<?> revokeAccess(
            @PathVariable Long adminId,
            @RequestBody AccessGrantRequest accessGrantRequest) {
        
        try {
            accessControlService.revokeAccess(
                    accessGrantRequest.getFileId(),
                    accessGrantRequest.getUserId(),
                    adminId
            );
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Access revoked successfully");
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Revoke access error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to revoke access");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/{adminId}/file/{fileId}/access")
    public ResponseEntity<?> getFileAccess(
            @PathVariable Long adminId,
            @PathVariable Long fileId) {
        
        try {
            List<AccessControl> accesses = accessControlService.getAccessesForFile(fileId, adminId);
            return new ResponseEntity<>(accesses, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Get file access error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to retrieve access information");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/{adminId}/earnings")
    public ResponseEntity<?> getEarnings(@PathVariable Long adminId) {
        try {
            Double earnings = adminService.getTotalEarnings(adminId);
            Map<String, Object> response = new HashMap<>();
            response.put("totalEarnings", earnings);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Get earnings error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to retrieve earnings");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/{adminId}/debug/files")
    public ResponseEntity<?> debugGetFiles(@PathVariable Long adminId) {
        try {
            Map<String, Object> debug = new HashMap<>();
            
            // Get the current admin being queried
            Admin admin = adminService.getAdminById(adminId);
            debug.put("queriedAdminId", adminId);
            debug.put("queriedAdminEmail", admin.getEmail());
            
            // Get files for this admin
            List<FileResponse> files = adminService.getAdminFiles(adminId);
            debug.put("filesCount", files.size());
            debug.put("files", files);
            
            // Log database details
            log.info("DEBUG: Admin {} has {} files", adminId, files.size());
            
            return new ResponseEntity<>(debug, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Debug files error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Debug operation failed");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{adminId}/debug/auth")
    public ResponseEntity<?> debugAuth(@PathVariable Long adminId) {
        try {
            Map<String, Object> info = new HashMap<>();
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) {
                info.put("authenticated", false);
            } else {
                info.put("authenticated", auth.isAuthenticated());
                info.put("principal", auth.getPrincipal());
                info.put("name", auth.getName());
                info.put("authorities", auth.getAuthorities());
            }
            return new ResponseEntity<>(info, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Debug auth error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Debug auth failed");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
}
