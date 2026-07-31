# FileVault - Phone Number Support Implementation Guide

## Overview

This document describes the implementation of phone number support for existing and new Admin and User accounts in FileVault. The implementation preserves all existing data while safely adding phone number functionality.

## Architecture Changes

### 1. Database Schema Updates

**Files Modified:**
- `database/schema.sql` - Updated with UNIQUE constraints on phone_number
- `database/V2__Add_Phone_Number_Support.sql` - Migration script for existing databases

**Changes:**
- Added `UNIQUE` constraint to `phone_number` columns in both `admins` and `users` tables
- Phone numbers remain `NULL`-able to maintain backward compatibility with existing accounts
- Added indexes on `phone_number` for faster lookups

### 2. Entity Updates

**Files Modified:**
- `backend/src/main/java/com/filevault/entity/Admin.java`
- `backend/src/main/java/com/filevault/entity/User.java`

**Changes:**
- Updated `@Column` annotation for `phoneNumber` field to include `unique = true`
- Maintained `nullable = true` to preserve existing data

### 3. New Utility Classes

**Created:** `PhoneNumberValidator.java`
- Validates phone number format
- Supports international formats (e.g., +919876543210, +1-234-567-8900)
- Provides detailed error messages for validation failures
- Min length: 10 digits, Max length: 20 characters

### 4. Database Repository Updates

**Files Modified:**
- `AdminRepository.java` - Added `findByPhoneNumber()` and `existsByPhoneNumber()`
- `UserRepository.java` - Added `findByPhoneNumber()` and `existsByPhoneNumber()`

### 5. DTO Updates

**Modified:** `JwtResponse.java`
- Added `phoneNumber` field to return phone number after login
- Added `phoneNumberWarning` field to warn users about missing phone numbers

**Modified:** `RegisterRequest.java`
- Made `phoneNumber` mandatory (`@NotBlank`) for NEW registrations
- Updated phone number pattern validation
- Enforces phone number format for all new users/admins

**Created:** `OtpVerificationRequest.java`
- For OTP verification in password reset flow

**Created:** `PasswordResetRequest.java`
- For password reset with OTP

### 6. Service Layer Updates

**Modified:** `AuthService.java`
- Added phone number validation on registration
- Added phone number uniqueness check
- Added phone number warning in login response if missing
- Preserves existing accounts without phone numbers

**Modified:** `AdminService.java`
- Added `updateAdminPhoneNumber()` method
- Updated `updateAdminProfile()` with phone validation
- Prevents duplicate phone numbers

**Modified:** `UserService.java`
- Added phone number validation in `updateUserPhoneNumber()`
- Updated `updateUserProfile()` with phone validation
- Prevents duplicate phone numbers

**Created:** `PasswordResetService.java`
- Handles OTP generation and verification
- OTP validity: 5 minutes
- Validates phone number exists before sending OTP
- Supports both ADMIN and USER roles
- Uses in-memory OTP storage (can be upgraded to Redis/Database)

### 7. Controller Updates

**Modified:** `AdminController.java`
- Added `PUT /api/admin/phone-number/{adminId}` endpoint
- Dedicated endpoint for phone number updates
- Returns detailed error messages

**Modified:** `UserController.java`
- Added `PUT /api/user/phone-number/{userId}` endpoint
- Dedicated endpoint for phone number updates
- Returns detailed error messages

**Created:** `PasswordResetController.java`
- `POST /api/password-reset/request-otp` - Request OTP
- `POST /api/password-reset/verify-otp` - Verify OTP
- `POST /api/password-reset/reset-password` - Reset password with OTP

## API Endpoints

### Registration (NEW - Phone Required)

**Admin Registration:**
```
POST /api/auth/admin/register
Content-Type: application/json

{
  "email": "admin@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+919876543210"
}
```

**User Registration:**
```
POST /api/auth/user/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "firstName": "Jane",
  "lastName": "Smith",
  "phoneNumber": "+919876543210"
}
```

### Login (WITH Phone Number Warning for Existing Users)

**Admin/User Login:**
```
POST /api/auth/admin/login
or
POST /api/auth/user/login

Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}

Response:
{
  "token": "eyJhbGc...",
  "type": "Bearer",
  "id": 1,
  "email": "user@example.com",
  "firstName": "Jane",
  "lastName": "Smith",
  "phoneNumber": null,
  "role": "USER",
  "message": "User login successful",
  "phoneNumberWarning": "Phone number missing. Please update your profile to enable OTP password recovery."
}
```

### Update Profile with Phone Number

**Admin:**
```
PUT /api/admin/profile/{adminId}
Content-Type: application/json
Authorization: Bearer {token}

{
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+919876543210"
}
```

**User:**
```
PUT /api/user/profile/{userId}
Content-Type: application/json
Authorization: Bearer {token}

{
  "firstName": "Jane",
  "lastName": "Smith",
  "phoneNumber": "+919876543210"
}
```

### Update Phone Number (Dedicated Endpoint)

**Admin:**
```
PUT /api/admin/phone-number/{adminId}
Content-Type: application/json
Authorization: Bearer {token}

{
  "phoneNumber": "+919876543210"
}
```

**User:**
```
PUT /api/user/phone-number/{userId}
Content-Type: application/json
Authorization: Bearer {token}

{
  "phoneNumber": "+919876543210"
}
```

### Password Reset with OTP

