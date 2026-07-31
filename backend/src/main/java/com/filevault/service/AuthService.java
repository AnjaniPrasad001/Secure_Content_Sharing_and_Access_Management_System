package com.filevault.service;

import com.filevault.dto.JwtResponse;
import com.filevault.dto.LoginRequest;
import com.filevault.dto.RegisterRequest;
import com.filevault.entity.Admin;
import com.filevault.entity.User;
import com.filevault.exception.ResourceNotFoundException;
import com.filevault.repository.AdminRepository;
import com.filevault.repository.UserRepository;
import com.filevault.security.JwtProvider;
import com.filevault.util.PhoneNumberValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class AuthService {
    
    @Autowired
    private AdminRepository adminRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtProvider jwtProvider;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    public JwtResponse loginAdmin(LoginRequest loginRequest) throws Exception {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );
            
            String token = jwtProvider.generateToken(authentication);
            Admin admin = adminRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
            
            // Check if phone number is missing
            String phoneNumberWarning = null;
            if (admin.getPhoneNumber() == null || admin.getPhoneNumber().isEmpty()) {
                phoneNumberWarning = "Phone number missing. Please update your profile to enable OTP password recovery.";
            }
            
            return JwtResponse.builder()
                    .token(token)
                    .id(admin.getId())
                    .email(admin.getEmail())
                    .firstName(admin.getFirstName())
                    .lastName(admin.getLastName())
                    .phoneNumber(admin.getPhoneNumber())
                    .role("ADMIN")
                    .message("Admin login successful")
                    .phoneNumberWarning(phoneNumberWarning)
                    .build();
        } catch (Exception e) {
            log.error("Admin login failed: {}", e.getMessage());
            throw new RuntimeException("Login failed. Invalid email or password");
        }
    }
    
    public JwtResponse loginUser(LoginRequest loginRequest) throws Exception {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );
            
            String token = jwtProvider.generateToken(authentication);
            User user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            
            // Check if phone number is missing
            String phoneNumberWarning = null;
            if (user.getPhoneNumber() == null || user.getPhoneNumber().isEmpty()) {
                phoneNumberWarning = "Phone number missing. Please update your profile to enable OTP password recovery.";
            }
            
            return JwtResponse.builder()
                    .token(token)
                    .id(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .phoneNumber(user.getPhoneNumber())
                    .role("USER")
                    .message("User login successful")
                    .phoneNumberWarning(phoneNumberWarning)
                    .build();
        } catch (Exception e) {
            log.error("User login failed: {}", e.getMessage());
            throw new RuntimeException("Login failed. Invalid email or password");
        }
    }
    
    public JwtResponse registerAdmin(RegisterRequest registerRequest) throws Exception {
        // Check if admin already exists
        if (adminRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Admin with this email already exists");
        }
        
        // Validate phone number for new registration
        PhoneNumberValidator.validateOrThrow(registerRequest.getPhoneNumber());
        
        // Check if phone number is already registered
        if (adminRepository.existsByPhoneNumber(registerRequest.getPhoneNumber())) {
            throw new RuntimeException("Phone number is already registered");
        }
        
        Admin admin = Admin.builder()
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .phoneNumber(registerRequest.getPhoneNumber())
                .isActive(true)
                .build();
        
        admin = adminRepository.save(admin);
        
        String token = jwtProvider.generateTokenFromUsername(admin.getEmail());
        
        return JwtResponse.builder()
                .token(token)
                .id(admin.getId())
                .email(admin.getEmail())
                .firstName(admin.getFirstName())
                .lastName(admin.getLastName())
                .phoneNumber(admin.getPhoneNumber())
                .role("ADMIN")
                .message("Admin registration successful")
                .build();
    }
    
    public JwtResponse registerUser(RegisterRequest registerRequest) throws Exception {
        // Check if user already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("User with this email already exists");
        }
        
        // Validate phone number for new registration
        PhoneNumberValidator.validateOrThrow(registerRequest.getPhoneNumber());
        
        // Check if phone number is already registered
        if (userRepository.existsByPhoneNumber(registerRequest.getPhoneNumber())) {
            throw new RuntimeException("Phone number is already registered");
        }
        
        User user = User.builder()
                .email(registerRequest.getEmail())
                .password(registerRequest.getPassword() != null ? 
                         passwordEncoder.encode(registerRequest.getPassword()) : null)
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .phoneNumber(registerRequest.getPhoneNumber())
                .isActive(true)
                .walletBalance(0.0)
                .build();
        
        user = userRepository.save(user);
        
        String token = jwtProvider.generateTokenFromUsername(user.getEmail());
        
        return JwtResponse.builder()
                .token(token)
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .role("USER")
                .message("User registration successful")
                .build();
    }
}
