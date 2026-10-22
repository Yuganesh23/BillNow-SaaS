import { useCallback, useEffect, useState } from 'react';
import { Download, ShieldCheck } from 'lucide-react';
import { useBranch } from '../context/BranchContext';
import {
  cancelPlanChange, cancelSubscription, changePlan, createSubscription, downloadChargeInvoice,
  getCharges, getSubscription, getUsage, verifySubscription,
} from '../api/subscriptionApi';
import { filenameFromDisposition, saveBlob } from '../utils/fileDownload';
import { apiErrorMessage, planAction } from '../utils/subscriptionPlan';

const tiers = [
  { name: 'BASE', price: '4,999', saving: 'Save ₹989/year', description: 'Single shop', features: ['250 invoices/month', '2 active users', '1 branch', 'Standard PDF generation'] },
  { name: 'PRO', price: '9,999', saving: 'Save ₹1,989/year', description: 'Growing retail team', features: ['2,000 invoices/month', '10 active users', '5 branches', 'E-invoice access'], featured: true },
  { name: 'ENTERPRISE', price: '24,999', saving: 'Save ₹4,989/year', description: 'Wholesalers and multi-branch', features: ['100,000 invoices/month', '100 active users', '100 branches', 'E-invoice access'] },
];

const statusLabels = {
  ACTIVE: 'Active', AUTHENTICATION_PENDING: 'Payment pending', PAYMENT_FAILED: 'Payment failed', HALTED: 'Payment failed',
  CANCELLED: 'Cancelled', COMPLETED: 'Completed', EXPIRED: 'Expired', CREATED: 'Created',
};

const formatDate = (value) => (value ? new Date(value).toLocaleDateString() : '—');
const formatMoney = (value) => Number(value).toLocaleString('en-IN', { style: 'currency', currency: 'INR' });

const loadRazorpay = () => new Promise((resolve) => {
  if (window.Razorpay) return resolve(true);
  const script = document.createElement('script');
  script.src = 'https://checkout.razorpay.com/v1/checkout.js';
  script.onload = () => resolve(true);
  script.onerror = () => resolve(false);
  document.body.appendChild(script);
});

