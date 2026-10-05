import axiosClient from './axiosClient';

const isMock = import.meta.env.VITE_USE_MOCK_API === 'true';

export const login = async (credentials) => {
  if (isMock) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          token: 'mock-jwt-token-12345',
          userId: 1,
          name: 'Demo Admin',
          email: credentials.email,
          role: 'ADMIN',
          shopId: 1
        });
      }, 500);
    });
  }
  const response = await axiosClient.post('/auth/login', credentials);
  return response.data;
};

export const resetPassword = async (data) => {
    const response = await axiosClient.post('/auth/reset-password', data);
    return response.data;
};