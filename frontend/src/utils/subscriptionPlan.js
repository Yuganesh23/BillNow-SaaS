const RANK = { BASE: 1, PRO: 2, ENTERPRISE: 3 };

/**
 * Decides what a plan card's button does for the shop's current subscription.
 * Without an active subscription every plan goes through Razorpay Checkout.
 */
export function planAction(subscription, tier) {
  if (!subscription?.active) return { kind: 'checkout', label: `Choose ${tier}`, disabled: false };
  if (subscription.plan === tier) return { kind: 'current', label: 'Current plan', disabled: true };
  if (subscription.pendingPlan === tier) return { kind: 'pending', label: 'Change in progress', disabled: true };
  if (subscription.cancelAtPeriodEnd) return { kind: 'locked', label: `Choose ${tier}`, disabled: true };
  const upgrade = (RANK[tier] || 0) > (RANK[subscription.plan] || 0);
  return upgrade
    ? { kind: 'upgrade', label: `Upgrade to ${tier}`, disabled: false }
    : { kind: 'downgrade', label: `Switch to ${tier}`, disabled: false };
}

/** Reads the API error message from a JSON or blob (file download) error response. */
export async function apiErrorMessage(error, fallback) {
  const data = error?.response?.data;
  if (data && typeof data.text === 'function') {
    try { return JSON.parse(await data.text()).message || fallback; } catch { return fallback; }
  }
  return data?.message || error?.message || fallback;
}
