import test from 'node:test';
import assert from 'node:assert/strict';
import { calculateInvoiceTotals } from './invoiceCalc.js';

test('preview matches backend fixture for discount, GST and round-off', () => {
  const result = calculateInvoiceTotals([
    { quantity: 2, product: { sellingPrice: 100, gstRate: 18, cessRate: 0, taxType: 'EXCLUSIVE' } },
  ], 10, 'AMOUNT', '33', '33');
  assert.deepEqual({ taxable: result.taxableAmount, cgst: result.cgstTotal, sgst: result.sgstTotal,
    igst: result.igstTotal, roundOff: result.roundOff, total: result.totalAmount },
  { taxable: 190, cgst: 17.1, sgst: 17.1, igst: 0, roundOff: -0.2, total: 224 });
});
