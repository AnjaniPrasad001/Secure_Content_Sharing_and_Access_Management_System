package com.filevault.service;

import com.filevault.entity.User;
import com.filevault.exception.ResourceNotFoundException;
import com.filevault.repository.UserRepository;
import com.filevault.util.PhoneNumberValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@Transactional
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
    
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public List<User> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return userRepository.searchUsers(query.trim());
    }
    
    public User updateUserProfile(Long userId, String firstName, String lastName, String phoneNumber) {
        User user = getUserById(userId);
        
        // Update name fields
        if (firstName != null && !firstName.isEmpty()) {
            user.setFirstName(firstName);
        }
        if (lastName != null && !lastName.isEmpty()) {
            user.setLastName(lastName);
        }
        
        // Validate and update phone number if provided
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            PhoneNumberValidator.validateOrThrow(phoneNumber);
            
            // Check if new phone number is already in use by another user
            if (userRepository.existsByPhoneNumber(phoneNumber)) {
                User existingUser = userRepository.findByPhoneNumber(phoneNumber).get();
                if (!existingUser.getId().equals(userId)) {
                    throw new RuntimeException("Phone number is already registered by another user");
                }
            }
            
            user.setPhoneNumber(phoneNumber);
        }
        
        return userRepository.save(user);
    }

    public User updateUserPhoneNumber(Long userId, String phoneNumber) {
        User user = getUserById(userId);
        
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            // Validate phone number
            PhoneNumberValidator.validateOrThrow(phoneNumber);
            
            // Check if phone number is already in use by another user
            if (userRepository.existsByPhoneNumber(phoneNumber)) {
                User existingUser = userRepository.findByPhoneNumber(phoneNumber).get();
                if (!existingUser.getId().equals(userId)) {
                    throw new RuntimeException("Phone number is already registered");
                }
            }
            
            user.setPhoneNumber(phoneNumber);
        } else {
            throw new RuntimeException("Phone number cannot be empty");
        }
        return userRepository.save(user);
    }
    
    public User addWalletBalance(Long userId, Double amount) {
        User user = getUserById(userId);
        user.setWalletBalance(user.getWalletBalance() + amount);
        return userRepository.save(user);
    }
    
    public User deductWalletBalance(Long userId, Double amount) {
        User user = getUserById(userId);
        if (user.getWalletBalance() < amount) {
            throw new RuntimeException("Insufficient wallet balance");
        }
        user.setWalletBalance(user.getWalletBalance() - amount);
        return userRepository.save(user);
    }
    
    public Double getWalletBalance(Long userId) {
        User user = getUserById(userId);
        return user.getWalletBalance();
    }
    
    public void deactivateUser(Long userId) {
        User user = getUserById(userId);
        user.setIsActive(false);
        userRepository.save(user);
    }
    
    public void activateUser(Long userId) {
        User user = getUserById(userId);
        user.setIsActive(true);
        userRepository.save(user);
    }
}
