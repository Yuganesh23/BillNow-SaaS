import React from 'react';
import iconLogo from '../../assets/brand/xenbill-logo-icon.png';

export const LoadingState = ({ message = 'Loading...', fullScreen = false }) => {
  if (fullScreen) {
    return (
      <div
        role="status"
        aria-live="polite"
        className="fixed inset-0 z-50 flex flex-col items-center justify-center bg-slate-50/95 backdrop-blur-sm"
      >
        <div className="relative flex items-center justify-center">
          <div className="absolute inset-0 rounded-2xl bg-emerald-400/20 blur-md animate-ping motion-reduce:hidden" />
          <img
            src={iconLogo}
            alt="XenBill"
            className="w-14 h-14 rounded-2xl shadow-lg shadow-emerald-500/20 animate-pulse motion-reduce:animate-none object-contain select-none"
          />
        </div>
        <p className="mt-4 text-sm font-semibold text-slate-800">{message}</p>
        <div className="mt-2 flex items-center gap-1.5" aria-hidden="true">
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-bounce [animation-delay:-0.3s]" />
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-bounce [animation-delay:-0.15s]" />
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-bounce" />
        </div>
        <span className="sr-only">{message}</span>
      </div>
    );
  }

  return (
    <div role="status" aria-live="polite" className="flex flex-col items-center justify-center p-12 w-full min-h-[50vh]">
      <div className="relative flex items-center justify-center">
        <div className="absolute inset-0 rounded-xl bg-emerald-400/20 blur-sm animate-ping motion-reduce:hidden" />
        <img
          src={iconLogo}
          alt="XenBill"
          className="w-12 h-12 rounded-xl shadow-md shadow-emerald-500/20 animate-pulse motion-reduce:animate-none object-contain select-none"
        />
      </div>
      <p className="mt-4 text-sm font-medium text-slate-600">{message}</p>
      <div className="mt-2 flex items-center gap-1.5" aria-hidden="true">
        <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-bounce [animation-delay:-0.3s]" />
        <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-bounce [animation-delay:-0.15s]" />
        <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-bounce" />
      </div>
      <span className="sr-only">{message}</span>
    </div>
  );
};

export const EmptyState = ({ icon: Icon, title, description, action }) => (
  <div className="flex flex-col items-center justify-center p-12 text-center border-2 border-dashed border-slate-200 rounded-lg bg-slate-50">
    {Icon && <Icon className="w-12 h-12 text-slate-400 mb-4" />}
    <h3 className="text-lg font-medium text-slate-900 mb-1">{title}</h3>
    <p className="text-slate-500 mb-6 max-w-sm">{description}</p>
    {action && <div>{action}</div>}
  </div>
);
