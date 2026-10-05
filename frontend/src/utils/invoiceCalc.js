export const calculateInvoiceTotals = (
  items, 
  discountAmount = 0, 
  discountType = 'AMOUNT', 
  shopState = '', 
  customerState = ''
) => {
  let subtotal = 0;
  let taxableAmountTotal = 0;
  let cgstTotal = 0;
  let sgstTotal = 0;
  let igstTotal = 0;

  // Normalise states for comparison
  const sState = (shopState || '').trim().toLowerCase();
  const cState = (customerState || '').trim().toLowerCase();
  const isInterState = sState !== cState && cState !== '';

  const calculatedItems = items.map(item => {
    const price = parseFloat(item.product?.sellingPrice || 0);
    const quantity = parseFloat(item.quantity || 0);
    const grossItemTotal = price * quantity;
    
    subtotal += grossItemTotal;

    const gstRate = parseFloat(item.product?.gstRate || 0);
    const taxType = item.product?.taxType || 'NONE';

    let itemTaxable = grossItemTotal;
    let itemTotalTax = 0;

    if (taxType === 'EXCLUSIVE' && gstRate > 0) {
      itemTaxable = grossItemTotal;
      itemTotalTax = itemTaxable * (gstRate / 100);
    } else if (taxType === 'INCLUSIVE' && gstRate > 0) {
      // Base = Gross / (1 + Rate)
      itemTaxable = grossItemTotal / (1 + (gstRate / 100));
      itemTotalTax = grossItemTotal - itemTaxable;
    }

    let cgst = 0, sgst = 0, igst = 0;
    if (itemTotalTax > 0) {
      if (isInterState) {
        igst = itemTotalTax;
      } else {
        cgst = itemTotalTax / 2;
        sgst = itemTotalTax / 2;
      }
    }

    taxableAmountTotal += itemTaxable;
    cgstTotal += cgst;
    sgstTotal += sgst;
    igstTotal += igst;

    return {
      ...item,
      itemSubtotal: grossItemTotal,
      taxableAmount: itemTaxable,
      cgst,
      sgst,
      igst,
      gstRate
    };
  });

  const discountInput = parseFloat(discountAmount) || 0;
  let finalDiscount = 0;

  // Discount applies to Subtotal (before tax) or after tax? 
  // Standard simple billing applies discount to the final grand total, OR proportionally.
  // We will apply it to the final computed total for simplicity in this MVP.
  let preDiscountTotal = taxableAmountTotal + cgstTotal + sgstTotal + igstTotal;

  if (discountType === 'PERCENTAGE') {
    finalDiscount = (preDiscountTotal * discountInput) / 100;
  } else {
    finalDiscount = discountInput;
  }

  const totalAmount = preDiscountTotal - finalDiscount;

  return {
    items: calculatedItems,
    subtotal,
    taxableAmount: taxableAmountTotal,
    cgstTotal,
    sgstTotal,
    igstTotal,
    isInterState,
    discountAmount: finalDiscount,
    totalAmount: Math.max(totalAmount, 0)
  };
};
