import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import FileViewer from '../components/FileViewer/FileViewer';
import PublicChatbot from '../components/Chat/PublicChatbot';

const HomePage = () => {
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedFile, setSelectedFile] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [showFileViewer, setShowFileViewer] = useState(false);
  const [downloadLoading, setDownloadLoading] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    fetchPublicFiles();
  }, []);

  const fetchPublicFiles = async () => {
    try {
      setLoading(true);
      const response = await axios.get('http://localhost:8080/api/files/public/sorted');
      setFiles(response.data);
      setError(null);
    } catch (err) {
      console.error('Error fetching files:', err);
      setError('Failed to load files. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  const handleViewFile = (file) => {
    setSelectedFile(file);
    setShowModal(true);
    // Increment view count
    axios.put(`http://localhost:8080/api/files/${file.id}/view`).catch(err => console.error('Error incrementing view:', err));
  };

  const handleRequestAccess = (file) => {
    const user = localStorage.getItem('user');
    if (!user) {
      navigate('/user/register');
    } else {
      setShowModal(false);
      // Redirect to file detail with request option
      navigate(`/file/${file.id}`);
    }
  };

  const handleDownloadFile = async (file) => {
    try {
      setDownloadLoading(true);
      const response = await axios.get(
        `http://localhost:8080/api/files/${file.id}/download`,
        {
          responseType: 'blob',
          headers: {
            Authorization: `Bearer ${localStorage.getItem('token')}`
          }
        }
      );
      
      // Create a temporary URL for the blob and trigger download
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', file.originalFileName);
      document.body.appendChild(link);
      link.click();
      link.parentElement.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Error downloading file:', err);
      alert('Failed to download file. You may need to request access or log in.');
    } finally {
      setDownloadLoading(false);
    }
  };

  const handleReadFile = (file) => {
    setSelectedFile(file);
    setShowFileViewer(true);
    setShowModal(false);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Navigation */}
      <nav className="bg-white shadow-lg sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex justify-between items-center">
            <h1 className="text-2xl font-bold text-blue-600 cursor-pointer" onClick={() => navigate('/')}>FileVault</h1>
            <div className="flex items-center space-x-4">
              {localStorage.getItem('user') ? (
                <>
                  <span className="text-gray-600">
                    {JSON.parse(localStorage.getItem('user')).firstName}
                  </span>
                  <button 
                    onClick={() => {
                      const userData = JSON.parse(localStorage.getItem('user'));
                      navigate(userData.role === 'ADMIN' ? '/admin/dashboard' : '/user/dashboard');
                    }}
                    className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 font-semibold"
                  >
                    Dashboard
                  </button>
                  <button 
                    onClick={() => {
                      localStorage.removeItem('token');
                      localStorage.removeItem('user');
                      window.location.reload();
                    }}
                    className="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700"
                  >
                    Logout
                  </button>
                </>
              ) : (
                <div className="flex items-center space-x-3">
                  <div className="border-l border-gray-300 pl-3">
                    <p className="text-sm text-gray-600 mb-2">Admin?</p>
                    <div className="space-x-2">
                      <Link to="/admin/login" className="bg-blue-500 text-white px-3 py-1 rounded text-sm hover:bg-blue-600">Login</Link>
                      <Link to="/admin/register" className="bg-blue-400 text-white px-3 py-1 rounded text-sm hover:bg-blue-500">Register</Link>
                    </div>
                  </div>
                  <div className="border-l border-gray-300 pl-3">
                    <p className="text-sm text-gray-600 mb-2">User?</p>
                    <div className="space-x-2">
                      <Link to="/user/login" className="bg-green-500 text-white px-3 py-1 rounded text-sm hover:bg-green-600">Login</Link>
                      <Link to="/user/register" className="bg-green-400 text-white px-3 py-1 rounded text-sm hover:bg-green-500">Register</Link>
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <div className="bg-gradient-to-r from-blue-500 to-purple-600 text-white py-12">
        <div className="max-w-7xl mx-auto px-4 text-center">
          <h2 className="text-4xl font-bold mb-4">Discover Public Content</h2>
          <p className="text-xl">Browse files uploaded by creators. Most viewed files are shown first.</p>
        </div>
      </div>

      {/* Main Content */}
      <div className="max-w-7xl mx-auto px-4 py-12">
        {loading ? (
          <div className="text-center py-12">
            <p className="text-lg text-gray-600">Loading files...</p>
          </div>
        ) : error ? (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
            {error}
          </div>
        ) : files.length === 0 ? (
          <div className="text-center py-12">
            <p className="text-lg text-gray-600">No public files available yet.</p>
          </div>
        ) : (
          <>
            <h3 className="text-2xl font-bold mb-6">
              Public Files ({files.length}) - Sorted by Views
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {files.map((file) => (
                <div key={file.id} className="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow overflow-hidden">
                  <div className="p-6">
                    <h4 className="text-lg font-semibold text-gray-800 truncate mb-2">
                      {file.originalFileName}
                    </h4>
                    <p className="text-sm text-gray-600 mb-2">
                      <strong>Category:</strong> {file.categoryName}
                    </p>
                    <p className="text-sm text-gray-600 mb-2">
                      <strong>Admin:</strong> {file.adminEmail}
                    </p>
                    <p className="text-sm text-gray-600 mb-2">
                      <strong>Type:</strong> {file.fileType}
                    </p>
                    <p className="text-sm text-gray-600 mb-2">
                      <strong>Size:</strong> {(file.fileSize / 1024).toFixed(2)} KB
                    </p>
                    <p className="text-sm text-blue-600 font-semibold mb-4">
                      👁️ {file.viewCount} views
                    </p>
                    {file.description && (
                      <p className="text-sm text-gray-700 mb-4 line-clamp-2">
                        {file.description}
                      </p>
                    )}
                    <button
                      onClick={() => handleViewFile(file)}
                      className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700 transition"
                    >
                      View Details
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </>
        )}
      </div>



      {/* File Details Modal */}
      {showModal && selectedFile && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full">
            <div className="p-6">
              <h3 className="text-2xl font-bold mb-4">{selectedFile.originalFileName}</h3>
              
              <div className="space-y-3 mb-6 text-sm">
                <div><strong>Category:</strong> {selectedFile.categoryName}</div>
                <div><strong>Admin:</strong> {selectedFile.adminEmail}</div>
                <div><strong>Type:</strong> {selectedFile.fileType}</div>
                <div><strong>Size:</strong> {(selectedFile.fileSize / 1024).toFixed(2)} KB</div>
                <div><strong>Views:</strong> {selectedFile.viewCount}</div>
                <div><strong>Access Type:</strong> {selectedFile.accessType}</div>
                {selectedFile.description && (
                  <div><strong>Description:</strong> {selectedFile.description}</div>
                )}
              </div>

              <div className="flex flex-col gap-2">
                {selectedFile.fileType === 'pdf' && (
                  <button
                    onClick={() => handleReadFile(selectedFile)}
                    className="w-full bg-purple-600 text-white py-2 rounded hover:bg-purple-700 transition"
                  >
                    📖 Read File
                  </button>
                )}
                <button
                  onClick={() => handleDownloadFile(selectedFile)}
                  disabled={downloadLoading}
                  className="w-full bg-green-600 text-white py-2 rounded hover:bg-green-700 disabled:opacity-50 transition"
                >
                  {downloadLoading ? 'Downloading...' : '⬇️ Download'}
                </button>
                {selectedFile.accessType === 'RESTRICTED' && !localStorage.getItem('user') ? (
                  <button
                    onClick={() => handleRequestAccess(selectedFile)}
                    className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700 transition"
                  >
                    🔒 Request Access
                  </button>
                ) : null}
                <button
                  onClick={() => setShowModal(false)}
                  className="w-full bg-gray-400 text-white py-2 rounded hover:bg-gray-500 transition"
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* File Viewer Modal */}
      {showFileViewer && selectedFile && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg shadow-xl w-full max-w-4xl max-h-screen overflow-auto">
            <div className="flex justify-between items-center p-4 border-b sticky top-0 bg-white">
              <h3 className="text-lg font-bold">{selectedFile.originalFileName}</h3>
              <button
                onClick={() => setShowFileViewer(false)}
                className="text-gray-600 hover:text-gray-800 text-2xl"
              >
                ✕
              </button>
            </div>
            <div className="p-4">
              <FileViewer fileId={selectedFile.id} />
            </div>
          </div>
        </div>
      )}

      {/* RAG & Search Public Chatbot */}
      <PublicChatbot onSelectFile={handleViewFile} />
    </div>
  );
};

export default HomePage;
