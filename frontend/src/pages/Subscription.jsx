import React, { useState } from 'react';
import { ShieldCheck } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useBranch } from '../context/BranchContext';
import axiosClient from '../api/axiosClient';
import { ButtonSpinner } from '../components/common/XenBillLoader';

export default function Subscription() {
  const { user } = useAuth();
  const { branches, activeBranchId, fetchBranches } = useBranch();
  
  // Track billing cycle: 'MONTHLY' or 'ANNUAL'
  const [billingCycle, setBillingCycle] = useState('MONTHLY');

  // Track specifically which tier is being upgraded to show the correct loading button
  const [upgradingTier, setUpgradingTier] = useState(null);
  
  const currentBranch = branches.find(b => b.id.toString() === activeBranchId);
  
  const currentPlan = currentBranch?.subscriptionTier || "TRIAL";
  const highlightedTier = (() => {
    if (currentPlan === 'PRO') return 'ENTERPRISE';
    if (currentPlan === 'ENTERPRISE') return 'ENTERPRISE';
    return 'PRO';
  })();
  
  let expires = new Date();
  if (currentBranch?.subscriptionEndsAt) {
      expires = new Date(currentBranch.subscriptionEndsAt);
  } else if (currentBranch?.trialEndsAt) {
      expires = new Date(currentBranch.trialEndsAt);
  } else {
      expires.setDate(expires.getDate() + 7);
  }

  const handleUpgrade = async (tier, price, cycle = billingCycle) => {
    if (!currentBranch) {
        alert("Please select a branch first.");
        return;
    }

    if (!window.Razorpay) {
        alert("Razorpay Checkout SDK is still loading. Please check your internet connection or reload the page.");
        return;
    }
    
    try {
        setUpgradingTier(tier);
        
        // 1. Create Razorpay order on backend
        const { data: orderData } = await axiosClient.post('/subscription/create-order', {
            tier,
            branchId: currentBranch.id,
            amount: price,
            billingCycle: cycle
        });

        // 2. Open standard Razorpay Checkout window
        const options = {
            key: orderData.keyId,
            amount: orderData.amount,
            currency: orderData.currency || "INR",
            name: "XenBill",
            description: `${tier} ${cycle === 'ANNUAL' ? 'Annual Plan (1 Year)' : 'Monthly Plan (30 Days)'}`,
            image: "/favicon.svg",
            order_id: orderData.orderId,
            handler: async function (response) {
                try {
                    setUpgradingTier(tier);
                    // 3. Cryptographically verify payment on backend & activate tier
                    await axiosClient.post('/subscription/verify-order', {
                        razorpayOrderId: response.razorpay_order_id,
                        razorpayPaymentId: response.razorpay_payment_id,
                        razorpaySignature: response.razorpay_signature,
                        branchId: currentBranch.id,
                        tier: tier,
                        billingCycle: cycle
                    });

                    alert(`Payment successful! Your shop has been upgraded to ${tier} (${cycle === 'ANNUAL' ? 'Annual' : 'Monthly'}).`);
                    await fetchBranches();
                    window.location.reload();
                } catch (verifyError) {
                    console.error("Payment verification failed:", verifyError);
                    const msg = verifyError.response?.data?.message || "Payment verification failed. Please contact support.";
                    alert(msg);
                } finally {
                    setUpgradingTier(null);
                }
            },
            prefill: {
                name: user?.name || "",
                email: user?.email || "",
                contact: currentBranch?.mobileNumber || user?.mobileNumber || ""
            },
            theme: {
                color: "#4f46e5"
            },
            modal: {
                ondismiss: function () {
                    setUpgradingTier(null);
                }
            }
        };

        const rzp = new window.Razorpay(options);
        rzp.on('payment.failed', function (resp) {
            console.error("Razorpay payment failed:", resp.error);
            alert(`Payment failed: ${resp.error?.description || resp.error?.reason || 'Transaction could not be completed'}`);
            setUpgradingTier(null);
        });
        rzp.open();
        
    } catch (e) {
        console.error("Order creation failed:", e);
        const errorMsg = e.response?.data?.message 
            || e.response?.data?.error 
            || (e.response?.status === 500 ? "Backend service error (500). Please ensure Cloud Run backend is deployed." : e.message) 
            || "Failed to initiate payment.";
        alert(errorMsg);
        setUpgradingTier(null);
    }
  };

  return (
    <div className="p-4 sm:p-6 lg:p-8 max-w-7xl mx-auto">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-slate-900">Manage Subscription</h1>
        <p className="mt-2 text-sm text-slate-700">View your current plan and upgrade to unlock premium features.</p>
      </div>

      <div className="bg-white rounded-lg shadow-sm ring-1 ring-slate-200 p-6 mb-8 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h2 className="text-lg font-semibold text-slate-900 flex items-center">
            Shop: <span className="ml-2 font-normal">{currentBranch ? currentBranch.name : 'Loading...'}</span>
          </h2>
          <h2 className="text-lg font-semibold text-slate-900 flex items-center mt-2">
            Current Plan: <span className="ml-2 px-3 py-1 bg-indigo-100 text-indigo-700 rounded-full text-sm font-bold uppercase tracking-wider">{currentPlan}</span>
          </h2>
          <p className="text-sm text-slate-500 mt-2">
            Your {currentPlan === 'TRIAL' ? 'free trial' : 'subscription'} will expire on <span className="font-semibold">{expires.toLocaleDateString()}</span>.
          </p>
        </div>
      </div>

      {/* BILLING CYCLE SELECTOR */}
      <div className="flex justify-center mb-8">
        <div className="bg-slate-100 p-1.5 rounded-2xl inline-flex items-center shadow-inner">
          <button
            type="button"
            onClick={() => setBillingCycle('MONTHLY')}
            className={`px-6 py-2.5 text-sm font-semibold rounded-xl transition-all ${
              billingCycle === 'MONTHLY'
                ? 'bg-white text-slate-900 shadow-sm'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Monthly Billing
          </button>
          <button
            type="button"
            onClick={() => setBillingCycle('ANNUAL')}
            className={`px-6 py-2.5 text-sm font-semibold rounded-xl transition-all flex items-center gap-2 ${
              billingCycle === 'ANNUAL'
                ? 'bg-white text-slate-900 shadow-sm'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Annual Billing
            <span className="bg-emerald-100 text-emerald-700 text-xs font-bold px-2 py-0.5 rounded-full uppercase tracking-wider">
              Save ~17%
            </span>
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        
        {/* BASE TIER */}
        <div className={`bg-white rounded-2xl p-8 flex flex-col relative ${highlightedTier === 'BASE' ? 'ring-2 ring-indigo-600 shadow-xl' : 'ring-1 ring-slate-200'}`}>
          {highlightedTier === 'BASE' && (
            <div className="absolute top-0 right-6 transform -translate-y-1/2">
              <span className="bg-indigo-600 text-white text-xs font-bold px-3 py-1 rounded-full uppercase tracking-wide">Most Popular</span>
            </div>
          )}
          <h3 className={`text-xl font-bold ${highlightedTier === 'BASE' ? 'text-indigo-600' : 'text-slate-900'}`}>BASE</h3>
          <p className="mt-4 text-sm text-slate-500 flex-grow">Perfect for single shops needing fast, reliable billing.</p>
          <div className="mt-6">
            <p className="flex items-baseline gap-x-1">
              <span className="text-4xl font-bold tracking-tight text-slate-900">
                {billingCycle === 'ANNUAL' ? '₹4,999' : '₹499'}
              </span>
              <span className="text-sm font-semibold text-slate-500">
                {billingCycle === 'ANNUAL' ? '/year' : '/month'}
              </span>
            </p>
            {billingCycle === 'ANNUAL' && (
              <p className="text-xs text-emerald-600 font-medium mt-1">₹416/mo · Save ₹989 per year</p>
            )}
          </div>
          <button 
            onClick={() => handleUpgrade('BASE', billingCycle === 'ANNUAL' ? 4999 : 499)}
            disabled={upgradingTier !== null || (currentPlan === 'BASE' && billingCycle === 'MONTHLY')}
            className={`mt-8 block w-full rounded-md py-2.5 px-3 text-center text-sm font-semibold transition-all ${
              currentPlan === 'BASE' && billingCycle === 'MONTHLY' ? 'bg-slate-100 text-slate-400 cursor-not-allowed' :
              highlightedTier === 'BASE' ? 'bg-indigo-600 text-white shadow-sm hover:bg-indigo-500' : 'text-indigo-600 ring-1 ring-inset ring-indigo-200 hover:ring-indigo-300'
            }`}>
            {currentPlan === 'BASE' && billingCycle === 'MONTHLY' ? 'Current Plan' : 
             currentPlan === 'BASE' && billingCycle === 'ANNUAL' ? 'Switch to Annual' : 
             upgradingTier === 'BASE' ? <span className="inline-flex items-center justify-center"><ButtonSpinner className="mr-2 text-current" />Processing...</span> : 'Subscribe to Base'}
          </button>
          <ul className="mt-8 space-y-3 text-sm text-slate-600">
            <li className="flex gap-x-3"><ShieldCheck className="h-5 w-5 text-indigo-600" /> Unlimited Invoices</li>
            <li className="flex gap-x-3"><ShieldCheck className="h-5 w-5 text-indigo-600" /> Standard PDF generation</li>
            <li className="flex gap-x-3"><ShieldCheck className="h-5 w-5 text-indigo-600" /> 1 User (Admin)</li>
          </ul>
        </div>

        {/* PRO TIER */}
        <div className={`bg-white rounded-2xl p-8 flex flex-col relative ${highlightedTier === 'PRO' ? 'ring-2 ring-indigo-600 shadow-xl' : 'ring-1 ring-slate-200'}`}>
          {highlightedTier === 'PRO' && (
            <div className="absolute top-0 right-6 transform -translate-y-1/2">
              <span className="bg-indigo-600 text-white text-xs font-bold px-3 py-1 rounded-full uppercase tracking-wide">Most Popular</span>
            </div>
          )}
          <h3 className={`text-xl font-bold ${highlightedTier === 'PRO' ? 'text-indigo-600' : 'text-slate-900'}`}>PRO</h3>
          <p className="mt-4 text-sm text-slate-500 flex-grow">Advanced features for growing retail shops.</p>
          <div className="mt-6">
            <p className="flex items-baseline gap-x-1">
              <span className="text-4xl font-bold tracking-tight text-slate-900">
                {billingCycle === 'ANNUAL' ? '₹9,999' : '₹999'}
              </span>
              <span className="text-sm font-semibold text-slate-500">
                {billingCycle === 'ANNUAL' ? '/year' : '/month'}
              </span>
            </p>
            {billingCycle === 'ANNUAL' && (
              <p className="text-xs text-emerald-600 font-medium mt-1">₹833/mo · Save ₹1,989 per year</p>
            )}
          </div>
          <button 
            onClick={() => handleUpgrade('PRO', billingCycle === 'ANNUAL' ? 9999 : 999)}
            disabled={upgradingTier !== null || (currentPlan === 'PRO' && billingCycle === 'MONTHLY') || currentPlan === 'ENTERPRISE'}
            className={`mt-8 block w-full rounded-md py-2.5 px-3 text-center text-sm font-semibold transition-all ${
              (currentPlan === 'PRO' && billingCycle === 'MONTHLY') || currentPlan === 'ENTERPRISE' ? 'bg-slate-100 text-slate-400 cursor-not-allowed' :
              highlightedTier === 'PRO' ? 'bg-indigo-600 text-white shadow-sm hover:bg-indigo-500' : 'text-indigo-600 ring-1 ring-inset ring-indigo-200 hover:ring-indigo-300'
            }`}>
            {currentPlan === 'PRO' && billingCycle === 'MONTHLY' ? 'Current Plan' : 
             currentPlan === 'PRO' && billingCycle === 'ANNUAL' ? 'Switch to Annual' :
             currentPlan === 'ENTERPRISE' ? 'Included' : 
             upgradingTier === 'PRO' ? <span className="inline-flex items-center justify-center"><ButtonSpinner className="mr-2 text-current" />Processing...</span> : 'Upgrade to Pro'}
          </button>
          <ul className="mt-8 space-y-3 text-sm text-slate-600">
            <li className="flex gap-x-3"><ShieldCheck className="h-5 w-5 text-indigo-600" /> Everything in Base</li>
            <li className="flex gap-x-3"><ShieldCheck className="h-5 w-5 text-indigo-600" /> WhatsApp Integration</li>
            <li className="flex gap-x-3"><ShieldCheck className="h-5 w-5 text-indigo-600" /> Up to 3 Staff Accounts</li>
            <li className="flex gap-x-3"><ShieldCheck className="h-5 w-5 text-indigo-600" /> Low-Stock Alerts</li>
          </ul>
        </div>

        {/* ENTERPRISE TIER */}
        <div className={`bg-white rounded-2xl p-8 flex flex-col relative ${highlightedTier === 'ENTERPRISE' ? 'ring-2 ring-indigo-600 shadow-xl' : 'ring-1 ring-slate-200'}`}>
          {highlightedTier === 'ENTERPRISE' && (
            <div className="absolute top-0 right-6 transform -translate-y-1/2">
              <span className="bg-indigo-600 text-white text-xs font-bold px-3 py-1 rounded-full uppercase tracking-wide">Most Popular</span>
            </div>
          )}
          <h3 className={`text-xl font-bold ${highlightedTier === 'ENTERPRISE' ? 'text-indigo-600' : 'text-slate-900'}`}>ENTERPRISE</h3>
          <p className="mt-4 text-sm text-slate-500 flex-grow">For large wholesalers or multi-branch networks.</p>
          <div className="mt-6">
            <p className="flex items-baseline gap-x-1">
              <span className="text-4xl font-bold tracking-tight text-slate-900">
                {billingCycle === 'ANNUAL' ? '₹24,990' : '₹2,499'}
              </span>
              <span className="text-sm font-semibold text-slate-500">
                {billingCycle === 'ANNUAL' ? '/year' : '/month'}
              </span>
            </p>
            {billingCycle === 'ANNUAL' && (
              <p className="text-xs text-emerald-600 font-medium mt-1">₹2,082/mo · Save ₹4,998 per year</p>
            )}
          </div>
          <button 
            onClick={() => handleUpgrade('ENTERPRISE', billingCycle === 'ANNUAL' ? 24990 : 2499)}
            disabled={upgradingTier !== null || (currentPlan === 'ENTERPRISE' && billingCycle === 'MONTHLY')}
            className={`mt-8 block w-full rounded-md py-2.5 px-3 text-center text-sm font-semibold transition-all ${
              currentPlan === 'ENTERPRISE' && billingCycle === 'MONTHLY' ? 'bg-slate-100 text-slate-400 cursor-not-allowed' :
              highlightedTier === 'ENTERPRISE' ? 'bg-indigo-600 text-white shadow-sm hover:bg-indigo-500' : 'text-indigo-600 ring-1 ring-inset ring-indigo-200 hover:ring-indigo-300'
            }`}>
            {currentPlan === 'ENTERPRISE' && billingCycle === 'MONTHLY' ? 'Current Plan' : 
             currentPlan === 'ENTERPRISE' && billingCycle === 'ANNUAL' ? 'Switch to Annual' : 
             upgradingTier === 'ENTERPRISE' ? <span className="inline-flex items-center justify-center"><ButtonSpinner className="mr-2 text-current" />Processing...</span> : 'Upgrade to Enterprise'}
          </button>
          <ul className="mt-8 space-y-3 text-sm text-slate-600">
            <li className="flex gap-x-3"><ShieldCheck className="h-5 w-5 text-indigo-600" /> Everything in Pro</li>
            <li className="flex gap-x-3"><ShieldCheck className="h-5 w-5 text-indigo-600" /> Multi-Branch Support</li>
            <li className="flex gap-x-3"><ShieldCheck className="h-5 w-5 text-indigo-600" /> Unlimited Staff</li>
            <li className="flex gap-x-3"><ShieldCheck className="h-5 w-5 text-indigo-600" /> GST Excel Exports</li>
          </ul>
        </div>

      </div>
    </div>
  );
}
