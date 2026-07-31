# Frontend Integration Guide - Phone Number Support

## New Components Created

### 1. PhoneNumberManager Component
**Location:** `src/components/PhoneNumberManager.jsx`

A reusable component for managing phone numbers in user/admin profiles.

**Props:**
- `userId` (Number) - ID of the user/admin
- `userRole` (String) - 'ADMIN' or 'USER'
- `currentPhoneNumber` (String) - Current phone number (optional)
- `onPhoneNumberUpdate` (Function) - Callback when phone number is updated

**Usage:**
```jsx
import PhoneNumberManager from './PhoneNumberManager';

<PhoneNumberManager
  userId={user.id}
  userRole={user.role}
  currentPhoneNumber={user.phoneNumber}
  onPhoneNumberUpdate={(newPhone) => {
    // Update user state with new phone number
    setUser({...user, phoneNumber: newPhone});
  }}
/>
```

### 2. ForgotPasswordOTP Component
**Location:** `src/components/Auth/ForgotPasswordOTP.jsx`

A new component for password reset with OTP verification.

**Features:**
- Request OTP via phone number
- Verify OTP and reset password in one flow
- Supports both USER and ADMIN roles
- Clear error handling and validation

**Usage:**
```jsx
import ForgotPasswordOTP from './Auth/ForgotPasswordOTP';

<ForgotPasswordOTP
  onBackClick={() => {
    // Handle back button
    setShowForgotPassword(false);
  }}
/>
```

## Updated Components

### AuthContext
**Location:** `src/context/AuthContext.jsx`

**New Properties:**
- `phoneNumberWarning` - Warning message if phone number is missing
- `clearPhoneNumberWarning()` - Method to clear the warning

**Updated Methods:**
- `login()` - Now stores phone number warning if present
- `logout()` - Now clears phone number warning

## Integration Steps

### 1. Update Login Components

**File:** `src/components/Auth/UserLogin.jsx` and `src/components/Auth/AdminLogin.jsx`

Add forgot password with OTP option:

```jsx
import { useState } from 'react';
import ForgotPasswordOTP from './ForgotPasswordOTP';

export const UserLogin = () => {
  const [showForgotPassword, setShowForgotPassword] = useState(false);

  if (showForgotPassword) {
    return (
      <ForgotPasswordOTP 
        onBackClick={() => setShowForgotPassword(false)}
      />
    );
  }

  return (
    <div>
      {/* Existing login form */}
      <button
        type="button"
        onClick={() => setShowForgotPassword(true)}
        className="text-blue-600 hover:text-blue-700 text-sm"
      >
        Forgot Password?
      </button>
    </div>
  );
};
```

### 2. Add Phone Number Warning to Dashboards

**User Dashboard:** `src/components/User/UserDashboard.jsx`

```jsx
import { useAuth } from '../../context/AuthContext';
import toast from 'react-hot-toast';

const UserDashboard = () => {
  const { phoneNumberWarning, clearPhoneNumberWarning } = useAuth();

  useEffect(() => {
    if (phoneNumberWarning) {
      toast.custom((t) => (
        <div className="bg-yellow-50 border-l-4 border-yellow-400 p-4 rounded">
          <p className="text-yellow-700 mb-2">{phoneNumberWarning}</p>
          <button
            onClick={() => {
              clearPhoneNumberWarning();
              // Navigate to profile/phone settings
              navigate('/profile');
            }}
            className="bg-yellow-600 hover:bg-yellow-700 text-white px-3 py-1 rounded text-sm"
          >
            Add Phone Number
          </button>
        </div>
      ), {
        position: 'top-right',
        duration: 0 // Don't auto-dismiss
      });
    }
  }, [phoneNumberWarning]);

  return (
    // ... existing dashboard code
  );
};
```

### 3. Update Profile Pages

**User Profile:** `src/components/User/UserDashboard.jsx` or create new Profile component

```jsx
import PhoneNumberManager from '../PhoneNumberManager';

const UserProfile = () => {
  const [user, setUser] = useState(null);

  const handlePhoneUpdate = (newPhone) => {
    setUser({...user, phoneNumber: newPhone});
  };

  return (
    <div className="space-y-6">
      {/* Existing profile info */}
      
      <PhoneNumberManager
        userId={user.id}
        userRole="USER"
        currentPhoneNumber={user.phoneNumber}
        onPhoneNumberUpdate={handlePhoneUpdate}
      />
      
      {/* Other profile sections */}
    </div>
  );
};
```

### 4. Update Registration Components

**File:** `src/components/Auth/UserRegister.jsx` and `src/components/Auth/AdminRegister.jsx`

Add phone number field to registration form:

