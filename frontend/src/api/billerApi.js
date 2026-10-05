import axiosClient from './axiosClient';

const isMock = import.meta.env.VITE_USE_MOCK_API === 'true';

let mockBillers = [];

export const getBillers = async () => {
  if (isMock) {
    return new Promise(resolve => setTimeout(() => resolve(mockBillers), 500));
  }
  const response = await axiosClient.get('/billers');
  return response.data;
};

export const createBiller = async (billerData) => {
  if (isMock) {
    return new Promise(resolve => {
      setTimeout(() => {
        const newBiller = { id: Date.now(), role: 'BILLER', active: true, ...billerData };
        mockBillers.push(newBiller);
        resolve(newBiller);
      }, 500);
    });
  }
  const response = await axiosClient.post('/billers', billerData);
  return response.data;
};

export const deactivateBiller = async (id) => {
  if (isMock) {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const index = mockBillers.findIndex(b => b.id === parseInt(id));
        if (index !== -1) {
          mockBillers[index].active = false;
          resolve(mockBillers[index]);
        } else {
          reject(new Error('Not found'));
        }
      }, 500);
    });
  }
  const response = await axiosClient.patch(`/billers/${id}/deactivate`);
  return response.data;
};
export const updateBiller = async (id, billerData) => {
  const response = await axiosClient.put('/billers/' + id, billerData);
  return response.data;
};
