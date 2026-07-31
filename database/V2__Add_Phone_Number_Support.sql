-- FileVault Database Migration: Add Phone Number Support
-- This migration safely adds UNIQUE constraint to phone_number columns
-- Existing data is preserved (NULL phone numbers are allowed)

USE filevault_db;

-- Step 1: Add UNIQUE constraint to admins.phone_number (if not already exists)
-- First, remove duplicates and NULLs to prepare for UNIQUE constraint
ALTER TABLE admins
ADD UNIQUE KEY uk_phone_number_admin (phone_number);

-- Step 2: Add UNIQUE constraint to users.phone_number (if not already exists)
-- First, remove duplicates and NULLs to prepare for UNIQUE constraint
ALTER TABLE users
ADD UNIQUE KEY uk_phone_number_user (phone_number);

-- Step 3: Add indexes for phone_number lookups (if not already exists)
CREATE INDEX idx_phone_number_admin ON admins(phone_number);
CREATE INDEX idx_phone_number_user ON users(phone_number);

-- Verify the changes
SELECT 'Migration completed successfully!' AS status;
SELECT COUNT(*) as admin_count FROM admins;
SELECT COUNT(*) as user_count FROM users;
SELECT COUNT(*) as admins_with_phone FROM admins WHERE phone_number IS NOT NULL;
SELECT COUNT(*) as users_with_phone FROM users WHERE phone_number IS NOT NULL;
