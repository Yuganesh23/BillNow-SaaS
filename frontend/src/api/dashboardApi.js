import axiosClient from './axiosClient';

const isMock = import.meta.env.VITE_USE_MOCK_API === 'true';

export const getDashboardData = async () => {
  if (isMock) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          todaySales: 0,
          totalSales: 0,
          todayInvoices: 0,
          totalInvoices: 0,
          totalCustomers: 0,
          totalProducts: 0,
          lowStockProducts: 0,
          cashPayments: 0,
          upiPayments: 0,
          cardPayments: 0,
          otherPayments: 0,
          recentInvoices: []
        });
      }, 500);
    });
  }
  const response = await axiosClient.get('/dashboard');
  return response.data;
};
