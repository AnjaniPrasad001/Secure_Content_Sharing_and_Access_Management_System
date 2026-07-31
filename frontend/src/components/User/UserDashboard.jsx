import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import PhoneNumberManager from '../PhoneNumberManager';
import api from '../../api/axiosConfig';
import AccessRequest from '../AccessRequest';
import FileViewer from '../FileViewer/FileViewer';
import toast from 'react-hot-toast';

const UserDashboard = () => {
  const [user, setUser] = useState(null);
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedFileId, setSelectedFileId] = useState(null);
  const [selectedFileDetails, setSelectedFileDetails] = useState(null);
  const [selectedFileForReading, setSelectedFileForReading] = useState(null);
  const [showPhoneWarning, setShowPhoneWarning] = useState(false);
  const navigate = useNavigate();
  const { logout, phoneNumberWarning, clearPhoneNumberWarning } = useAuth();

  useEffect(() => {
    fetchDashboardData();
    
    // Check for phone number warning
    if (phoneNumberWarning) {
      setShowPhoneWarning(true);
    }
  }, [phoneNumberWarning]);

  const fetchDashboardData = async () => {
    try {
      const userData = JSON.parse(localStorage.getItem('user'));
      const userId = userData.id;

      const [userRes, filesRes] = await Promise.all([
        api.get(`/user/profile/${userId}`),
        api.get(`/user/${userId}/files`),
      ]);

      setUser(userRes.data);
      console.log('Fetched files:', filesRes.data);
      console.log('Number of files:', filesRes.data?.length || 0);
      setFiles(filesRes.data || []);
      setLoading(false);
    } catch (error) {
      console.error('Failed to fetch dashboard data:', error);
      console.error('Error response:', error.response?.data);
      setLoading(false);
    }
  };

  const handlePhoneNumberUpdate = (newPhone) => {
    setUser({ ...user, phoneNumber: newPhone });
    setShowPhoneWarning(false);
    clearPhoneNumberWarning();
    toast.success('Phone number updated successfully!');
  };

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const handleDownload = async (fileId) => {
    try {
      const userData = JSON.parse(localStorage.getItem('user'));
      const response = await api.get(`/files/download/${fileId}?userId=${userData.id}`, {
        responseType: 'blob',
      });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'file');
      document.body.appendChild(link);
      link.click();
      link.parentElement.removeChild(link);
    } catch (error) {
      console.error('Failed to download file:', error);
    }
  };

  const handleViewDetails = (file) => {
    setSelectedFileDetails(file);
  };

  if (loading) {
    return <div className="flex items-center justify-center min-h-screen">Loading...</div>;
  }

  return (
    <div className="min-h-screen bg-gray-100">
      {/* Navbar */}
      <nav className="bg-green-600 text-white shadow-lg">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex justify-between items-center">
          <h1 className="text-2xl font-bold">FileVault - User Dashboard</h1>
          <button
            onClick={handleLogout}
            className="bg-red-500 hover:bg-red-700 px-4 py-2 rounded"
          >
            Logout
          </button>
        </div>
      </nav>

      <div className="max-w-7xl mx-auto px-4 py-8">
        {/* Phone Number Warning */}
        {showPhoneWarning && !user?.phoneNumber && (
          <div className="bg-yellow-50 border-l-4 border-yellow-400 p-4 mb-6 rounded">
            <div className="flex justify-between items-start">
              <div className="flex-1">
                <p className="text-yellow-800 font-semibold">⚠️ No Phone Number on File</p>
                <p className="text-yellow-700 text-sm mt-1">
                  Please add your phone number to enable OTP-based password recovery and account security features.
                </p>
              </div>
              <button
                onClick={() => setShowPhoneWarning(false)}
                className="text-yellow-400 hover:text-yellow-600 text-xl"
              >
                ✕
              </button>
            </div>
          </div>
        )}

        {/* Phone Number Manager */}
        {user && (
          <div className="bg-white rounded-lg shadow p-6 mb-8">
            <PhoneNumberManager
              userId={user.id}
              userRole="USER"
              currentPhoneNumber={user.phoneNumber}
              onPhoneNumberUpdate={handlePhoneNumberUpdate}
            />
          </div>
        )}

        {/* User Info */}
        <div className="bg-white rounded-lg shadow p-6 mb-8">
          <div className="flex justify-between items-center">
            <div>
              <h2 className="text-xl font-bold text-gray-800">
                Welcome, {user?.firstName} {user?.lastName}
              </h2>
              <p className="text-gray-600">{user?.email}</p>
            </div>
            <div className="text-right">
              <p className="text-sm text-gray-600">Wallet Balance</p>
              <p className="text-2xl font-bold text-green-600">${user?.walletBalance?.toFixed(2) || 0}</p>
            </div>
          </div>
        </div>

        {/* Available Files */}
        <div className="bg-white rounded-lg shadow">
          <div className="px-6 py-4 border-b">
            <h2 className="text-xl font-bold text-gray-800">Available Files</h2>
          </div>
          <div className="p-6">
            {files.length === 0 ? (
              <p className="text-gray-500">No files available</p>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {files.map((file) => (
                  <div key={file.id} className="border rounded-lg p-4 hover:shadow-lg transition bg-white">
                    <h3 className="font-semibold text-gray-800 truncate mb-2">{file.originalFileName}</h3>
                    <p className="text-sm text-gray-600 mb-1">By: {file.adminName}</p>
                    <p className="text-sm text-gray-600 mb-1">Category: {file.categoryName}</p>
                    <p className="text-sm text-gray-600 mb-1">Type: {file.accessType}</p>
                    {file.price && <p className="text-sm font-semibold text-green-600 mb-1">Price: ${file.price}</p>}
                    <p className="text-xs text-gray-500 mb-2">
                      Size: {(file.fileSize / 1024).toFixed(2)} KB | Views: {file.viewCount || 0}
                    </p>
                    <p className="text-xs text-gray-500 mb-3">
                      Uploaded: {new Date(file.uploadedAt).toLocaleDateString()}
                    </p>
                    
                    {/* Description Preview */}
                    {file.description && (
                      <p className="text-sm text-gray-700 mb-3 line-clamp-2 bg-gray-50 p-2 rounded">
                        {file.description}
                      </p>
                    )}

                    {/* Action Buttons */}
                    <div className="space-y-2">
                      <button
                        onClick={() => handleViewDetails(file)}
                        className="w-full bg-indigo-500 text-white py-2 rounded hover:bg-indigo-600 text-sm font-medium"
                      >
                        View Details
                      </button>
                      {file.hasAccess && (
                        <>
                          <button
                            onClick={() => setSelectedFileForReading(file)}
                            className="w-full bg-purple-500 text-white py-2 rounded hover:bg-purple-600 text-sm font-medium"
                          >
                            Read File
                          </button>
                          <button
                            onClick={() => handleDownload(file.id)}
                            className="w-full bg-blue-500 text-white py-2 rounded hover:bg-blue-600 text-sm font-medium"
                          >
                            Download
                          </button>
                        </>
                      )}
                      {!file.hasAccess && file.accessType === 'RESTRICTED' && (
                        <button
                          onClick={() => setSelectedFileId(file.id)}
                          className="w-full bg-green-500 text-white py-2 rounded hover:bg-green-600 text-sm font-medium"
                        >
                          Request Access
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Access Request Modal */}
        {selectedFileId && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg shadow-lg p-6 max-w-md w-full mx-4">
              <div className="flex justify-between items-center mb-4">
                <h3 className="text-xl font-bold">Request File Access</h3>
                <button
                  onClick={() => setSelectedFileId(null)}
                  className="text-gray-500 hover:text-gray-700 text-2xl"
                >
                  ×
                </button>
              </div>
              <AccessRequest
                fileId={selectedFileId}
                fileName={files.find(f => f.id === selectedFileId)?.originalFileName}
                userId={user?.id}
                onRequestSuccess={() => {
                  setSelectedFileId(null);
                  fetchDashboardData();
                }}
              />
            </div>
          </div>
        )}

        {/* File Details Modal */}
        {selectedFileDetails && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg shadow-lg p-6 max-w-2xl w-full mx-4 max-h-96 overflow-y-auto">
              <div className="flex justify-between items-center mb-4">
                <h3 className="text-2xl font-bold">File Details</h3>
                <button
                  onClick={() => setSelectedFileDetails(null)}
                  className="text-gray-500 hover:text-gray-700 text-2xl"
                >
                  ×
                </button>
              </div>

              <div className="space-y-3">
                <div className="border-b pb-3">
                  <p className="text-sm text-gray-600">File Name</p>
                  <p className="font-semibold text-gray-800 break-all">{selectedFileDetails.originalFileName}</p>
                </div>

                <div className="border-b pb-3">
                  <p className="text-sm text-gray-600">Admin</p>
                  <p className="font-semibold text-gray-800">{selectedFileDetails.adminName}</p>
                </div>

                <div className="grid grid-cols-2 gap-4 border-b pb-3">
                  <div>
                    <p className="text-sm text-gray-600">File Size</p>
                    <p className="font-semibold text-gray-800">{(selectedFileDetails.fileSize / 1024).toFixed(2)} KB</p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-600">File Type</p>
                    <p className="font-semibold text-gray-800">{selectedFileDetails.fileType || 'Unknown'}</p>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4 border-b pb-3">
                  <div>
                    <p className="text-sm text-gray-600">Category</p>
                    <p className="font-semibold text-gray-800">{selectedFileDetails.categoryName}</p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-600">Access Type</p>
                    <p className={`font-semibold ${selectedFileDetails.accessType === 'PUBLIC' ? 'text-green-600' : 'text-orange-600'}`}>
                      {selectedFileDetails.accessType}
                    </p>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4 border-b pb-3">
                  <div>
                    <p className="text-sm text-gray-600">Views</p>
                    <p className="font-semibold text-gray-800">👁️ {selectedFileDetails.viewCount || 0}</p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-600">Uploaded On</p>
                    <p className="font-semibold text-gray-800">{new Date(selectedFileDetails.uploadedAt).toLocaleDateString()}</p>
                  </div>
                </div>

                {selectedFileDetails.price && (
                  <div className="border-b pb-3">
                    <p className="text-sm text-gray-600">Price</p>
                    <p className="font-semibold text-green-600 text-lg">${selectedFileDetails.price}</p>
                  </div>
                )}

                {selectedFileDetails.description && (
                  <div>
                    <p className="text-sm text-gray-600 mb-2">Description</p>
                    <p className="text-gray-800 bg-gray-50 p-3 rounded">{selectedFileDetails.description}</p>
                  </div>
                )}

                <div className="mt-6 space-y-2">
                  {selectedFileDetails.hasAccess && (
                    <>
                      <button
                        onClick={() => {
                          setSelectedFileForReading(selectedFileDetails);
                          setSelectedFileDetails(null);
                        }}
                        className="w-full bg-purple-600 text-white py-3 rounded hover:bg-purple-700 font-medium"
                      >
                        Read File
                      </button>
                      <button
                        onClick={() => {
                          handleDownload(selectedFileDetails.id);
                          setSelectedFileDetails(null);
                        }}
                        className="w-full bg-blue-600 text-white py-3 rounded hover:bg-blue-700 font-medium"
                      >
                        Download File
                      </button>
                    </>
                  )}
                  {!selectedFileDetails.hasAccess && selectedFileDetails.accessType === 'RESTRICTED' && (
                    <button
                      onClick={() => {
                        setSelectedFileDetails(null);
                        setSelectedFileId(selectedFileDetails.id);
                      }}
                      className="w-full bg-green-600 text-white py-3 rounded hover:bg-green-700 font-medium"
                    >
                      Request Access
                    </button>
                  )}
                  <button
                    onClick={() => setSelectedFileDetails(null)}
                    className="w-full bg-gray-300 text-gray-800 py-2 rounded hover:bg-gray-400"
                  >
                    Close
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* File Viewer Modal */}
        {selectedFileForReading && (
          <FileViewer
            file={selectedFileForReading}
            userId={user?.id}
            onClose={() => setSelectedFileForReading(null)}
          />
        )}
      </div>
    </div>
  );
};

export default UserDashboard;
