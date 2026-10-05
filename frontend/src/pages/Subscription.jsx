import React, { useState } from 'react';
import { ShieldCheck } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useBranch } from '../context/BranchContext';
import axiosClient from '../api/axiosClient';

export default function Subscription() {
  const { user } = useAuth();
  const { branches, activeBranchId, fetchBranches } = useBranch();
  
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

  const handleUpgrade = async (tier, price) => {
    if (!currentBranch) {
        alert("Please select a branch first.");
        return;
    }
    
    try {
        setUpgradingTier(tier);
        
        // ==========================================
        // FUTURE RAZORPAY INTEGRATION GOES HERE
        // ==========================================
        // 1. Call Backend to create a Razorpay Order:
        //    const orderResponse = await axiosClient.post('/payments/create-order', { amount: price });
        // 2. Open Razorpay Checkout Window:
        //    const options = {
        //        key: "YOUR_RAZORPAY_KEY", 
        //        amount: price * 100, 
        //        order_id: orderResponse.data.id,
        //        handler: async function (response) {
        //            // 3. Payment Successful! Now update the database:
        //            await axiosClient.post(`/shops/${currentBranch.id}/upgrade`, { tier });
        //            alert(`Successfully upgraded to ${tier}!`);
        //            window.location.reload();
        //        }
        //    };
        //    const rzp = new window.Razorpay(options);
        //    rzp.open();
        // ==========================================
        
        // For now, simulating the successful payment delay:
        await new Promise(r => setTimeout(r, 1500));
        await axiosClient.post(`/shops/${currentBranch.id}/upgrade`, { tier });
        
        alert(`Successfully upgraded to ${tier}! Your payment was processed.`);
        await fetchBranches();
        window.location.reload();
        
    } catch (e) {
        console.error(e);
        alert("Failed to upgrade plan.");
    } finally {
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
          <p className="mt-6 flex items-baseline gap-x-1">
            <span className="text-4xl font-bold tracking-tight text-slate-900">₹499</span>
            <span className="text-sm font-semibold text-slate-500">/month</span>
          </p>
          <button 
            onClick={() => handleUpgrade('BASE', 499)}
            disabled={upgradingTier !== null || currentPlan === 'BASE'}
            className={`mt-8 block w-full rounded-md py-2.5 px-3 text-center text-sm font-semibold transition-all ${
              currentPlan === 'BASE' ? 'bg-slate-100 text-slate-400 cursor-not-allowed' :
              highlightedTier === 'BASE' ? 'bg-indigo-600 text-white shadow-sm hover:bg-indigo-500' : 'text-indigo-600 ring-1 ring-inset ring-indigo-200 hover:ring-indigo-300'
            }`}>
            {currentPlan === 'BASE' ? 'Current Plan' : upgradingTier === 'BASE' ? 'Processing...' : 'Subscribe to Base'}
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
          <p className="mt-6 flex items-baseline gap-x-1">
            <span className="text-4xl font-bold tracking-tight text-slate-900">₹999</span>
            <span className="text-sm font-semibold text-slate-500">/month</span>
          </p>
          <button 
            onClick={() => handleUpgrade('PRO', 999)}
            disabled={upgradingTier !== null || currentPlan === 'PRO' || currentPlan === 'ENTERPRISE'}
            className={`mt-8 block w-full rounded-md py-2.5 px-3 text-center text-sm font-semibold transition-all ${
              currentPlan === 'PRO' ? 'bg-slate-100 text-slate-400 cursor-not-allowed' :
              currentPlan === 'ENTERPRISE' ? 'bg-slate-100 text-slate-400 cursor-not-allowed' :
              highlightedTier === 'PRO' ? 'bg-indigo-600 text-white shadow-sm hover:bg-indigo-500' : 'text-indigo-600 ring-1 ring-inset ring-indigo-200 hover:ring-indigo-300'
            }`}>
            {currentPlan === 'PRO' ? 'Current Plan' : currentPlan === 'ENTERPRISE' ? 'Included' : upgradingTier === 'PRO' ? 'Processing...' : 'Upgrade to Pro'}
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
          <p className="mt-6 flex items-baseline gap-x-1">
            <span className="text-4xl font-bold tracking-tight text-slate-900">₹2,499</span>
            <span className="text-sm font-semibold text-slate-500">/month</span>
          </p>
          <button 
            onClick={() => handleUpgrade('ENTERPRISE', 2499)}
            disabled={upgradingTier !== null || currentPlan === 'ENTERPRISE'}
            className={`mt-8 block w-full rounded-md py-2.5 px-3 text-center text-sm font-semibold transition-all ${
              currentPlan === 'ENTERPRISE' ? 'bg-slate-100 text-slate-400 cursor-not-allowed' :
              highlightedTier === 'ENTERPRISE' ? 'bg-indigo-600 text-white shadow-sm hover:bg-indigo-500' : 'text-indigo-600 ring-1 ring-inset ring-indigo-200 hover:ring-indigo-300'
            }`}>
            {currentPlan === 'ENTERPRISE' ? 'Current Plan' : upgradingTier === 'ENTERPRISE' ? 'Processing...' : 'Upgrade to Enterprise'}
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
