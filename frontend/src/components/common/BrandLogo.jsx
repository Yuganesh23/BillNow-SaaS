import React from 'react';
import horizontalLight from '../../assets/brand/xenbill-logo-transparent.png';
import horizontalDark from '../../assets/brand/xenbill-logo-dark.png';
import iconLogo from '../../assets/brand/xenbill-logo-icon.png';

/**
 * XenBill Official Brand Logo Component
 *
 * @param {('horizontal'|'icon')} variant - 'horizontal' (wordmark + icon) or 'icon' (standalone mark)
 * @param {('xs'|'sm'|'md'|'lg'|'xl')} size - Component sizing
 * @param {('dark'|'light')} theme - 'dark' (for light backgrounds) | 'light' (for dark backgrounds)
 * @param {string} className - Optional additional CSS classes
 * @param {string} alt - Accessible image description
 */
export default function BrandLogo({
  variant = 'horizontal',
  size = 'md',
  theme = 'dark',
  className = '',
  alt = 'XenBill'
}) {
  const heights = {
    xs: variant === 'icon' ? 'h-6 w-6' : 'h-6 w-auto',
    sm: variant === 'icon' ? 'h-8 w-8' : 'h-8 w-auto',
    md: variant === 'icon' ? 'h-10 w-10' : 'h-10 w-auto',
    lg: variant === 'icon' ? 'h-12 w-12' : 'h-12 w-auto',
    xl: variant === 'icon' ? 'h-16 w-16' : 'h-16 w-auto'
  };

  const selectedHeight = heights[size] || heights.md;

  if (variant === 'icon') {
    return (
      <img
        src={iconLogo}
        alt={alt}
        className={`object-contain select-none flex-shrink-0 transition-transform duration-200 ${selectedHeight} ${className}`}
        loading="eager"
      />
    );
  }

  const logoSrc = theme === 'light' ? horizontalDark : horizontalLight;

  return (
    <div className={`inline-flex items-center flex-shrink-0 ${className}`}>
      <img
        src={logoSrc}
        alt={alt}
        className={`object-contain select-none ${selectedHeight}`}
        loading="eager"
      />
    </div>
  );
}

export { BrandLogo as XenBillLogo };
