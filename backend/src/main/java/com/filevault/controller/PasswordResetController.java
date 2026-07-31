package com.filevault.controller;

import com.filevault.dto.OtpVerificationRequest;
import com.filevault.dto.PasswordResetRequest;
import com.filevault.dto.PhoneNumberRequest;
import com.filevault.service.PasswordResetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/password-reset")
public class PasswordResetController {
    
    @Autowired
    private PasswordResetService passwordResetService;
    
    /**
     * Request OTP for password reset
     * POST /api/password-reset/request-otp
     * Body: { "phoneNumber": "+919876543210", "role": "ADMIN" or "USER" }
     */
    @PostMapping("/request-otp")
    public ResponseEntity<?> requestPasswordResetOtp(@RequestBody Map<String, String> request) {
        try {
            String phoneNumber = request.get("phoneNumber");
            String role = request.get("role");
            
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return new ResponseEntity<>(
                    createErrorResponse("Phone number is required"),
                    HttpStatus.BAD_REQUEST
                );
            }
            
            if (role == null || role.trim().isEmpty()) {
                return new ResponseEntity<>(
                    createErrorResponse("Role (ADMIN/USER) is required"),
                    HttpStatus.BAD_REQUEST
                );
            }
            
            Map<String, Object> response = passwordResetService.requestPasswordResetOtp(phoneNumber, role);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            log.error("Request OTP error: {}", e.getMessage());
            return new ResponseEntity<>(
                createErrorResponse(e.getMessage()),
                HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            log.error("Request OTP error: {}", e.getMessage());
            return new ResponseEntity<>(
                createErrorResponse("Failed to request OTP: " + e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    /**
     * Verify OTP
     * POST /api/password-reset/verify-otp
     * Body: { "phoneNumber": "+919876543210", "otp": "123456", "role": "ADMIN" or "USER" }
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpVerificationRequest request) {
        try {
            if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
                return new ResponseEntity<>(
                    createErrorResponse("Phone number is required"),
                    HttpStatus.BAD_REQUEST
                );
            }
            
            if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
                return new ResponseEntity<>(
                    createErrorResponse("OTP is required"),
                    HttpStatus.BAD_REQUEST
                );
            }
            
            Map<String, Object> response = passwordResetService.verifyOtp(
                request.getPhoneNumber(),
                request.getOtp()
            );
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            log.error("Verify OTP error: {}", e.getMessage());
            return new ResponseEntity<>(
                createErrorResponse(e.getMessage()),
                HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            log.error("Verify OTP error: {}", e.getMessage());
            return new ResponseEntity<>(
                createErrorResponse("Failed to verify OTP: " + e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    /**
     * Reset password with OTP
     * POST /api/password-reset/reset-password
     * Body: { 
     *   "phoneNumber": "+919876543210", 
     *   "otp": "123456",
     *   "newPassword": "newPassword123",
     *   "role": "ADMIN" or "USER" 
     * }
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetRequest request) {
        try {
            if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
                return new ResponseEntity<>(
                    createErrorResponse("Phone number is required"),
                    HttpStatus.BAD_REQUEST
                );
            }
            
            if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
                return new ResponseEntity<>(
                    createErrorResponse("OTP is required"),
                    HttpStatus.BAD_REQUEST
                );
            }
            
            if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
                return new ResponseEntity<>(
                    createErrorResponse("New password must be at least 6 characters"),
                    HttpStatus.BAD_REQUEST
                );
            }
            
            if (request.getRole() == null || request.getRole().trim().isEmpty()) {
                return new ResponseEntity<>(
                    createErrorResponse("Role (ADMIN/USER) is required"),
                    HttpStatus.BAD_REQUEST
                );
            }
            
            Map<String, Object> response = passwordResetService.resetPassword(request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            log.error("Reset password error: {}", e.getMessage());
            return new ResponseEntity<>(
                createErrorResponse(e.getMessage()),
                HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            log.error("Reset password error: {}", e.getMessage());
            return new ResponseEntity<>(
                createErrorResponse("Password reset failed: " + e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    /**
     * Helper method to create error response
     */
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Password reset failed");
        error.put("message", message);
        return error;
    }
}
