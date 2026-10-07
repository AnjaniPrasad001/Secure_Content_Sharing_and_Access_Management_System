package com.filevault.service;

import com.filevault.dto.PasswordResetRequest;
import com.filevault.dto.RegisterRequest;
import com.filevault.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class PasswordResetServiceTest {

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testPasswordResetFlow() throws Exception {
        String phone = "9876543210";
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email("resetuser@example.com")
                .password("OldPassword123!")
                .firstName("Reset")
                .lastName("User")
                .phoneNumber(phone)
                .build();
        authService.registerUser(registerRequest);

        Map<String, Object> otpRes = passwordResetService.requestPasswordResetOtp(phone, "USER");
        assertNotNull(otpRes.get("message"));

        // Retrieve the user from DB and test reset if OTP is valid
        // Password reset fails with invalid OTP:
        PasswordResetRequest badOtpReq = new PasswordResetRequest();
        badOtpReq.setPhoneNumber(phone);
        badOtpReq.setOtp("000000");
        badOtpReq.setNewPassword("NewPassword123!");
        badOtpReq.setRole("USER");

        assertThrows(RuntimeException.class, () -> passwordResetService.resetPassword(badOtpReq));
    }
}
