import axiosClient from './axiosClient';

const isMock = import.meta.env.VITE_USE_MOCK_API === 'true';

let mockCustomers = [];

export const getCustomers = async () => {
  if (isMock) {
    return new Promise(resolve => setTimeout(() => resolve(mockCustomers), 500));
  }
  const response = await axiosClient.get('/customers');
  return response.data;
};

export const getCustomerById = async (id) => {
  if (isMock) {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const customer = mockCustomers.find(c => c.id === parseInt(id));
        if (customer) resolve(customer);
        else reject(new Error('Not found'));
      }, 500);
    });
  }
  const response = await axiosClient.get(`/customers/${id}`);
  return response.data;
};

export const createCustomer = async (customerData) => {
  if (isMock) {
    return new Promise(resolve => {
      setTimeout(() => {
        const newCustomer = { id: Date.now(), ...customerData };
        mockCustomers.push(newCustomer);
        resolve(newCustomer);
      }, 500);
    });
  }
  const response = await axiosClient.post('/customers', customerData);
  return response.data;
};

export const updateCustomer = async (id, customerData) => {
  if (isMock) {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const index = mockCustomers.findIndex(c => c.id === parseInt(id));
        if (index !== -1) {
          mockCustomers[index] = { ...mockCustomers[index], ...customerData };
          resolve(mockCustomers[index]);
        } else {
          reject(new Error('Not found'));
        }
      }, 500);
    });
  }
  const response = await axiosClient.put(`/customers/${id}`, customerData);
  return response.data;
};

export const deleteCustomer = async (id) => {
  if (isMock) {
    return new Promise((resolve) => {
      setTimeout(() => {
        mockCustomers = mockCustomers.filter(c => c.id !== parseInt(id));
        resolve();
      }, 500);
    });
  }
  const response = await axiosClient.delete(`/customers/${id}`);
  return response.data;
};
