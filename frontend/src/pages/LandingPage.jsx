import React from 'react';
import { Link } from 'react-router-dom';

export default function LandingPage() {
  return (
    <div className="bg-white">
      {/* Header */}
      <header className="absolute inset-x-0 top-0 z-50">
        <nav className="flex items-center justify-between p-6 lg:px-8" aria-label="Global">
          <div className="flex lg:flex-1">
            <a href="#" className="-m-1.5 p-1.5">
              <span className="sr-only">BillNow</span>
              <span className="text-2xl font-bold text-indigo-600">BillNow</span>
            </a>
          </div>
          <div className="flex lg:hidden">
            <button type="button" className="-m-2.5 inline-flex items-center justify-center rounded-md p-2.5 text-gray-700">
              <span className="sr-only">Open main menu</span>
              <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" strokeWidth="1.5" stroke="currentColor" aria-hidden="true">
                <path strokeLinecap="round" strokeLinejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
              </svg>
            </button>
          </div>
          <div className="hidden lg:flex lg:gap-x-12">
            <a href="#features" className="text-sm font-semibold leading-6 text-gray-900">Features</a>
            <a href="#pricing" className="text-sm font-semibold leading-6 text-gray-900">Pricing</a>
          </div>
          <div className="hidden lg:flex lg:flex-1 lg:justify-end gap-x-4">
            <Link to="/login" className="text-sm font-semibold leading-6 text-gray-900 pt-2">Log in</Link>
            <Link to="/register" className="rounded-md bg-indigo-600 px-3.5 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500">Sign up</Link>
          </div>
        </nav>
      </header>

      <main>
        {/* Hero section */}
        <div className="relative isolate px-6 pt-14 lg:px-8">
          <div className="mx-auto max-w-2xl py-32 sm:py-48 lg:py-56">
            <div className="text-center">
              <h1 className="text-4xl font-bold tracking-tight text-gray-900 sm:text-6xl">
                The Fastest Billing Software for Retail & Wholesale
              </h1>
              <p className="mt-6 text-lg leading-8 text-gray-600">
                Generate GST invoices, track inventory, send WhatsApp receipts, and manage multiple branches. Start your 7-day free trial today.
              </p>
              <div className="mt-10 flex items-center justify-center gap-x-6">
                <Link to="/register" className="rounded-md bg-indigo-600 px-3.5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600">
                  Start Free Trial
                </Link>
                <a href="#features" className="text-sm font-semibold leading-6 text-gray-900">
                  Learn more <span aria-hidden="true">→</span>
                </a>
              </div>
            </div>
          </div>
        </div>

        {/* Pricing section */}
        <div id="pricing" className="py-24 sm:py-32">
          <div className="mx-auto max-w-7xl px-6 lg:px-8">
            <div className="mx-auto max-w-4xl text-center">
              <h2 className="text-base font-semibold leading-7 text-indigo-600">Pricing</h2>
              <p className="mt-2 text-4xl font-bold tracking-tight text-gray-900 sm:text-5xl">Pricing plans for teams of all sizes</p>
            </div>
            <p className="mx-auto mt-6 max-w-2xl text-center text-lg leading-8 text-gray-600">
              Choose an affordable plan that's packed with the best features for engaging your audience, creating customer loyalty, and driving sales.
            </p>
            <div className="isolate mx-auto mt-16 grid max-w-md grid-cols-1 gap-y-8 sm:mt-20 lg:mx-0 lg:max-w-none lg:grid-cols-3 lg:gap-x-8 lg:gap-y-0">
              {/* BASE */}
              <div className="rounded-3xl p-8 ring-1 ring-gray-200">
                <h3 className="text-lg font-semibold leading-8 text-gray-900">BASE</h3>
                <p className="mt-4 text-sm leading-6 text-gray-600">Perfect for single shops needing fast billing.</p>
                <p className="mt-6 flex items-baseline gap-x-1">
                  <span className="text-4xl font-bold tracking-tight text-gray-900">₹499</span>
                  <span className="text-sm font-semibold leading-6 text-gray-600">/month</span>
                </p>
                <a href="/register" className="mt-6 block rounded-md py-2 px-3 text-center text-sm font-semibold leading-6 text-indigo-600 ring-1 ring-inset ring-indigo-200 hover:ring-indigo-300">Buy plan</a>
                <ul role="list" className="mt-8 space-y-3 text-sm leading-6 text-gray-600">
                  <li className="flex gap-x-3">✓ Unlimited Invoices</li>
                  <li className="flex gap-x-3">✓ Standard PDF generation</li>
                  <li className="flex gap-x-3">✓ 1 User (Admin)</li>
                </ul>
              </div>

              {/* PRO */}
              <div className="rounded-3xl p-8 ring-2 ring-indigo-600">
                <h3 className="text-lg font-semibold leading-8 text-indigo-600">PRO</h3>
                <p className="mt-4 text-sm leading-6 text-gray-600">Our most popular plan for growing retail shops.</p>
                <p className="mt-6 flex items-baseline gap-x-1">
                  <span className="text-4xl font-bold tracking-tight text-gray-900">₹999</span>
                  <span className="text-sm font-semibold leading-6 text-gray-600">/month</span>
                </p>
                <a href="/register" className="mt-6 block rounded-md bg-indigo-600 py-2 px-3 text-center text-sm font-semibold leading-6 text-white shadow-sm hover:bg-indigo-500">Buy plan</a>
                <ul role="list" className="mt-8 space-y-3 text-sm leading-6 text-gray-600">
                  <li className="flex gap-x-3">✓ Everything in Base</li>
                  <li className="flex gap-x-3">✓ WhatsApp Integration</li>
                  <li className="flex gap-x-3">✓ Up to 3 Staff Accounts</li>
                  <li className="flex gap-x-3">✓ Low-Stock Alerts</li>
                </ul>
              </div>

              {/* ENTERPRISE */}
              <div className="rounded-3xl p-8 ring-1 ring-gray-200">
                <h3 className="text-lg font-semibold leading-8 text-gray-900">ENTERPRISE</h3>
                <p className="mt-4 text-sm leading-6 text-gray-600">For large wholesalers or multi-branch networks.</p>
                <p className="mt-6 flex items-baseline gap-x-1">
                  <span className="text-4xl font-bold tracking-tight text-gray-900">₹2499</span>
                  <span className="text-sm font-semibold leading-6 text-gray-600">/month</span>
                </p>
                <a href="/register" className="mt-6 block rounded-md py-2 px-3 text-center text-sm font-semibold leading-6 text-indigo-600 ring-1 ring-inset ring-indigo-200 hover:ring-indigo-300">Buy plan</a>
                <ul role="list" className="mt-8 space-y-3 text-sm leading-6 text-gray-600">
                  <li className="flex gap-x-3">✓ Everything in Pro</li>
                  <li className="flex gap-x-3">✓ Multi-Branch Support</li>
                  <li className="flex gap-x-3">✓ Unlimited Staff</li>
                  <li className="flex gap-x-3">✓ GST Excel Exports</li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
