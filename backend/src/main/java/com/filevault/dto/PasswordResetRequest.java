package com.filevault.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequest {
    private String phoneNumber;
    private String otp;
    private String newPassword;
    private String role; // ADMIN or USER
}
