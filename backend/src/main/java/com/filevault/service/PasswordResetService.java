package com.filevault.service;

import com.filevault.dto.PasswordResetRequest;
import com.filevault.entity.Admin;
import com.filevault.entity.User;
import com.filevault.exception.ResourceNotFoundException;
import com.filevault.repository.AdminRepository;
import com.filevault.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@Transactional
public class PasswordResetService {
    
    @Autowired
    private AdminRepository adminRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private TwilioService twilioService;
    
    // In-memory OTP storage (phone -> OTP)
    // In production, use Redis or database with expiration
    private final Map<String, OtpEntry> otpStorage = new ConcurrentHashMap<>();
    
    private static final int OTP_LENGTH = 6;
    private static final long OTP_EXPIRY_TIME = 5 * 60 * 1000; // 5 minutes
    
    private static class OtpEntry {
        String otp;
        long expiryTime;
        
        OtpEntry(String otp) {
            this.otp = otp;
            this.expiryTime = System.currentTimeMillis() + OTP_EXPIRY_TIME;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
    
    /**
     * Request OTP for password reset
     * Validates that phone number exists and sends OTP
     */
    public Map<String, Object> requestPasswordResetOtp(String phoneNumber, String role) {
        // Validate phone number exists
        if ("ADMIN".equalsIgnoreCase(role)) {
            Admin admin = adminRepository.findByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> new RuntimeException(
                        "No admin account found with this phone number. Please update your profile with a phone number first."
                    ));
        } else if ("USER".equalsIgnoreCase(role)) {
            User user = userRepository.findByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> new RuntimeException(
                        "No user account found with this phone number. Please update your profile with a phone number first."
                    ));
        } else {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
        
        // Generate OTP
        String otp = generateOtp();
        
        // Store OTP
        otpStorage.put(phoneNumber, new OtpEntry(otp));
        
        // Send OTP via SMS (using TwilioService)
        try {
            log.info("Sending OTP to phone number: {}", phoneNumber);
            // twilioService.sendOtp(phoneNumber, otp); // Uncomment when Twilio is configured
            
            // For development/testing, log the OTP
            log.debug("OTP for phone {}: {}", phoneNumber, otp);
        } catch (Exception e) {
            log.error("Failed to send OTP: {}", e.getMessage());
            // Don't throw exception to avoid revealing phone number existence
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "OTP sent to your phone number. Valid for 5 minutes.");
        response.put("phoneNumber", maskPhoneNumber(phoneNumber));
        
        return response;
    }
    
    /**
     * Verify OTP and reset password
     */
    public Map<String, Object> resetPassword(PasswordResetRequest request) {
        // Check if OTP exists and is not expired
        OtpEntry otpEntry = otpStorage.get(request.getPhoneNumber());
        
        if (otpEntry == null) {
            throw new RuntimeException("OTP not requested for this phone number. Please request OTP first.");
        }
        
        if (otpEntry.isExpired()) {
            otpStorage.remove(request.getPhoneNumber());
            throw new RuntimeException("OTP has expired. Please request a new OTP.");
        }
        
        if (!otpEntry.otp.equals(request.getOtp())) {
            throw new RuntimeException("Invalid OTP. Please try again.");
        }
        
        // OTP is valid, reset password
        try {
            if ("ADMIN".equalsIgnoreCase(request.getRole())) {
                Admin admin = adminRepository.findByPhoneNumber(request.getPhoneNumber())
                        .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
                admin.setPassword(passwordEncoder.encode(request.getNewPassword()));
                adminRepository.save(admin);
            } else if ("USER".equalsIgnoreCase(request.getRole())) {
                User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                userRepository.save(user);
            } else {
                throw new IllegalArgumentException("Invalid role: " + request.getRole());
            }
            
            // Remove OTP after successful reset
            otpStorage.remove(request.getPhoneNumber());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Password reset successfully. You can now login with your new password.");
            
            return response;
        } catch (Exception e) {
            log.error("Password reset failed: {}", e.getMessage());
            throw new RuntimeException("Password reset failed: " + e.getMessage());
        }
    }
    
    /**
     * Verify OTP only (for future use in multi-step verification)
     */
    public Map<String, Object> verifyOtp(String phoneNumber, String otp) {
        OtpEntry otpEntry = otpStorage.get(phoneNumber);
        
        if (otpEntry == null) {
            throw new RuntimeException("OTP not requested for this phone number.");
        }
        
        if (otpEntry.isExpired()) {
            otpStorage.remove(phoneNumber);
            throw new RuntimeException("OTP has expired.");
        }
        
        if (!otpEntry.otp.equals(otp)) {
            throw new RuntimeException("Invalid OTP.");
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "OTP verified successfully");
        response.put("verified", true);
        
        return response;
    }
    
    /**
     * Generate random OTP
     */
    private String generateOtp() {
        Random random = new Random();
        int otp = random.nextInt((int) Math.pow(10, OTP_LENGTH));
        return String.format("%0" + OTP_LENGTH + "d", otp);
    }
    
    /**
     * Mask phone number for display
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        String last4 = phoneNumber.substring(phoneNumber.length() - 4);
        return "*".repeat(phoneNumber.length() - 4) + last4;
    }
}