**Step 1: Request OTP**
```
POST /api/password-reset/request-otp
Content-Type: application/json

{
  "phoneNumber": "+919876543210",
  "role": "ADMIN" or "USER"
}

Response:
{
  "message": "OTP sent to your phone number. Valid for 5 minutes.",
  "phoneNumber": "****3210"
}
```

**Step 2: Verify OTP (Optional)**
```
POST /api/password-reset/verify-otp
Content-Type: application/json

{
  "phoneNumber": "+919876543210",
  "otp": "123456",
  "role": "ADMIN" or "USER"
}

Response:
{
  "message": "OTP verified successfully",
  "verified": true
}
```

**Step 3: Reset Password**
```
POST /api/password-reset/reset-password
Content-Type: application/json

{
  "phoneNumber": "+919876543210",
  "otp": "123456",
  "newPassword": "newPassword123",
  "role": "ADMIN" or "USER"
}

Response:
{
  "message": "Password reset successfully. You can now login with your new password."
}
```

## Data Migration for Existing Users

### Important Notes

1. **Existing accounts without phone numbers:**
   - Will NOT be deleted
   - Will NOT be locked out
   - Can still login with email/password
   - Will see warning message to add phone number

2. **Phone number requirement:**
   - MANDATORY for NEW registrations
   - OPTIONAL for existing accounts (but required for password reset)
   - Can be added anytime via profile update

3. **Backward Compatibility:**
   - All existing features work unchanged
   - File uploads, downloads, access control - all preserved
   - Payment history - all preserved
   - JWT authentication - all preserved

### Migration Steps

1. **Apply Database Migration:**
   ```bash
   mysql -u root -p filevault_db < database/V2__Add_Phone_Number_Support.sql
   ```

2. **Rebuild and Restart Backend:**
   ```bash
   cd backend
   mvn clean install
   mvn spring-boot:run
   ```

3. **Frontend Update:**
   - Users see profile update prompt on next login
   - Can add phone number via Settings page
   - Password reset now available if phone number is added

## Security Considerations

1. **Phone Number Validation:**
   - Format validated before storage
   - Uniqueness enforced at database level
   - Prevents duplicate registrations

2. **OTP Security:**
   - 6-digit OTP with 5-minute expiration
   - OTP stored in memory (can be upgraded to secure Redis)
   - Single-use OTP (removed after verification)
   - Phone number masked in responses

3. **Password Reset:**
   - Only works with registered phone number
   - OTP verification required
   - Uses same password encoder as registration

## Frontend Integration

### Profile Page Updates

Add phone number section on profile:
```jsx
// Show if phone number is missing
<div className="warning">
  Phone number missing. Add it to enable password recovery.
</div>

// Phone input field
<input type="tel" placeholder="+919876543210" />

// Save button
<button onClick={updatePhoneNumber}>Save Phone Number</button>
```

### Login Response Handling

```javascript
// Check for phoneNumberWarning in login response
if (response.phoneNumberWarning) {
  showWarning(response.phoneNumberWarning);
  redirectToAddPhone();
}
```

### Forgot Password Flow

```jsx
// New forgot password with OTP
1. User clicks "Forgot Password"
2. Enters phone number
3. Receives OTP via SMS
4. Enters OTP and new password
5. Password reset successful
```

## Error Handling

### Common Error Responses

**Invalid Phone Number Format:**
```json
{
  "error": "Invalid phone number format",
  "message": "Phone number must be valid (e.g., +919876543210 or +1-234-567-8900)"
}
```

**Phone Number Already Registered:**
```json
{
  "error": "Failed to update phone number",
  "message": "Phone number is already registered"
}
```

**Phone Number Required for Password Reset:**
```json
{
  "error": "Password reset failed",
  "message": "No admin account found with this phone number. Please update your profile with a phone number first."
}
```

**OTP Expired:**
```json
{
  "error": "Password reset failed",
  "message": "OTP has expired. Please request a new OTP."
}
```

## Testing Checklist

- [ ] Existing admins can login without phone number
- [ ] Existing users can login without phone number
- [ ] New admin registration requires phone number
- [ ] New user registration requires phone number
- [ ] Phone number validation works correctly
- [ ] Duplicate phone numbers are rejected
- [ ] Existing admins can add phone number via profile update
- [ ] Existing users can add phone number via profile update
- [ ] Phone number warning appears in login response for accounts without phone
- [ ] OTP request works for registered phone numbers
- [ ] OTP verification works correctly
- [ ] Password reset with OTP works
- [ ] All existing files are preserved
- [ ] All existing purchases/payments are preserved
- [ ] All existing access controls are preserved

## Future Enhancements

1. **Redis Integration:** Move OTP storage to Redis for distributed systems
2. **Email OTP Option:** Send OTP via email as alternative
3. **Biometric Login:** Support phone-based biometric authentication
4. **2FA:** Two-factor authentication using phone number
5. **SMS Notifications:** Use phone number for important notifications
6. **Twilio Integration:** Production SMS sending

## Rollback Plan

If issues occur:

1. **Revert Database:**
   ```sql
   ALTER TABLE admins DROP INDEX uk_phone_number_admin;
   ALTER TABLE users DROP INDEX uk_phone_number_user;
   ```

2. **Revert Code:**
   - Git checkout to previous commit
   - No data is lost (phone_number column remains with existing data)

3. **Restart Services:**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

## Support

For issues or questions:
1. Check error messages returned by API
2. Review logs for detailed error information
3. Verify phone number format (international format recommended)
4. Ensure database migration was applied correctly