```jsx
import { useState } from 'react';
import api from '../../api/axiosConfig';
import { useAuth } from '../../context/AuthContext';
import toast from 'react-hot-toast';

export const UserRegister = () => {
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    phoneNumber: ''
  });
  const [error, setError] = useState(null);
  const { login } = useAuth();

  const handleRegister = async (e) => {
    e.preventDefault();
    setError(null);

    try {
      const response = await api.post('/auth/user/register', formData);
      
      // Login the user automatically
      login(response.data);
      toast.success('Registration successful!');
      
      // Redirect to dashboard
      window.location.href = '/user-dashboard';
    } catch (err) {
      const errorMessage = err.response?.data?.message || 'Registration failed';
      setError(errorMessage);
      toast.error(errorMessage);
    }
  };

  return (
    <form onSubmit={handleRegister} className="space-y-4">
      {/* Email */}
      <input
        type="email"
        placeholder="Email"
        value={formData.email}
        onChange={(e) => setFormData({...formData, email: e.target.value})}
        required
      />

      {/* Password */}
      <input
        type="password"
        placeholder="Password"
        value={formData.password}
        onChange={(e) => setFormData({...formData, password: e.target.value})}
        required
      />

      {/* First Name */}
      <input
        type="text"
        placeholder="First Name"
        value={formData.firstName}
        onChange={(e) => setFormData({...formData, firstName: e.target.value})}
        required
      />

      {/* Last Name */}
      <input
        type="text"
        placeholder="Last Name"
        value={formData.lastName}
        onChange={(e) => setFormData({...formData, lastName: e.target.value})}
        required
      />

      {/* Phone Number - NEW REQUIRED FIELD */}
      <input
        type="tel"
        placeholder="+919876543210"
        value={formData.phoneNumber}
        onChange={(e) => setFormData({...formData, phoneNumber: e.target.value})}
        required
      />
      <p className="text-sm text-gray-500">
        Format: +919876543210 or +1-234-567-8900
      </p>

      {error && (
        <div className="bg-red-50 border border-red-200 rounded p-3">
          <p className="text-red-700 text-sm">{error}</p>
        </div>
      )}

      <button
        type="submit"
        className="w-full bg-blue-600 hover:bg-blue-700 text-white py-2 rounded-lg"
      >
        Register
      </button>
    </form>
  );
};
```

## API Integration

### Phone Number Update
```javascript
// Update phone number
const updatePhoneNumber = async (userId, userRole, phoneNumber) => {
  const endpoint = userRole === 'ADMIN'
    ? `/admin/phone-number/${userId}`
    : `/user/phone-number/${userId}`;

  const response = await api.put(endpoint, { phoneNumber });
  return response.data;
};
```

### Password Reset with OTP
```javascript
// Request OTP
const requestPasswordResetOtp = async (phoneNumber, role) => {
  const response = await api.post('/password-reset/request-otp', {
    phoneNumber,
    role
  });
  return response.data;
};

// Reset password with OTP
const resetPassword = async (phoneNumber, otp, newPassword, role) => {
  const response = await api.post('/password-reset/reset-password', {
    phoneNumber,
    otp,
    newPassword,
    role
  });
  return response.data;
};
```

## Error Handling

### Common Error Scenarios

**Invalid Phone Format:**
```javascript
catch (err) {
  if (err.response?.data?.message.includes('Invalid phone number')) {
    setError('Please use format like +919876543210');
  }
}
```

**Phone Already Registered:**
```javascript
catch (err) {
  if (err.response?.data?.message.includes('already registered')) {
    setError('This phone number is already associated with another account');
  }
}
```

**OTP Expired:**
```javascript
catch (err) {
  if (err.response?.data?.message.includes('expired')) {
    setError('OTP has expired. Please request a new one.');
  }
}
```

## State Management

### Updated AuthContext Usage

```jsx
import { useAuth } from './context/AuthContext';

const MyComponent = () => {
  const { 
    user,
    phoneNumberWarning,
    clearPhoneNumberWarning
  } = useAuth();

  // Check if user has phone number
  if (!user?.phoneNumber) {
    return <div>Add phone number to your profile</div>;
  }

  // Handle warning
  useEffect(() => {
    if (phoneNumberWarning) {
      // Show warning and clear after action
      // ...
      clearPhoneNumberWarning();
    }
  }, [phoneNumberWarning]);
};
```

## Styling Considerations

### Responsive Design
- Phone number input should be full-width on mobile
- Use consistent color scheme (green for success, red for errors, yellow for warnings)
- Keep forms compact but readable

### Accessibility
- Use proper labels for all inputs
- Implement proper error messages
- Support keyboard navigation
- Use proper ARIA attributes

## Testing Checklist - Frontend

- [ ] Registration requires phone number
- [ ] Phone number validation works on form submission
- [ ] Login shows warning if phone number is missing
- [ ] Warning disappears after dismissal
- [ ] Existing users without phone can still login
- [ ] Profile page shows phone number status
- [ ] Can add/update phone number from profile
- [ ] Forgot password flow works with OTP
- [ ] OTP expires correctly
- [ ] Password reset successful with OTP
- [ ] Proper error messages shown for all scenarios
- [ ] Mobile responsive design works
- [ ] Toast notifications appear correctly

## File Structure

```
frontend/src/
├── components/
│   ├── PhoneNumberManager.jsx (NEW)
│   ├── Auth/
│   │   ├── UserLogin.jsx (UPDATED)
│   │   ├── AdminLogin.jsx (UPDATED)
│   │   ├── UserRegister.jsx (UPDATED)
│   │   ├── AdminRegister.jsx (UPDATED)
│   │   └── ForgotPasswordOTP.jsx (NEW)
│   ├── User/
│   │   └── UserDashboard.jsx (UPDATED)
│   └── Admin/
│       └── AdminDashboard.jsx (UPDATED)
├── context/
│   └── AuthContext.jsx (UPDATED)
└── api/
    └── axiosConfig.js
```

## Deployment Notes

1. Update all login pages to show "Forgot Password with OTP" option
2. Update all registration forms to include phone number field
3. Update user dashboards to show phone number warning
4. Add phone number manager to profile pages
5. Test password reset flow thoroughly
6. Verify backward compatibility with existing users
7. Update user documentation about phone number requirement
