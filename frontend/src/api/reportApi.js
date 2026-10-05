import axiosClient from './axiosClient';

const isMock = import.meta.env.VITE_USE_MOCK_API === 'true';

const getDefaultDates = (startDate, endDate) => {
  const getLocalDateString = (date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };

  if (!startDate) {
    const today = new Date();
    startDate = getLocalDateString(new Date(today.getFullYear(), 0, 1));
  }
  if (!endDate) {
    endDate = getLocalDateString(new Date());
  }
  return { startDate, endDate };
}

export const getSalesReport = async (startDate, endDate) => {
  if (isMock) return { totalSales: 0, totalInvoices: 0, totalDiscount: 0, subtotal: 0 };
  const dates = getDefaultDates(startDate, endDate);
  const response = await axiosClient.get('/reports/sales', { params: { from: dates.startDate, to: dates.endDate } });
  return response.data;
};

export const getProductsReport = async (startDate, endDate) => {
  if (isMock) return [];
  const dates = getDefaultDates(startDate, endDate);
  const response = await axiosClient.get('/reports/products', { params: { from: dates.startDate, to: dates.endDate } });
  return response.data;
};

export const getCustomersReport = async (startDate, endDate) => {
  if (isMock) return [];
  const dates = getDefaultDates(startDate, endDate);
  const response = await axiosClient.get('/reports/customers', { params: { from: dates.startDate, to: dates.endDate } });
  return response.data;
};

export const getPaymentsReport = async (startDate, endDate) => {
  if (isMock) return { cashAmount: 0, upiAmount: 0, cardAmount: 0, otherAmount: 0, totalAmount: 0 };
  const dates = getDefaultDates(startDate, endDate);
  const response = await axiosClient.get('/reports/payments', { params: { from: dates.startDate, to: dates.endDate } });
  return response.data;
};

export const getNewCustomersReport = async (startDate, endDate) => {
  if (isMock) return [];
  const dates = getDefaultDates(startDate, endDate);
  const response = await axiosClient.get('/reports/customers/new', { params: { from: dates.startDate, to: dates.endDate } });
  return response.data;
};

