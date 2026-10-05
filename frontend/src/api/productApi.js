import axiosClient from './axiosClient';

const isMock = import.meta.env.VITE_USE_MOCK_API === 'true';

let mockProducts = [];

export const getProducts = async () => {
  if (isMock) {
    return new Promise(resolve => setTimeout(() => resolve(mockProducts), 500));
  }
  const response = await axiosClient.get('/products');
  return response.data;
};

export const getProductById = async (id) => {
  if (isMock) {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const product = mockProducts.find(p => p.id === parseInt(id));
        if (product) resolve(product);
        else reject(new Error('Not found'));
      }, 500);
    });
  }
  const response = await axiosClient.get(`/products/${id}`);
  return response.data;
};

export const createProduct = async (productData) => {
  if (isMock) {
    return new Promise(resolve => {
      setTimeout(() => {
        const newProduct = { id: Date.now(), ...productData };
        mockProducts.push(newProduct);
        resolve(newProduct);
      }, 500);
    });
  }
  const response = await axiosClient.post('/products', productData);
  return response.data;
};

export const updateProduct = async (id, productData) => {
  if (isMock) {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const index = mockProducts.findIndex(p => p.id === parseInt(id));
        if (index !== -1) {
          mockProducts[index] = { ...mockProducts[index], ...productData };
          resolve(mockProducts[index]);
        } else {
          reject(new Error('Not found'));
        }
      }, 500);
    });
  }
  const response = await axiosClient.put(`/products/${id}`, productData);
  return response.data;
};

export const deleteProduct = async (id) => {
  if (isMock) {
    return new Promise((resolve) => {
      setTimeout(() => {
        mockProducts = mockProducts.filter(p => p.id !== parseInt(id));
        resolve();
      }, 500);
    });
  }
  const response = await axiosClient.delete(`/products/${id}`);
  return response.data;
};
