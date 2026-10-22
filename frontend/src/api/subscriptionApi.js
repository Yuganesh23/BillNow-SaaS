import axiosClient from './axiosClient';

/** The shop's subscription, or null when it has never subscribed. */
export const getSubscription = async () => {
  try {
    const { data } = await axiosClient.get('/subscription');
    return data;
  } catch (error) {
    if (error.response?.status === 404) return null;
    throw error;
  }
};

export const getUsage = async () => (await axiosClient.get('/subscription/usage')).data;

export const createSubscription = async (plan) => (await axiosClient.post('/subscription/create', { plan })).data;

export const verifySubscription = async (payment) => (await axiosClient.post('/subscription/verify', payment)).data;

export const cancelSubscription = async () => (await axiosClient.post('/subscription/cancel')).data;

export const changePlan = async (plan) => (await axiosClient.post('/subscription/change-plan', { plan })).data;

export const cancelPlanChange = async () => (await axiosClient.post('/subscription/change-plan/cancel')).data;

export const getCharges = async () => (await axiosClient.get('/subscription/charges')).data;

export const downloadChargeInvoice = (chargeId) =>
  axiosClient.get(`/subscription/charges/${chargeId}/invoice`, { responseType: 'blob' });
