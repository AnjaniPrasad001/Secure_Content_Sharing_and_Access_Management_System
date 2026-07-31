import React, { useState } from 'react';
import api from '../api/axiosConfig';
import toast from 'react-hot-toast';

const PhoneNumberManager = ({ userId, userRole, currentPhoneNumber, onPhoneNumberUpdate }) => {
  const [phoneNumber, setPhoneNumber] = useState(currentPhoneNumber || '');
  const [isEditing, setIsEditing] = useState(!currentPhoneNumber);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSavePhoneNumber = async () => {
    if (!phoneNumber.trim()) {
      setError('Phone number cannot be empty');
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const endpoint = userRole === 'ADMIN' 
        ? `/admin/phone-number/${userId}`
        : `/user/phone-number/${userId}`;

      await api.put(endpoint, {
        phoneNumber: phoneNumber.trim()
      });

      toast.success('Phone number updated successfully');
      setIsEditing(false);
      
      // Callback to parent component
      if (onPhoneNumberUpdate) {
        onPhoneNumberUpdate(phoneNumber);
      }
    } catch (err) {
      const errorMessage = err.response?.data?.message || 'Failed to update phone number';
      setError(errorMessage);
      toast.error(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCancel = () => {
    setPhoneNumber(currentPhoneNumber || '');
    setIsEditing(false);
    setError(null);
  };

  return (
    <div className="bg-white rounded-lg shadow p-6 mb-6">
      <h3 className="text-lg font-semibold text-gray-800 mb-4">Phone Number</h3>
      
      {!currentPhoneNumber && (
        <div className="bg-yellow-50 border-l-4 border-yellow-400 p-4 mb-4">
          <p className="text-yellow-700">
            <strong>⚠️ No phone number on file.</strong> Add one to enable password recovery via OTP.
          </p>
        </div>
      )}

      {isEditing ? (
        <div className="space-y-4">
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
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-green-500"
            />
            <p className="text-sm text-gray-500 mt-1">
              Format: +919876543210 or +1-234-567-8900
            </p>
          </div>

          {error && (
            <div className="bg-red-50 border border-red-200 rounded p-3">
              <p className="text-red-700 text-sm">{error}</p>
            </div>
          )}

          <div className="flex gap-3">
            <button
              onClick={handleSavePhoneNumber}
              disabled={isLoading}
              className="bg-green-600 hover:bg-green-700 disabled:bg-gray-400 text-white px-4 py-2 rounded-lg transition"
            >
              {isLoading ? 'Saving...' : 'Save Phone Number'}
            </button>
            <button
              onClick={handleCancel}
              disabled={isLoading}
              className="bg-gray-300 hover:bg-gray-400 disabled:bg-gray-400 text-gray-800 px-4 py-2 rounded-lg transition"
            >
              Cancel
            </button>
          </div>
        </div>
      ) : (
        <div className="flex justify-between items-center">
          <div>
            {currentPhoneNumber ? (
              <p className="text-gray-700 font-mono">{currentPhoneNumber}</p>
            ) : (
              <p className="text-gray-500 italic">No phone number added</p>
            )}
          </div>
          <button
            onClick={() => setIsEditing(true)}
            className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg transition"
          >
            {currentPhoneNumber ? 'Update' : 'Add'} Phone Number
          </button>
        </div>
      )}
    </div>
  );
};

export default PhoneNumberManager;
