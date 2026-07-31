import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import PhoneNumberManager from '../PhoneNumberManager';
import api from '../../api/axiosConfig';
import FileUpload from '../FileUpload';
import RequestManagement from '../RequestManagement';
import FileEditor from '../FileEditor';
import toast from 'react-hot-toast';

const AdminDashboard = () => {
  const [admin, setAdmin] = useState(null);
  const [adminData, setAdminData] = useState(null);
  const [files, setFiles] = useState([]);
  const [categories, setCategories] = useState([]);
  const [dashboard, setDashboard] = useState({});
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('dashboard');
  const [editingFile, setEditingFile] = useState(null);
  const [deletingFileId, setDeletingFileId] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [deleteError, setDeleteError] = useState('');
  const [showPhoneWarning, setShowPhoneWarning] = useState(false);
  const navigate = useNavigate();
  const { logout, phoneNumberWarning, clearPhoneNumberWarning } = useAuth();

  useEffect(() => {
    fetchDashboardData();
    fetchCategories();
    
    // Check for phone number warning
    if (phoneNumberWarning) {
      setShowPhoneWarning(true);
    }
  }, [phoneNumberWarning]);

  const fetchCategories = async () => {
    try {
      const res = await api.get('/categories');
      setCategories(res.data || []);
    } catch (error) {
      console.error('Failed to fetch categories:', error);
    }
  };

  const fetchDashboardData = async () => {
    try {
      const userData = JSON.parse(localStorage.getItem('user'));
      const adminId = userData?.id;
      
      console.log('Admin ID from localStorage:', adminId);
      console.log('Full user object:', userData);
      
      if (!adminId) {
        console.error('No admin ID found in localStorage');
        setLoading(false);
        return;
      }
      
      setAdmin(adminId);
      setAdminData(userData);

      console.log('Fetching dashboard for admin:', adminId);
      
      const [dashRes, filesRes, adminRes] = await Promise.all([
        api.get(`/admin/${adminId}/dashboard`),
        api.get(`/admin/${adminId}/files`),
        api.get(`/admin/profile/${adminId}`),
      ]);

      console.log('Dashboard response:', dashRes.data);
      console.log('Files response:', filesRes.data);
      console.log('Admin response:', adminRes.data);
      
      setDashboard(dashRes.data);
      setFiles(filesRes.data);
      setAdminData(adminRes.data);
      setLoading(false);
    } catch (error) {
      console.error('Failed to fetch dashboard data:', error);
      console.error('Error details:', error.response?.data || error.message);
      setLoading(false);
    }
  };

  const handlePhoneNumberUpdate = (newPhone) => {
    setAdminData({ ...adminData, phoneNumber: newPhone });
    setShowPhoneWarning(false);
    clearPhoneNumberWarning();
    toast.success('Phone number updated successfully!');
  };

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const handleDeleteFile = async (fileId) => {
    setDeleteLoading(true);
    setDeleteError('');

    try {
      await api.delete(`/files/${fileId}`);
      setDeletingFileId(null);
      fetchDashboardData();
    } catch (error) {
      console.error('Delete file error:', error);
      setDeleteError(error.response?.data?.message || 'Failed to delete file');
      setDeleteLoading(false);
    }
  };

  const handleEditFile = (file) => {
    setEditingFile(file);
  };

  if (loading) {
    return <div className="flex items-center justify-center min-h-screen">Loading...</div>;
  }

  return (
    <div className="min-h-screen bg-gray-100">
      {/* Debug panel removed: left intentionally blank for production */}
      {/* Navbar */}
      <nav className="bg-blue-600 text-white shadow-lg">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex justify-between items-center">
          <h1 className="text-2xl font-bold">FileVault - Admin Dashboard</h1>
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
        {showPhoneWarning && !adminData?.phoneNumber && (
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
        {adminData && (
          <div className="bg-white rounded-lg shadow p-6 mb-8">
            <PhoneNumberManager
              userId={adminData.id}
              userRole="ADMIN"
              currentPhoneNumber={adminData.phoneNumber}
              onPhoneNumberUpdate={handlePhoneNumberUpdate}
            />
          </div>
        )}

        {/* Dashboard Stats */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <div className="bg-white rounded-lg shadow p-6">
            <h3 className="text-gray-600 text-sm font-medium">Total Earnings</h3>
            <p className="text-3xl font-bold text-blue-600 mt-2">
              ${dashboard.totalEarnings || 0}
            </p>
          </div>
          <div className="bg-white rounded-lg shadow p-6">
            <h3 className="text-gray-600 text-sm font-medium">Files Uploaded</h3>
            <p className="text-3xl font-bold text-green-600 mt-2">
              {dashboard.totalFilesUploaded || 0}
            </p>
          </div>
          <div className="bg-white rounded-lg shadow p-6">
            <h3 className="text-gray-600 text-sm font-medium">Total Accesses</h3>
            <p className="text-3xl font-bold text-purple-600 mt-2">
              {dashboard.totalAccessesByAdmin || 0}
            </p>
          </div>
        </div>

        {/* Tabs */}
        <div className="bg-white rounded-lg shadow">
          <div className="border-b">
            <div className="flex">
              <button
                onClick={() => setActiveTab('dashboard')}
                className={`px-6 py-4 font-medium ${
                  activeTab === 'dashboard'
                    ? 'border-b-2 border-blue-600 text-blue-600'
                    : 'text-gray-600 hover:text-gray-800'
                }`}
              >
                Files
              </button>
              <button
                onClick={() => setActiveTab('upload')}
                className={`px-6 py-4 font-medium ${
                  activeTab === 'upload'
                    ? 'border-b-2 border-blue-600 text-blue-600'
                    : 'text-gray-600 hover:text-gray-800'
                }`}
              >
                Upload File
              </button>
              <button
                onClick={() => setActiveTab('requests')}
                className={`px-6 py-4 font-medium ${
                  activeTab === 'requests'
                    ? 'border-b-2 border-blue-600 text-blue-600'
                    : 'text-gray-600 hover:text-gray-800'
                }`}
              >
                Access Requests
              </button>
            </div>
          </div>

          <div className="p-6">
            {/* Files Section */}
            {activeTab === 'dashboard' && (
              <div>
                <h2 className="text-xl font-bold text-gray-800 mb-4">Your Files</h2>
                {files.length === 0 ? (
                  <p className="text-gray-500">No files uploaded yet</p>
                ) : (
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {files.map((file) => (
                      <div key={file.id} className="border rounded-lg p-4 hover:shadow-lg transition bg-white flex flex-col">
                        <h3 className="font-semibold text-gray-800 truncate mb-2">{file.originalFileName}</h3>
                        <p className="text-sm text-gray-600">Category: {file.categoryName}</p>
                        <p className="text-sm text-gray-600">Type: {file.accessType}</p>
                        {file.price && <p className="text-sm font-semibold text-green-600">Price: ${file.price}</p>}
                        {file.description && <p className="text-sm text-gray-700 mt-2 line-clamp-2">{file.description}</p>}
                        <p className="text-xs text-gray-500 mt-2">
                          {new Date(file.uploadedAt).toLocaleDateString()}
                        </p>
                        <div className="flex gap-2 mt-4">
                          <button
                            onClick={() => handleEditFile(file)}
                            className="flex-1 bg-blue-500 hover:bg-blue-600 text-white py-2 rounded text-sm font-medium"
                          >
                            Edit
                          </button>
                          <button
                            onClick={() => setDeletingFileId(file.id)}
                            className="flex-1 bg-red-500 hover:bg-red-600 text-white py-2 rounded text-sm font-medium"
                          >
                            Delete
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* Upload Section */}
            {activeTab === 'upload' && (
              <div>
                <h2 className="text-xl font-bold text-gray-800 mb-4">Upload New File</h2>
                <FileUpload onUploadSuccess={() => {
                  fetchDashboardData();
                  setActiveTab('dashboard');
                }} />
              </div>
            )}

            {/* Access Requests Section */}
            {activeTab === 'requests' && (
              <div>
                <h2 className="text-xl font-bold text-gray-800 mb-4">Access Requests</h2>
                <RequestManagement adminId={admin} onRefresh={() => fetchDashboardData()} />
              </div>
            )}
          </div>
        </div>

        {/* Edit File Modal */}
        {editingFile && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-lg shadow-lg p-6 max-w-md w-full max-h-[90vh] overflow-y-auto">
              <div className="flex justify-between items-center mb-4">
                <h3 className="text-xl font-bold">Edit File Details</h3>
                <button
                  onClick={() => setEditingFile(null)}
                  className="text-gray-500 hover:text-gray-700 text-2xl"
                >
                  ×
                </button>
              </div>
              <FileEditor
                file={editingFile}
                categories={categories}
                onEditSuccess={() => {
                  setEditingFile(null);
                  fetchDashboardData();
                }}
                onClose={() => setEditingFile(null)}
              />
            </div>
          </div>
        )}

        {/* Delete File Modal */}
        {deletingFileId && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-lg shadow-lg p-6 max-w-md w-full">
              <h3 className="text-xl font-bold text-gray-800 mb-4">Confirm Delete</h3>
              {deleteError && (
                <div className="bg-red-100 text-red-700 p-3 rounded mb-4">
                  {deleteError}
                </div>
              )}
              <p className="text-gray-600 mb-6">
                Are you sure you want to delete <strong>{files.find(f => f.id === deletingFileId)?.originalFileName}</strong>? This action cannot be undone.
              </p>
              <div className="flex gap-3">
                <button
                  onClick={() => handleDeleteFile(deletingFileId)}
                  disabled={deleteLoading}
                  className="flex-1 bg-red-600 text-white py-2 rounded hover:bg-red-700 disabled:bg-gray-500"
                >
                  {deleteLoading ? 'Deleting...' : 'Delete'}
                </button>
                <button
                  onClick={() => {
                    setDeletingFileId(null);
                    setDeleteError('');
                    setDeleteLoading(false);
                  }}
                  disabled={deleteLoading}
                  className="flex-1 bg-gray-300 text-gray-800 py-2 rounded hover:bg-gray-400 disabled:bg-gray-400"
                >
                  Cancel
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};


export default AdminDashboard;
