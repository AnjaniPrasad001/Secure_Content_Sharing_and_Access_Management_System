package com.filevault.service;

import com.filevault.dto.JwtResponse;
import com.filevault.dto.LoginRequest;
import com.filevault.dto.RegisterRequest;
import com.filevault.entity.Admin;
import com.filevault.entity.User;
import com.filevault.repository.AdminRepository;
import com.filevault.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Test
    void testRegisterAndLoginUser() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email("testuser@example.com")
                .password("Password123!")
                .firstName("Test")
                .lastName("User")
                .phoneNumber("1234567890")
                .build();

        JwtResponse regResponse = authService.registerUser(registerRequest);
        assertNotNull(regResponse.getToken());
        assertEquals("USER", regResponse.getRole());
        assertEquals("testuser@example.com", regResponse.getEmail());

        LoginRequest loginRequest = LoginRequest.builder()
                .email("testuser@example.com")
                .password("Password123!")
                .build();

        JwtResponse loginResponse = authService.loginUser(loginRequest);
        assertNotNull(loginResponse.getToken());
        assertEquals("USER", loginResponse.getRole());
    }

    @Test
    void testRegisterAndLoginAdmin() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email("testadmin@example.com")
                .password("Password123!")
                .firstName("Test")
                .lastName("Admin")
                .phoneNumber("0987654321")
                .build();

        JwtResponse regResponse = authService.registerAdmin(registerRequest);
        assertNotNull(regResponse.getToken());
        assertEquals("ADMIN", regResponse.getRole());
        assertEquals("testadmin@example.com", regResponse.getEmail());

        LoginRequest loginRequest = LoginRequest.builder()
                .email("testadmin@example.com")
                .password("Password123!")
                .build();

        JwtResponse loginResponse = authService.loginAdmin(loginRequest);
        assertNotNull(loginResponse.getToken());
        assertEquals("ADMIN", loginResponse.getRole());
    }
}
