import React, { useState } from 'react';
import api from '../../api/axiosConfig';
import toast from 'react-hot-toast';

const ForgotPasswordOTP = ({ onBackClick }) => {
  const [step, setStep] = useState(1); // 1: Request OTP, 2: Verify OTP, 3: Reset Password
  const [phoneNumber, setPhoneNumber] = useState('');
  const [role, setRole] = useState('USER'); // USER or ADMIN
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [maskedPhone, setMaskedPhone] = useState(null);

  const handleRequestOtp = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setError(null);

    try {
      const response = await api.post('/password-reset/request-otp', {
        phoneNumber: phoneNumber.trim(),
        role: role.toUpperCase()
      });

      setMaskedPhone(response.data.phoneNumber);
      setStep(2);
      toast.success('OTP sent to your phone number');
    } catch (err) {
      const errorMessage = err.response?.data?.message || 'Failed to request OTP';
      setError(errorMessage);
      toast.error(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  const handleVerifyAndReset = async (e) => {
    e.preventDefault();
    
    if (newPassword !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    if (newPassword.length < 6) {
      setError('Password must be at least 6 characters');
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      await api.post('/password-reset/reset-password', {
        phoneNumber: phoneNumber.trim(),
        otp: otp.trim(),
        newPassword: newPassword,
        role: role.toUpperCase()
      });

      toast.success('Password reset successfully! You can now login.');
      setStep(1);
      setPhoneNumber('');
      setOtp('');
      setNewPassword('');
      setConfirmPassword('');
      setRole('USER');
      
      // Redirect to login after 2 seconds
      setTimeout(() => {
        window.location.href = '/';
      }, 2000);
    } catch (err) {
      const errorMessage = err.response?.data?.message || 'Failed to reset password';
      setError(errorMessage);
      toast.error(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  const handleBack = () => {
    if (step === 2) {
      setStep(1);
      setError(null);
    } else {
      onBackClick();
    }
  };

  return (
    <div className="bg-white rounded-lg shadow p-8 w-full max-w-md mx-auto">
      <h2 className="text-2xl font-bold text-gray-800 mb-6">Reset Password with OTP</h2>

      {step === 1 && (
        <form onSubmit={handleRequestOtp}>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Account Type
              </label>
              <select
                value={role}
                onChange={(e) => setRole(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              >
                <option value="USER">User</option>
                <option value="ADMIN">Admin</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Phone Number
              </label>
              <input
                type="tel"
                placeholder="+919876543210"
                value={phoneNumber}
                onChange={(e) => {
                  setPhoneNumber(e.target.value);
                  setError(null);
                }}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                required
              />
              <p className="text-sm text-gray-500 mt-1">
                The phone number registered with your account
              </p>
            </div>

            {error && (
              <div className="bg-red-50 border border-red-200 rounded p-3">
                <p className="text-red-700 text-sm">{error}</p>
              </div>
            )}

            <button
              type="submit"
              disabled={isLoading}
              className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-gray-400 text-white py-2 rounded-lg font-medium transition"
            >
              {isLoading ? 'Sending OTP...' : 'Request OTP'}
            </button>

            <button
              type="button"
              onClick={handleBack}
              className="w-full bg-gray-300 hover:bg-gray-400 text-gray-800 py-2 rounded-lg font-medium transition"
            >
              Back to Login
            </button>
          </div>
        </form>
      )}

      {step === 2 && (
        <form onSubmit={handleVerifyAndReset}>
          <div className="space-y-4">
            <div className="bg-blue-50 border border-blue-200 rounded p-3">
              <p className="text-blue-700 text-sm">
                OTP sent to {maskedPhone}. Valid for 5 minutes.
              </p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Enter OTP
              </label>
              <input
                type="text"
                placeholder="123456"
                maxLength="6"
                value={otp}
                onChange={(e) => {
                  setOtp(e.target.value.replace(/[^\d]/g, ''));
                  setError(null);
                }}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-center text-2xl tracking-widest"
                required
              />
              <p className="text-sm text-gray-500 mt-1">6-digit code</p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                New Password
              </label>
              <input
                type="password"
                placeholder="Minimum 6 characters"
                value={newPassword}
                onChange={(e) => {
                  setNewPassword(e.target.value);
                  setError(null);
                }}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Confirm Password
              </label>
              <input
                type="password"
                placeholder="Re-enter your password"
                value={confirmPassword}
                onChange={(e) => {
                  setConfirmPassword(e.target.value);
                  setError(null);
                }}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                required
              />
            </div>

            {error && (
              <div className="bg-red-50 border border-red-200 rounded p-3">
                <p className="text-red-700 text-sm">{error}</p>
              </div>
            )}

            <button
              type="submit"
              disabled={isLoading}
              className="w-full bg-green-600 hover:bg-green-700 disabled:bg-gray-400 text-white py-2 rounded-lg font-medium transition"
            >
              {isLoading ? 'Resetting Password...' : 'Reset Password'}
            </button>

            <button
              type="button"
              onClick={handleBack}
              className="w-full bg-gray-300 hover:bg-gray-400 text-gray-800 py-2 rounded-lg font-medium transition"
            >
              Back
            </button>
          </div>
        </form>
      )}
    </div>
  );
};

export default ForgotPasswordOTP;
