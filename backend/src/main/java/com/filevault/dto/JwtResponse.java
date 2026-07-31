package com.filevault.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtResponse {
    
    private String token;
    private String type = "Bearer";
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String role; // ADMIN or USER
    private String message;
    private String phoneNumberWarning; // Warning message if phone number is missing
}
