import React from 'react';

/**
 * Standard XenBill Logo & Branding Component
 * Features the signature tilted green badge with 'X' and bold XenBill typography.
 *
 * @param {string} size - 'sm' | 'md' | 'lg' | 'xl' (default: 'md')
 * @param {string} theme - 'dark' (dark text for light bg) | 'light' (white text for dark bg)
 * @param {string} className - Additional CSS classes
 */
export default function BrandLogo({ size = 'md', theme = 'dark', className = '' }) {
  const boxSizes = {
    sm: 'w-8 h-8 rounded-md',
    md: 'w-10 h-10 rounded-lg',
    lg: 'w-12 h-12 rounded-xl',
    xl: 'w-14 h-14 rounded-2xl'
  };

  const letterSizes = {
    sm: 'text-lg',
    md: 'text-xl',
    lg: 'text-2xl',
    xl: 'text-3xl'
  };

  const textSizes = {
    sm: 'text-xl',
    md: 'text-2xl',
    lg: 'text-3xl',
    xl: 'text-4xl'
  };

  const textColor = theme === 'light' ? 'text-white' : 'text-slate-900';

  return (
    <div className={`flex items-center gap-3 ${className}`}>
      <div
        className={`${boxSizes[size] || boxSizes.md} bg-green-500 flex items-center justify-center transform rotate-3 shadow-md shadow-green-500/20 flex-shrink-0`}
      >
        <span
          className={`text-white font-bold ${letterSizes[size] || letterSizes.md} transform -rotate-3 select-none`}
        >
          X
        </span>
      </div>
      <span className={`${textSizes[size] || textSizes.md} font-bold tracking-tight ${textColor} select-none`}>
        XenBill
      </span>
    </div>
  );
}
