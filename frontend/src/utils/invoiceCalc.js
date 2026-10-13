// UI preview only. The backend remains authoritative and returns persisted totals.
const money = (value) => Math.round((Number(value) + Number.EPSILON) * 100) / 100;

export const calculateInvoiceTotals = (
  items, discountAmount = 0, discountType = 'AMOUNT', shopState = '', customerState = '', registrationType = 'REGULAR'
) => {
  const destination = String(customerState || shopState).trim();
  const seller = String(shopState).trim();
  const isInterState = seller !== destination;
  const billOfSupply = registrationType === 'COMPOSITION' || registrationType === 'UNREGISTERED';
  const prepared = items.map((item) => {
    const price = Number(item.product?.sellingPrice || 0);
    const quantity = Number(item.quantity || 0);
    const gstRate = Number(item.product?.gstRate || 0);
    const cessRate = Number(item.product?.cessRate || 0);
    const lineDiscount = money(item.lineDiscount || 0);
    const gross = money(price * quantity - lineDiscount);
    const inclusive = item.product?.taxType === 'INCLUSIVE' && gstRate + cessRate > 0;
    const taxableBeforeInvoiceDiscount = inclusive ? money((gross * 100) / (100 + gstRate + cessRate)) : gross;
    return { item, price, quantity, gstRate, cessRate, taxableBeforeInvoiceDiscount };
  });
  const grossTaxable = money(prepared.reduce((sum, line) => sum + line.taxableBeforeInvoiceDiscount, 0));
  const requestedDiscount = Number(discountAmount) || 0;
  const finalDiscount = money(discountType === 'PERCENTAGE' ? grossTaxable * requestedDiscount / 100 : requestedDiscount);
  let allocated = 0;
  const calculatedItems = prepared.map((line, index) => {
    const discountAllocated = index === prepared.length - 1 ? money(finalDiscount - allocated)
      : money(grossTaxable === 0 ? 0 : finalDiscount * line.taxableBeforeInvoiceDiscount / grossTaxable);
    allocated = money(allocated + discountAllocated);
    const taxableAmount = money(line.taxableBeforeInvoiceDiscount - discountAllocated);
    const gst = billOfSupply ? 0 : money(taxableAmount * line.gstRate / 100);
    const cess = billOfSupply ? 0 : money(taxableAmount * line.cessRate / 100);
    const cgst = isInterState ? 0 : money(gst / 2);
    const sgst = isInterState ? 0 : money(gst - cgst);
    const igst = isInterState ? gst : 0;
    return { ...line.item, itemSubtotal: money(line.price * line.quantity), taxableAmount,
      discountAllocated, cgst, sgst, igst, cess, gstRate: line.gstRate };
  });
  const sum = (key) => money(calculatedItems.reduce((total, item) => total + item[key], 0));
  const taxableAmount = sum('taxableAmount');
  const cgstTotal = sum('cgst');
  const sgstTotal = sum('sgst');
  const igstTotal = sum('igst');
  const cessTotal = sum('cess');
  const unrounded = money(taxableAmount + cgstTotal + sgstTotal + igstTotal + cessTotal);
  const totalAmount = Math.round(unrounded);
  return { items: calculatedItems, subtotal: grossTaxable, taxableAmount, cgstTotal, sgstTotal,
    igstTotal, cessTotal, isInterState, billOfSupply, discountAmount: finalDiscount,
    roundOff: money(totalAmount - unrounded), totalAmount: money(Math.max(totalAmount, 0)) };
};
