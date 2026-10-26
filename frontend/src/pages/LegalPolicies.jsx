import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Shield, ArrowLeft, Mail, Phone, MapPin } from 'lucide-react';

export default function LegalPolicies() {
  const location = useLocation();
  const path = location.pathname.replace('/', '') || 'terms';

  const [activeTab, setActiveTab] = React.useState(
    path === 'privacy' ? 'privacy' : 
    path === 'refund' ? 'refund' : 
    path === 'contact' ? 'contact' : 'terms'
  );

  React.useEffect(() => {
    if (location.pathname === '/privacy') setActiveTab('privacy');
    else if (location.pathname === '/refund') setActiveTab('refund');
    else if (location.pathname === '/contact') setActiveTab('contact');
    else if (location.pathname === '/terms') setActiveTab('terms');
  }, [location.pathname]);

  return (
    <div className="min-h-screen bg-slate-50">
      {/* Header */}
      <header className="bg-white border-b border-slate-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2 text-indigo-600 font-bold text-xl">
            <ArrowLeft className="w-5 h-5" />
            <span>XenBill</span>
          </Link>
          <div className="flex items-center gap-4">
            <Link to="/login" className="text-sm font-medium text-slate-700 hover:text-slate-900">Log In</Link>
            <Link to="/register" className="text-sm font-semibold text-white bg-indigo-600 px-4 py-2 rounded-lg hover:bg-indigo-500">Sign Up</Link>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        {/* Navigation Tabs */}
        <div className="flex flex-wrap gap-2 mb-8 bg-white p-2 rounded-xl shadow-sm border border-slate-200">
          <button
            onClick={() => setActiveTab('terms')}
            className={`px-4 py-2 text-sm font-semibold rounded-lg transition-all ${
              activeTab === 'terms' ? 'bg-indigo-600 text-white' : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Terms & Conditions
          </button>
          <button
            onClick={() => setActiveTab('privacy')}
            className={`px-4 py-2 text-sm font-semibold rounded-lg transition-all ${
              activeTab === 'privacy' ? 'bg-indigo-600 text-white' : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Privacy Policy
          </button>
          <button
            onClick={() => setActiveTab('refund')}
            className={`px-4 py-2 text-sm font-semibold rounded-lg transition-all ${
              activeTab === 'refund' ? 'bg-indigo-600 text-white' : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Cancellation & Refund
          </button>
          <button
            onClick={() => setActiveTab('contact')}
            className={`px-4 py-2 text-sm font-semibold rounded-lg transition-all ${
              activeTab === 'contact' ? 'bg-indigo-600 text-white' : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Contact Us
          </button>
        </div>

        {/* Content Box */}
        <div className="bg-white p-8 rounded-2xl shadow-sm border border-slate-200 text-slate-700 leading-relaxed space-y-6">
          
          {/* TERMS & CONDITIONS */}
          {activeTab === 'terms' && (
            <div>
              <h1 className="text-3xl font-bold text-slate-900 mb-4">Terms & Conditions</h1>
              <p className="text-sm text-slate-500 mb-6">Last updated: October 24, 2026</p>

              <h2 className="text-xl font-semibold text-slate-900 mt-6 mb-2">1. Acceptance of Terms</h2>
              <p>By accessing and using XenBill (https://xenbill.app), you agree to comply with and be bound by these Terms and Conditions. XenBill is a cloud-based billing, GST invoicing, and inventory management SaaS platform operated for retail and wholesale business owners.</p>

              <h2 className="text-xl font-semibold text-slate-900 mt-6 mb-2">2. Subscription and Service Access</h2>
              <p>XenBill offers subscription tiers (BASE, PRO, and ENTERPRISE) available on Monthly (30-day) and Annual (365-day) prepaid billing cycles. Payment processing is securely facilitated via certified payment partners, including Razorpay.</p>

              <h2 className="text-xl font-semibold text-slate-900 mt-6 mb-2">3. User Accounts and Security</h2>
              <p>You are responsible for maintaining the confidentiality of your account credentials, login passwords, and multi-staff billing access. You agree to notify us immediately of any unauthorized use of your account.</p>

              <h2 className="text-xl font-semibold text-slate-900 mt-6 mb-2">4. Data Ownership & Privacy</h2>
              <p>All data entered into XenBill—including customer lists, product catalogs, invoice numbers, and sales transactions—remains the sole property of the account owner. XenBill does not sell, trade, or distribute your business data.</p>

              <h2 className="text-xl font-semibold text-slate-900 mt-6 mb-2">5. Limitation of Liability</h2>
              <p>XenBill is provided on an "as is" and "as available" basis. While we strive for 99.9% uptime, XenBill shall not be held liable for indirect, incidental, or consequential damages resulting from downtime or payment gateway interruptions.</p>
            </div>
          )}

          {/* PRIVACY POLICY */}
          {activeTab === 'privacy' && (
            <div>
              <h1 className="text-3xl font-bold text-slate-900 mb-4">Privacy Policy</h1>
              <p className="text-sm text-slate-500 mb-6">Last updated: October 24, 2026</p>

              <h2 className="text-xl font-semibold text-slate-900 mt-6 mb-2">1. Information We Collect</h2>
              <p>We collect information necessary to provide billing services, including your name, shop/business name, email address, contact phone number, GSTIN, business address, and transaction logs created during normal invoicing operations.</p>

              <h2 className="text-xl font-semibold text-slate-900 mt-6 mb-2">2. How We Use Your Information</h2>
              <p>We use your information exclusively to:</p>
              <ul className="list-disc pl-6 space-y-1 mt-2">
                <li>Create and manage your SaaS shop workspace</li>
                <li>Process subscription payments and renewals securely through Razorpay</li>
                <li>Dispatch digital receipts via official WhatsApp Business APIs upon request</li>
                <li>Deliver technical support, security updates, and invoice backups</li>
              </ul>

              <h2 className="text-xl font-semibold text-slate-900 mt-6 mb-2">3. Payment Security</h2>
              <p>Payment information (including debit/credit card numbers and UPI identifiers) is processed directly by RBI-authorized payment aggregator Razorpay through 256-bit SSL encryption. XenBill never stores raw payment card numbers or banking passwords on our servers.</p>

              <h2 className="text-xl font-semibold text-slate-900 mt-6 mb-2">4. Data Protection</h2>
              <p>Your database records are encrypted in transit (TLS 1.3) and at rest with role-based access control ensuring isolation between merchant workspaces.</p>
            </div>
          )}

          {/* CANCELLATION & REFUND */}
          {activeTab === 'refund' && (
            <div>
              <h1 className="text-3xl font-bold text-slate-900 mb-4">Cancellation & Refund Policy</h1>
              <p className="text-sm text-slate-500 mb-6">Last updated: October 24, 2026</p>

              <h2 className="text-xl font-semibold text-slate-900 mt-6 mb-2">1. Free Trial Period</h2>
              <p>All new shops registered on XenBill receive a complimentary 7-day free trial with full access to test invoicing and product features without requiring any credit card or upfront payment.</p>

              <h2 className="text-xl font-semibold text-slate-900 mt-6 mb-2">2. Cancellation Policy</h2>
              <p>You may cancel your subscription plan at any time through the <strong>Subscription</strong> page in your dashboard. Upon cancellation, your access will remain fully active until the end of your current prepaid billing cycle (30 days for monthly plans, 365 days for annual plans), after which no further charges will occur.</p>

              <h2 className="text-xl font-semibold text-slate-900 mt-6 mb-2">3. Refund Policy</h2>
              <p>If you experience technical issues or are dissatisfied with your subscription, you may request a full refund within <strong>7 days</strong> of your initial upgrade payment. To request a refund, please contact us at <strong>support@xenbill.com</strong> with your registered shop email and payment reference ID. Approved refunds are credited back to the original payment source within 5–7 business days.</p>
            </div>
          )}

          {/* CONTACT US */}
          {activeTab === 'contact' && (
            <div>
              <h1 className="text-3xl font-bold text-slate-900 mb-4">Contact Us</h1>
              <p className="text-sm text-slate-500 mb-6">We're here to help you manage your billing seamlessly.</p>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 my-6">
                <div className="flex items-start gap-4 p-4 rounded-xl bg-slate-50 border border-slate-100">
                  <Mail className="w-6 h-6 text-indigo-600 mt-1" />
                  <div>
                    <h3 className="font-semibold text-slate-900">Email Support</h3>
                    <p className="text-sm text-slate-600 mt-1">support@xenbill.com</p>
                    <p className="text-xs text-slate-400 mt-1">Response time: within 24 hours</p>
                  </div>
                </div>

                <div className="flex items-start gap-4 p-4 rounded-xl bg-slate-50 border border-slate-100">
                  <Phone className="w-6 h-6 text-indigo-600 mt-1" />
                  <div>
                    <h3 className="font-semibold text-slate-900">Customer Helpline</h3>
                    <p className="text-sm text-slate-600 mt-1">+91 82487 18870</p>
                    <p className="text-xs text-slate-400 mt-1">Mon - Sat: 9:00 AM - 7:00 PM IST</p>
                  </div>
                </div>

                <div className="flex items-start gap-4 p-4 rounded-xl bg-slate-50 border border-slate-100 md:col-span-2">
                  <MapPin className="w-6 h-6 text-indigo-600 mt-1" />
                  <div>
                    <h3 className="font-semibold text-slate-900">Office Address</h3>
                    <p className="text-sm text-slate-600 mt-1">XenBill Technologies, India</p>
                    <p className="text-xs text-slate-400 mt-1">GST & Business Registration available upon request</p>
                  </div>
                </div>
              </div>
            </div>
          )}

        </div>
      </main>
    </div>
  );
}
