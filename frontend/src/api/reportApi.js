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

const reportDates = (startDate, endDate) => {
  const dates = getDefaultDates(startDate, endDate);
  return { from: dates.startDate, to: dates.endDate };
};

export const getRegister = async (type, startDate, endDate, filters = {}) => {
  const response = await axiosClient.get(`/reports/v2/registers/${type}`, {
    params: { ...reportDates(startDate, endDate), ...filters },
  });
  return response.data;
};

export const getGstr1 = async (startDate, endDate, b2clThreshold) => {
  const response = await axiosClient.get('/reports/v2/gst/gstr1', {
    params: { ...reportDates(startDate, endDate), b2clThreshold },
  });
  return response.data;
};

export const getGstr3b = async (startDate, endDate) => {
  const response = await axiosClient.get('/reports/v2/gst/gstr3b', { params: reportDates(startDate, endDate) });
  return response.data;
};

export const getStockReport = async (lowOnly = false) => {
  const response = await axiosClient.get('/reports/v2/inventory/stock', { params: { lowOnly } });
  return response.data;
};

export const getStockMovements = async (startDate, endDate, productId) => {
  const response = await axiosClient.get('/reports/v2/inventory/movements', {
    params: { ...reportDates(startDate, endDate), productId },
  });
  return response.data;
};

export const getBusinessReport = async (startDate, endDate) => {
  const response = await axiosClient.get('/reports/v2/business', { params: reportDates(startDate, endDate) });
  return response.data;
};

export const getPaymentRegister = async (startDate, endDate) => {
  const response = await axiosClient.get('/reports/v2/payments', { params: reportDates(startDate, endDate) });
  return response.data;
};

export const getDayBook = async (startDate, endDate) => {
  const response = await axiosClient.get('/reports/v2/day-book', { params: reportDates(startDate, endDate) });
  return response.data;
};

export const downloadReport = async (path, startDate, endDate, filename, extra = {}) => {
  const response = await axiosClient.get(path, {
    params: { ...reportDates(startDate, endDate), ...extra }, responseType: 'blob',
  });
  const url = URL.createObjectURL(response.data);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
};

