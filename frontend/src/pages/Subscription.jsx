import { useEffect, useState } from 'react';
import { ShieldCheck } from 'lucide-react';
import { useBranch } from '../context/BranchContext';
import axiosClient from '../api/axiosClient';

const tiers = [
  { name: 'BASE', price: '4,999', saving: 'Save ₹989/year', description: 'Single shop', features: ['250 invoices/month', '2 active users', '1 branch', 'Standard PDF generation'] },
  { name: 'PRO', price: '9,999', saving: 'Save ₹1,989/year', description: 'Growing retail team', features: ['2,000 invoices/month', '10 active users', '5 branches', 'E-invoice access'], featured: true },
  { name: 'ENTERPRISE', price: '24,999', saving: 'Save ₹4,989/year', description: 'Wholesalers and multi-branch', features: ['100,000 invoices/month', '100 active users', '100 branches', 'E-invoice access'] },
];

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
  const [upgradingTier, setUpgradingTier] = useState(null);
  const [usage, setUsage] = useState(null);
  const currentBranch = branches.find((branch) => String(branch.id) === String(activeBranchId));
  const currentPlan = currentBranch?.subscriptionTier || 'TRIAL';
  const expiry = currentBranch?.subscriptionEndsAt || currentBranch?.trialEndsAt;

  useEffect(() => {
    if (!activeBranchId) return;
    axiosClient.get('/subscription/usage').then(({ data }) => setUsage(data)).catch(() => setUsage(null));
  }, [activeBranchId]);

  const handleUpgrade = async (tier) => {
    if (!currentBranch) return alert('Please select a branch first.');
    setUpgradingTier(tier);
    try {
      if (!await loadRazorpay()) throw new Error('Unable to load Razorpay Checkout.');
      const { data } = await axiosClient.post('/subscription/create', { plan: tier });
      const checkout = new window.Razorpay({
        key: data.razorpayKeyId,
        subscription_id: data.razorpaySubscriptionId,
        name: 'BillNow',
        description: `${tier} annual subscription`,
        prefill: { name: currentBranch.name, email: currentBranch.email, contact: currentBranch.mobileNumber },
        theme: { color: '#4FB28F' },
        handler: async (payment) => {
          try {
            await axiosClient.post('/subscription/verify', {
              razorpayPaymentId: payment.razorpay_payment_id,
              razorpaySubscriptionId: payment.razorpay_subscription_id,
              razorpaySignature: payment.razorpay_signature,
            });
            await fetchBranches();
            alert(`Your ${tier} plan is active.`);
          } catch (error) {
            alert(error.response?.data?.message || 'Payment was received but verification failed. Contact support with your payment ID.');
          } finally {
            setUpgradingTier(null);
          }
        },
        modal: { ondismiss: () => setUpgradingTier(null) },
      });
      checkout.on('payment.failed', (response) => {
        setUpgradingTier(null);
        alert(response.error?.description || 'Payment failed.');
      });
      checkout.open();
    } catch (error) {
      setUpgradingTier(null);
      alert(error.response?.data?.message || error.message || 'Unable to start checkout.');
    }
  };

  return (
    <div className="p-4 sm:p-6 lg:p-8 max-w-7xl mx-auto">
      <div className="mb-8"><h1 className="text-2xl font-bold text-slate-900">Manage Subscription</h1><p className="mt-2 text-sm text-slate-700">Secure annual billing powered by Razorpay.</p></div>
      <div className="mb-8 flex flex-col gap-4 rounded-lg bg-white p-6 shadow-sm ring-1 ring-slate-200 md:flex-row md:items-center md:justify-between">
        <div><h2 className="text-lg font-semibold text-slate-900">Shop: <span className="font-normal">{currentBranch?.name || 'Select a branch'}</span></h2><p className="mt-2 text-sm text-slate-500">Current plan: <strong className="text-indigo-700">{currentPlan}</strong>{expiry ? ` · valid until ${new Date(expiry).toLocaleDateString()}` : ''}</p></div>
        {usage && <div className="grid grid-cols-3 gap-4 text-center text-sm"><div><strong className="block text-slate-900">{usage.invoicesThisMonth}/{usage.limits.invoicesPerMonth}</strong><span className="text-slate-500">Invoices</span></div><div><strong className="block text-slate-900">{usage.activeUsers}/{usage.limits.users}</strong><span className="text-slate-500">Users</span></div><div><strong className="block text-slate-900">{usage.branches}/{usage.limits.branches}</strong><span className="text-slate-500">Branches</span></div></div>}
      </div>
      <div className="grid grid-cols-1 gap-8 md:grid-cols-3">
        {tiers.map((tier) => {
          const isCurrent = currentPlan === tier.name;
          return <article key={tier.name} className={`relative flex flex-col rounded-2xl bg-white p-8 ${tier.featured ? 'ring-2 ring-indigo-600 shadow-xl' : 'ring-1 ring-slate-200'}`}>
            {tier.featured && <span className="absolute right-6 top-0 -translate-y-1/2 rounded-full bg-indigo-600 px-3 py-1 text-xs font-bold uppercase tracking-wide text-white">Most popular</span>}
            <h3 className="text-xl font-bold text-slate-900">{tier.name}</h3><p className="mt-2 text-sm text-slate-500">{tier.description}</p>
            <p className="mt-6"><span className="text-4xl font-bold text-slate-900">₹{tier.price}</span><span className="text-sm font-semibold text-slate-500">/year</span></p><p className="mt-1 text-sm font-semibold text-green-700">{tier.saving}</p>
            <button onClick={() => handleUpgrade(tier.name)} disabled={upgradingTier !== null || isCurrent} className={`mt-8 w-full rounded-md py-2.5 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-50 ${tier.featured ? 'bg-indigo-600 text-white hover:bg-indigo-500' : 'text-indigo-600 ring-1 ring-indigo-200 hover:ring-indigo-300'}`}>{isCurrent ? 'Current plan' : upgradingTier === tier.name ? 'Opening checkout…' : `Choose ${tier.name}`}</button>
            <ul className="mt-8 space-y-3 text-sm text-slate-600">{tier.features.map((feature) => <li key={feature} className="flex gap-3"><ShieldCheck className="h-5 w-5 shrink-0 text-indigo-600" />{feature}</li>)}</ul>
          </article>;
        })}
      </div>
    </div>
  );
}