export default function Subscription() {
  const { branches, activeBranchId, fetchBranches } = useBranch();
  const [busy, setBusy] = useState(null);
  const [usage, setUsage] = useState(null);
  const [subscription, setSubscription] = useState(null);
  const [charges, setCharges] = useState([]);
  const [notice, setNotice] = useState('');
  const currentBranch = branches.find((branch) => String(branch.id) === String(activeBranchId));
  const currentPlan = currentBranch?.subscriptionTier || 'TRIAL';
  const expiry = currentBranch?.subscriptionEndsAt || currentBranch?.trialEndsAt;

  const refresh = useCallback(async () => {
    const [nextUsage, nextSubscription, nextCharges] = await Promise.all([
      getUsage().catch(() => null),
      getSubscription().catch(() => null),
      getCharges().catch(() => []),
    ]);
    setUsage(nextUsage);
    setSubscription(nextSubscription);
    setCharges(nextCharges);
  }, []);

  useEffect(() => {
    if (activeBranchId) refresh();
  }, [activeBranchId, refresh]);

  const afterChange = async (message) => {
    await Promise.all([refresh(), fetchBranches()]);
    setNotice(message);
  };

  const startCheckout = async (tier) => {
    if (!await loadRazorpay()) throw new Error('Unable to load Razorpay Checkout.');
    const data = await createSubscription(tier);
    const checkout = new window.Razorpay({
      key: data.razorpayKeyId,
      subscription_id: data.razorpaySubscriptionId,
      name: 'BillNow',
      description: `${tier} annual subscription`,
      prefill: { name: currentBranch.name, email: currentBranch.email, contact: currentBranch.mobileNumber },
      theme: { color: '#4FB28F' },
      handler: async (payment) => {
        try {
          await verifySubscription({
            razorpayPaymentId: payment.razorpay_payment_id,
            razorpaySubscriptionId: payment.razorpay_subscription_id,
            razorpaySignature: payment.razorpay_signature,
          });
          await afterChange(`Your ${tier} plan is active.`);
        } catch (error) {
          setNotice(await apiErrorMessage(error, 'Payment was received but verification failed. Contact support with your payment ID.'));
        } finally {
          setBusy(null);
        }
      },
      modal: { ondismiss: () => setBusy(null) },
    });
    checkout.on('payment.failed', (response) => {
      setBusy(null);
      setNotice(response.error?.description || 'Payment failed.');
    });
    checkout.open();
  };

  const handlePlan = async (tier) => {
    if (!currentBranch) return setNotice('Please select a branch first.');
    const action = planAction(subscription, tier);
    if (action.kind === 'downgrade' && !window.confirm(`Switch to ${tier} at the end of the current billing period?`)) return;
    if (action.kind === 'upgrade' && !window.confirm(`Upgrade to ${tier} now? Razorpay will charge your saved payment method.`)) return;
    setBusy(tier);
    setNotice('');
    try {
      if (action.kind === 'checkout') {
        await startCheckout(tier);
        return;
      }
      await changePlan(tier);
      await afterChange(action.kind === 'upgrade'
        ? `Upgrade to ${tier} requested. The new limits apply as soon as Razorpay confirms the payment.`
        : `Your plan will switch to ${tier} at the end of the current billing period.`);
      setBusy(null);
    } catch (error) {
      setBusy(null);
      setNotice(await apiErrorMessage(error, 'Unable to change the plan.'));
    }
  };

  const runAction = async (key, call, message, fallback) => {
    setBusy(key);
    setNotice('');
    try {
      await call();
      await afterChange(message);
    } catch (error) {
      setNotice(await apiErrorMessage(error, fallback));
    } finally {
      setBusy(null);
    }
  };

  const handleCancel = () => {
    if (!window.confirm('Cancel your subscription? You keep your current plan until the end of the paid period, and it will not renew.')) return;
    runAction('cancel', cancelSubscription, 'Your subscription will end at the end of the current period.', 'Unable to cancel the subscription.');
  };

  const handleKeepPlan = () => runAction('keep', cancelPlanChange, 'The scheduled plan change was cancelled.', 'Unable to cancel the plan change.');

  const handleDownload = async (charge) => {
    setBusy(`invoice-${charge.id}`);
    try {
      const response = await downloadChargeInvoice(charge.id);
      saveBlob(response.data, filenameFromDisposition(response.headers['content-disposition'], `billnow-invoice-${charge.id}.pdf`));
    } catch (error) {
      setNotice(await apiErrorMessage(error, 'Unable to download the invoice.'));
    } finally {
      setBusy(null);
    }
  };

  const active = subscription?.active;

  return (
    <div className="p-4 sm:p-6 lg:p-8 max-w-7xl mx-auto">
      <div className="mb-8"><h1 className="text-2xl font-bold text-slate-900">Manage Subscription</h1><p className="mt-2 text-sm text-slate-700">Secure annual billing powered by Razorpay.</p></div>
      {notice && <div role="status" className="mb-6 rounded-md bg-indigo-50 p-4 text-sm text-indigo-900 ring-1 ring-indigo-200">{notice}</div>}
      <div className="mb-8 flex flex-col gap-4 rounded-lg bg-white p-6 shadow-sm ring-1 ring-slate-200 md:flex-row md:items-center md:justify-between">
        <div>
          <h2 className="text-lg font-semibold text-slate-900">Shop: <span className="font-normal">{currentBranch?.name || 'Select a branch'}</span></h2>
          <p className="mt-2 text-sm text-slate-500">Current plan: <strong className="text-indigo-700">{currentPlan}</strong>{expiry ? ` · valid until ${formatDate(expiry)}` : ''}</p>
          {subscription && <p className="mt-1 text-sm text-slate-500">Subscription status: <strong className="text-slate-800">{statusLabels[subscription.status] || subscription.status}</strong>{active && !subscription.cancelAtPeriodEnd && subscription.nextBillingDate ? ` · renews on ${formatDate(subscription.nextBillingDate)}` : ''}</p>}
          {active && subscription.cancelAtPeriodEnd && <p className="mt-2 text-sm font-medium text-amber-700">Cancelled. Your plan stays active until {formatDate(subscription.endDate)} and will not renew.</p>}
          {active && subscription.pendingPlan && <p className="mt-2 text-sm font-medium text-amber-700">Switching to {subscription.pendingPlan}. <button type="button" onClick={handleKeepPlan} disabled={busy !== null} className="underline disabled:opacity-50">Keep {subscription.plan}</button></p>}
          {active && !subscription.cancelAtPeriodEnd && <button type="button" onClick={handleCancel} disabled={busy !== null} className="mt-3 text-sm font-semibold text-red-700 hover:text-red-600 disabled:opacity-50">{busy === 'cancel' ? 'Cancelling…' : 'Cancel subscription'}</button>}
        </div>
        {usage && <div className="grid grid-cols-3 gap-4 text-center text-sm"><div><strong className="block text-slate-900">{usage.invoicesThisMonth}/{usage.limits.invoicesPerMonth}</strong><span className="text-slate-500">Invoices</span></div><div><strong className="block text-slate-900">{usage.activeUsers}/{usage.limits.users}</strong><span className="text-slate-500">Users</span></div><div><strong className="block text-slate-900">{usage.branches}/{usage.limits.branches}</strong><span className="text-slate-500">Branches</span></div></div>}
      </div>
      <div className="grid grid-cols-1 gap-8 md:grid-cols-3">
        {tiers.map((tier) => {
          const action = planAction(subscription, tier.name);
          return <article key={tier.name} className={`relative flex flex-col rounded-2xl bg-white p-8 ${tier.featured ? 'ring-2 ring-indigo-600 shadow-xl' : 'ring-1 ring-slate-200'}`}>
            {tier.featured && <span className="absolute right-6 top-0 -translate-y-1/2 rounded-full bg-indigo-600 px-3 py-1 text-xs font-bold uppercase tracking-wide text-white">Most popular</span>}
            <h3 className="text-xl font-bold text-slate-900">{tier.name}</h3><p className="mt-2 text-sm text-slate-500">{tier.description}</p>
            <p className="mt-6"><span className="text-4xl font-bold text-slate-900">₹{tier.price}</span><span className="text-sm font-semibold text-slate-500">/year</span></p><p className="mt-1 text-sm font-semibold text-green-700">{tier.saving}</p>
            <button onClick={() => handlePlan(tier.name)} disabled={busy !== null || action.disabled} className={`mt-8 w-full rounded-md py-2.5 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-50 ${tier.featured ? 'bg-indigo-600 text-white hover:bg-indigo-500' : 'text-indigo-600 ring-1 ring-indigo-200 hover:ring-indigo-300'}`}>{busy === tier.name ? 'Please wait…' : action.label}</button>
            <ul className="mt-8 space-y-3 text-sm text-slate-600">{tier.features.map((feature) => <li key={feature} className="flex gap-3"><ShieldCheck className="h-5 w-5 shrink-0 text-indigo-600" />{feature}</li>)}</ul>
          </article>;
        })}
      </div>
      <section className="mt-10 rounded-lg bg-white p-6 shadow-sm ring-1 ring-slate-200">
        <h2 className="text-lg font-semibold text-slate-900">Billing history</h2>
        {charges.length === 0
          ? <p className="mt-2 text-sm text-slate-500">No subscription payments yet.</p>
          : <div className="mt-4 overflow-x-auto"><table className="min-w-full text-left text-sm">
            <thead className="text-slate-500"><tr><th className="py-2 pr-4 font-medium">Paid on</th><th className="py-2 pr-4 font-medium">Plan</th><th className="py-2 pr-4 font-medium">Period</th><th className="py-2 pr-4 font-medium text-right">Amount</th><th className="py-2 pr-4 font-medium">Invoice</th></tr></thead>
            <tbody className="divide-y divide-slate-100">{charges.map((charge) => <tr key={charge.id}>
              <td className="py-2 pr-4 text-slate-900">{formatDate(charge.paidAt)}</td>
              <td className="py-2 pr-4">{charge.plan}</td>
              <td className="py-2 pr-4 text-slate-500">{charge.periodStart ? `${formatDate(charge.periodStart)} – ${formatDate(charge.periodEnd)}` : '—'}</td>
              <td className="py-2 pr-4 text-right text-slate-900">{formatMoney(charge.totalAmount)}{charge.taxAmount != null && <span className="block text-xs text-slate-500">incl. GST {formatMoney(charge.taxAmount)}</span>}</td>
              <td className="py-2 pr-4">{charge.invoiceAvailable
                ? <button type="button" onClick={() => handleDownload(charge)} disabled={busy !== null} className="inline-flex items-center gap-1 font-semibold text-indigo-600 hover:text-indigo-500 disabled:opacity-50"><Download className="h-4 w-4" />{charge.invoiceNumber || 'Download'}</button>
                : <span className="text-slate-500">Available soon</span>}</td>
            </tr>)}</tbody>
          </table></div>}
      </section>
    </div>
  );
}
