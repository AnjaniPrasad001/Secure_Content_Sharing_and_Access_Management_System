package com.filevault.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerificationRequest {
    private String phoneNumber;
    private String otp;
    private String role; // ADMIN or USER
}
