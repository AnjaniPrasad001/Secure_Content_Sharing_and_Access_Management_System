import axios from 'axios';

// Use REACT_APP_API_URL when set (production), otherwise use relative '/api'
// so the dev server proxy (package.json "proxy") forwards requests to the backend
const API_BASE_URL = process.env.REACT_APP_API_URL || '/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add token to every request
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    // Debug: log outgoing requests and whether Authorization header is present
    try {
      // eslint-disable-next-line no-console
      console.log('[API REQUEST]', config.method?.toUpperCase(), config.url, 'Auth:', !!config.headers.Authorization);
    } catch (e) {}
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Handle response errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Debug: log response errors and avoid automatic redirect to root to debug 401 sources
    try {
      const status = error.response?.status;
      const url = error.config?.url;
      // eslint-disable-next-line no-console
      console.error('[API RESPONSE ERROR]', status, url, error.response?.data || error.message);
      if (status === 401) {
        // For debugging: do not remove token here so you can inspect Network/localStorage.
        // Later we can re-enable auto-logout. Emit an event for UI handling.
        window.dispatchEvent(new CustomEvent('app:unauthorized', { detail: { url, status } }));
      }
    } catch (e) {}
    return Promise.reject(error);
  }
);

export default api;
