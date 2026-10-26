import React from 'react';
import iconLogo from '../../assets/brand/xenbill-logo-icon.png';

/**
 * XenBill Branded Full-Page & Section Loading Component
 *
 * @param {string} message - Status message to display (default: 'Loading XenBill...')
 * @param {boolean} fullScreen - Whether to render full screen overlay (default: true)
 * @param {string} size - 'sm' | 'md' | 'lg' (default: 'md')
 * @param {string} className - Additional CSS classes
 */
export default function XenBillLoader({
  message = 'Loading XenBill...',
  fullScreen = true,
  size = 'md',
  className = ''
}) {
  const iconSizes = {
    sm: 'w-10 h-10',
    md: 'w-14 h-14',
    lg: 'w-20 h-20'
  };

  const containerClasses = fullScreen
    ? 'fixed inset-0 z-50 flex flex-col items-center justify-center bg-slate-50/95 backdrop-blur-sm transition-opacity duration-300'
    : 'flex flex-col items-center justify-center p-8 w-full';

  return (
    <div
      role="status"
      aria-live="polite"
      className={`${containerClasses} ${className}`}
    >
      <div className="relative flex items-center justify-center">
        {/* Soft background pulse ring */}
        <div className="absolute inset-0 rounded-2xl bg-emerald-400/20 blur-md animate-ping motion-reduce:hidden" />
        
        {/* XenBill Squircle Brand Mark */}
        <img
          src={iconLogo}
          alt="XenBill"
          className={`${iconSizes[size] || iconSizes.md} relative rounded-2xl shadow-lg shadow-emerald-500/20 animate-pulse motion-reduce:animate-none object-contain select-none`}
        />
      </div>

      {message && (
        <div className="mt-4 flex flex-col items-center text-center">
          <p className="text-sm font-semibold text-slate-800 tracking-wide">
            {message}
          </p>
          <div className="mt-2 flex items-center gap-1.5" aria-hidden="true">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-bounce [animation-delay:-0.3s]" />
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-bounce [animation-delay:-0.15s]" />
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-bounce" />
          </div>
        </div>
      )}
      <span className="sr-only">{message}</span>
    </div>
  );
}

/**
 * Compact Inline Loader for tables, cards, and section containers
 */
export function InlineLoader({ message = 'Loading...', size = 'sm', className = '' }) {
  return (
    <div
      role="status"
      aria-live="polite"
      className={`flex items-center justify-center gap-3 p-6 text-slate-500 ${className}`}
    >
      <img
        src={iconLogo}
        alt=""
        className="w-6 h-6 rounded-lg animate-pulse motion-reduce:animate-none object-contain select-none"
      />
      <span className="text-sm font-medium text-slate-600">{message}</span>
      <span className="sr-only">{message}</span>
    </div>
  );
}

/**
 * Button Spinner component for interactive actions (PDF, Email, WhatsApp, Save)
 */
export function ButtonSpinner({ className = 'w-4 h-4 mr-2' }) {
  return (
    <svg
      className={`animate-spin ${className}`}
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      aria-hidden="true"
    >
      <circle
        className="opacity-25"
        cx="12"
        cy="12"
        r="10"
        stroke="currentColor"
        strokeWidth="4"
      />
      <path
        className="opacity-75"
        fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
      />
    </svg>
  );
}
