import React from 'react';
import { CreditCard } from 'lucide-react';
import { EmptyState } from '../components/common/States';

export default function Payments() {
  return (
    <div className="p-4 sm:p-6 lg:p-8">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-slate-900">Payments</h1>
        <p className="mt-2 text-sm text-slate-700">Track and manage invoice payments.</p>
      </div>
      
      <EmptyState 
        icon={CreditCard}
        title="No payments yet"
        description="Payments recorded against invoices will appear here."
      />
    </div>
  );
}
