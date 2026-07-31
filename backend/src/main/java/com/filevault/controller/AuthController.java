package com.filevault.controller;

import com.filevault.dto.JwtResponse;
import com.filevault.dto.LoginRequest;
import com.filevault.dto.RegisterRequest;
import com.filevault.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @PostMapping("/admin/login")
    public ResponseEntity<?> loginAdmin(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            JwtResponse response = authService.loginAdmin(loginRequest);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Admin login error: {}", e.getMessage());
            return new ResponseEntity<>(
                    new JwtResponse(null, "Bearer", null, null, null, null, null, "ADMIN", e.getMessage(), null),
                    HttpStatus.UNAUTHORIZED
            );
        }
    }
    
    @PostMapping("/admin/register")
    public ResponseEntity<?> registerAdmin(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            JwtResponse response = authService.registerAdmin(registerRequest);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Admin registration error: {}", e.getMessage());
            return new ResponseEntity<>(
                    new JwtResponse(null, "Bearer", null, null, null, null, null, "ADMIN", e.getMessage(), null),
                    HttpStatus.BAD_REQUEST
            );
        }
    }
    
    @PostMapping("/user/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            JwtResponse response = authService.loginUser(loginRequest);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("User login error: {}", e.getMessage());
            return new ResponseEntity<>(
                    new JwtResponse(null, "Bearer", null, null, null, null, null, "USER", e.getMessage(), null),
                    HttpStatus.UNAUTHORIZED
            );
        }
    }
    
    @PostMapping("/user/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            JwtResponse response = authService.registerUser(registerRequest);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("User registration error: {}", e.getMessage());
            return new ResponseEntity<>(
                    new JwtResponse(null, "Bearer", null, null, null, null, null, "USER", e.getMessage(), null),
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
