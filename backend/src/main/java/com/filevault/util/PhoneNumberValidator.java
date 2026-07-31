package com.filevault.util;

import java.util.regex.Pattern;

public class PhoneNumberValidator {
    
    // Regex pattern for international phone numbers
    // Supports: +1-234-567-8900, +91 9876543210, 9876543210, +1234567890, etc.
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[0-9]{1,3}[-\\s]?[0-9]{1,4}[-\\s]?[0-9]{1,4}[-\\s]?[0-9]{1,9}$"
    );
    
    // Minimum and maximum length for phone numbers (including country code)
    private static final int MIN_LENGTH = 10;
    private static final int MAX_LENGTH = 20;
    
    /**
     * Validate phone number format
     * @param phoneNumber the phone number to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        String cleaned = phoneNumber.trim();
        
        // Check length
        if (cleaned.length() < MIN_LENGTH || cleaned.length() > MAX_LENGTH) {
            return false;
        }
        
        // Check pattern
        return PHONE_PATTERN.matcher(cleaned).matches();
    }
    
    /**
     * Validate phone number and throw exception if invalid
     * @param phoneNumber the phone number to validate
     * @throws IllegalArgumentException if phone number is invalid
     */
    public static void validateOrThrow(String phoneNumber) {
        if (!isValid(phoneNumber)) {
            throw new IllegalArgumentException(
                "Invalid phone number format. Please use format like +919876543210 or +1-234-567-8900"
            );
        }
    }
    
    /**
     * Get validation error message
     * @param phoneNumber the phone number to validate
     * @return error message if invalid, null if valid
     */
    public static String getValidationError(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return "Phone number cannot be empty";
        }
        
        String cleaned = phoneNumber.trim();
        
        if (cleaned.length() < MIN_LENGTH) {
            return "Phone number is too short (minimum " + MIN_LENGTH + " digits required)";
        }
        
        if (cleaned.length() > MAX_LENGTH) {
            return "Phone number is too long (maximum " + MAX_LENGTH + " characters)";
        }
        
        if (!PHONE_PATTERN.matcher(cleaned).matches()) {
            return "Invalid phone number format. Use formats like +919876543210 or +1-234-567-8900";
        }
        
        return null;
    }
}
