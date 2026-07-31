package com.filevault.controller;

import com.filevault.dto.FileResponse;
import com.filevault.dto.PaymentRequest;
import com.filevault.dto.PhoneNumberRequest;
import com.filevault.entity.Payment;
import com.filevault.entity.User;
import com.filevault.service.AccessControlService;
import com.filevault.service.FileService;
import com.filevault.service.PaymentService;
import com.filevault.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private FileService fileService;
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private AccessControlService accessControlService;
    
    @GetMapping("/profile/{userId}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long userId) {
        try {
            User user = userService.getUserById(userId);
            return new ResponseEntity<>(user, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Get user profile error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "User not found");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        }
    }
    
    @PutMapping("/profile/{userId}")
    public ResponseEntity<?> updateUserProfile(
            @PathVariable Long userId,
            @RequestBody Map<String, String> profileData) {
        
        try {
            User updatedUser = userService.updateUserProfile(
                    userId,
                    profileData.get("firstName"),
                    profileData.get("lastName"),
                    profileData.get("phoneNumber")
            );
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Update user profile error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update profile");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    @PutMapping("/phone-number/{userId}")
    public ResponseEntity<?> updateUserPhoneNumber(
            @PathVariable Long userId,
            @RequestBody PhoneNumberRequest phoneNumberRequest) {
        
        try {
            User updatedUser = userService.updateUserPhoneNumber(userId, phoneNumberRequest.getPhoneNumber());
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Phone number updated successfully");
            response.put("user", updatedUser);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.error("Invalid phone number: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid phone number format");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Update user phone number error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update phone number");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/{userId}/files")
    public ResponseEntity<?> getAvailableFiles(@PathVariable Long userId) {
        try {
            List<FileResponse> files = fileService.getAvailableFilesForUser(userId);
            return new ResponseEntity<>(files, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Get available files error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to retrieve files");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/{userId}/dashboard")
    public ResponseEntity<?> getUserDashboard(@PathVariable Long userId) {
        try {
            Map<String, Object> dashboard = new HashMap<>();
            User user = userService.getUserById(userId);
            dashboard.put("userInfo", user);
            dashboard.put("walletBalance", user.getWalletBalance());
            
            return new ResponseEntity<>(dashboard, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Get user dashboard error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to retrieve dashboard data");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/{userId}/wallet")
    public ResponseEntity<?> getWalletBalance(@PathVariable Long userId) {
        try {
            Double balance = userService.getWalletBalance(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("walletBalance", balance);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Get wallet balance error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to retrieve wallet balance");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam String q) {
        try {
            List<User> users = userService.searchUsers(q);
            return new ResponseEntity<>(users, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Search users error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to search users");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    @PostMapping("/{userId}/wallet/fund")
    public ResponseEntity<?> addWalletBalance(
            @PathVariable Long userId,
            @RequestBody Map<String, Double> fundData) {
        
        try {
            Double amount = fundData.get("amount");
            User updatedUser = userService.addWalletBalance(userId, amount);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Wallet funded successfully");
            response.put("newBalance", updatedUser.getWalletBalance());
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Add wallet balance error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fund wallet");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    @PostMapping("/{userId}/payment/purchase/{fileId}")
    public ResponseEntity<?> purchaseFile(
            @PathVariable Long userId,
            @PathVariable Long fileId,
            @RequestBody PaymentRequest paymentRequest) {
        
        try {
            Payment payment = paymentService.initiatePayment(fileId, userId, paymentRequest);
            
            // Simulate payment completion (in production, integrate with payment gateway)
            Payment completedPayment = paymentService.completePayment(payment.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "File purchased successfully");
            response.put("payment", completedPayment);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Purchase file error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to purchase file");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/{userId}/purchases")
    public ResponseEntity<?> getUserPurchases(@PathVariable Long userId) {
        try {
            List<Payment> payments = paymentService.getPaymentsByUser(userId);
            return new ResponseEntity<>(payments, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Get user purchases error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to retrieve purchases");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/{userId}/access")
    public ResponseEntity<?> getUserAccess(@PathVariable Long userId) {
        try {
            var accesses = accessControlService.getUserAccess(userId);
            return new ResponseEntity<>(accesses, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Get user access error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to retrieve access information");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{userId}/update-phone")
    public ResponseEntity<?> updatePhoneNumber(
            @PathVariable Long userId,
            @RequestBody com.filevault.dto.PhoneNumberRequest phoneRequest) {
        try {
            User updatedUser = userService.updateUserPhoneNumber(userId, phoneRequest.getPhoneNumber());
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Phone number updated successfully");
            response.put("phoneNumber", updatedUser.getPhoneNumber());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Update phone number error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update phone number");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/debug/all-files")
    public ResponseEntity<?> debugAllFiles() {
        try {
            List<Map<String, Object>> allFiles = new java.util.ArrayList<>();
            
            java.util.List<com.filevault.entity.File> files = fileService.getAllFilesDebug();
            for (com.filevault.entity.File file : files) {
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("id", file.getId());
                fileInfo.put("fileName", file.getFileName());
                fileInfo.put("originalFileName", file.getOriginalFileName());
                fileInfo.put("accessType", file.getAccessType().toString());
                fileInfo.put("adminId", file.getAdmin().getId());
                fileInfo.put("adminEmail", file.getAdmin().getEmail());
                fileInfo.put("categoryName", file.getCategory().getName());
                allFiles.add(fileInfo);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalFiles", allFiles.size());
            response.put("files", allFiles);
            
            log.info("Debug: Total files in database: {}", allFiles.size());
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Debug all files error: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
}
