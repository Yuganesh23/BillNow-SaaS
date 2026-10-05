import React from 'react';
import { Loader2 } from 'lucide-react';

export const LoadingState = ({ message = 'Loading...' }) => (
  <div className="flex flex-col items-center justify-center p-12">
    <Loader2 className="w-8 h-8 text-indigo-600 animate-spin mb-4" />
    <p className="text-slate-500">{message}</p>
  </div>
);

export const EmptyState = ({ icon: Icon, title, description, action }) => (
  <div className="flex flex-col items-center justify-center p-12 text-center border-2 border-dashed border-slate-200 rounded-lg bg-slate-50">
    {Icon && <Icon className="w-12 h-12 text-slate-400 mb-4" />}
    <h3 className="text-lg font-medium text-slate-900 mb-1">{title}</h3>
    <p className="text-slate-500 mb-6 max-w-sm">{description}</p>
    {action && <div>{action}</div>}
  </div>
);
