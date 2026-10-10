import axios from 'axios';

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

axiosClient.interceptors.request.use((config) => {
  // Token is now securely handled via HttpOnly Cookies automatically
  
  const branchId = localStorage.getItem('activeBranchId');
  if (branchId) {
    config.headers['X-Branch-Id'] = branchId;
  }
  return config;
});

let refreshPromise = null;

axiosClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    const status = error.response?.status;
    const isAuthEndpoint = original?.url?.startsWith('/auth/');

    if (status === 401 && !isAuthEndpoint && !original?._retried) {
      original._retried = true;
      refreshPromise ??= axiosClient.post('/auth/refresh').finally(() => {
        refreshPromise = null;
      });
      try {
        await refreshPromise;
        return axiosClient(original);
      } catch {
        // The refresh failure is handled below by the refresh request itself.
      }
    }

    if ((status === 401 || status === 403) && original?.url !== '/auth/logout') {
      localStorage.removeItem('user');
      window.dispatchEvent(new Event('auth:unauthorized'));
    }
    return Promise.reject(error);
  }
);

export default axiosClient;
