import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getPurchaseById, recordPurchasePayment } from '../api/purchaseApi';
import { LoadingState } from '../components/common/States';
import { ArrowLeft, PackagePlus, FileText, IndianRupee } from 'lucide-react';
import { format } from 'date-fns';
import { useBranch } from '../context/BranchContext';

export default function PurchaseDetails() {
  const { id } = useParams();
  const { branches } = useBranch();
  const [purchase, setPurchase] = useState(null);
  const [loading, setLoading] = useState(true);
  const [showPayModal, setShowPayModal] = useState(false);
  const [payAmount, setPayAmount] = useState('');
  const [payMethod, setPayMethod] = useState('CASH');
  const [payNotes, setPayNotes] = useState('');

  const handlePayment = async (e) => {
    e.preventDefault();
    try {
      await recordPurchasePayment(id, payAmount, payMethod, payNotes);
      setShowPayModal(false);
      fetchPurchase();
    } catch(err) { alert('Failed to record payment: ' + (err.response?.data?.message || err.message)); }
  };


  useEffect(() => {
    fetchPurchase();
  }, [id]);

  const fetchPurchase = async () => {
    try {
      const { data } = await getPurchaseById(id);
      setPurchase(data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleMarkPaid = async () => {
    try {
      setLoading(true);
      await markPurchasePaid(id);
      await fetchPurchase();
    } catch (err) {
      console.error(err);
      alert('Failed to mark as paid');
      setLoading(false);
    }
  };

  if (loading) return <LoadingState />;
  if (!purchase) return <div className="p-8 text-red-500">Purchase not found.</div>;

  const invoiceShop = branches?.find(b => b.id === purchase.shopId);
  const shopName = invoiceShop?.name || 'Your Shop';

  return (
    <div className="p-4 sm:p-6 lg:p-8 max-w-4xl mx-auto">
      <div className="mb-6 flex items-center justify-between">
        
        <div className="flex items-center">
          <Link to="/purchases" className="mr-4 text-slate-400 hover:text-slate-600">
            <ArrowLeft className="w-6 h-6" />
          </Link>
          <h1 className="text-2xl font-bold text-slate-900">
            Purchase {purchase.purchaseNumber}
          </h1>
          <span className={`ml-4 inline-flex px-2 text-xs font-semibold leading-5 rounded-full ${
            purchase.status === 'PAID' ? 'bg-green-100 text-green-800' : 
            purchase.status === 'PENDING' ? 'bg-amber-100 text-amber-800' : 
            'bg-slate-100 text-slate-800'
          }`}>
            {purchase.status}
          </span>
        </div>
        {purchase.status !== 'PAID' && (
          <button onClick={() => setShowPayModal(true)} className="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-green-600 hover:bg-green-700">
          <IndianRupee className="w-4 h-4 mr-2" /> Record Payment
        </button>
        )}

      </div>

      <div className="bg-white shadow-sm ring-1 ring-slate-200 rounded-lg p-8 mb-6">
        <div className="grid grid-cols-2 gap-8 border-b border-slate-200 pb-6 mb-6">
          <div>
            <h3 className="text-sm font-medium text-slate-500 uppercase tracking-wide mb-1">Supplier</h3>
            <Link to={`/suppliers/${purchase.supplierId}`} className="text-lg font-bold text-indigo-600 hover:underline block">
              {purchase.supplierName}
            </Link>
            {purchase.supplierGstin && (
               <div className="text-sm text-slate-500 mt-1">GSTIN: {purchase.supplierGstin}</div>
            )}
          </div>
          <div className="text-right">
            <h3 className="text-sm font-medium text-slate-500 uppercase tracking-wide mb-1">Purchase Details</h3>
            <p className="text-sm text-slate-900"><span className="font-medium">Date:</span> {format(new Date(purchase.createdAt), 'MMM dd, yyyy, h:mm a')}</p>
            <p className="text-sm text-slate-900"><span className="font-medium">Received By:</span> {shopName}</p>
          </div>
        </div>

        <table className="min-w-full mb-8">
          <thead className="border-b border-slate-200">
            <tr>
              <th className="py-3 text-left text-xs font-medium text-slate-500 uppercase">Product</th>
                <th className="py-3 text-center text-xs font-medium text-slate-500 uppercase">HSN</th>
                <th className="py-3 text-right text-xs font-medium text-slate-500 uppercase">Qty</th>
              <th className="py-3 text-right text-xs font-medium text-slate-500 uppercase">Purchase Price</th>
              <th className="py-3 text-right text-xs font-medium text-slate-500 uppercase">Total</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {purchase.items?.map((item) => (
              <tr key={item.id}>
                <td className="py-4 whitespace-nowrap">
                    <div className="flex items-center">
                      <PackagePlus className="w-5 h-5 mr-3 text-indigo-400" />
                      <div>
                        <div className="text-[15px] font-semibold text-slate-900">{item.productName}</div>
                        {item.sku && <div className="text-[13px] text-slate-500 mt-0.5">SKU: {item.sku}</div>}
                      </div>
                    </div>
                  </td>
                <td className="py-4 text-sm text-slate-500 text-center">{item.hsnCode || '-'}</td>
                  <td className="py-4 text-sm text-slate-500 text-right">{item.quantity}</td>
                <td className="py-4 text-sm text-slate-500 text-right">₹{parseFloat(item.purchasePrice || 0).toLocaleString()}</td>
                <td className="py-4 text-sm text-slate-900 text-right">₹{parseFloat(item.totalPrice || 0).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>

        <div className="flex justify-end pt-6 border-t border-slate-200">
          <div className="w-64 space-y-3">
              {(() => {
                const gt = parseFloat(purchase.grandTotal || 0);
                const sub = parseFloat(purchase.subtotal || 0);
                const disc = parseFloat(purchase.discountAmount || 0);
                const tax = parseFloat(purchase.taxAmount || 0);
                const isInclusive = Math.abs((gt + disc) - sub) < 0.1;
                const taxableAmount = (isInclusive && tax > 0) ? (sub - tax) : sub;

                return (
                  <>
                    <div className="flex justify-between text-sm text-slate-600">
                      <span>{isInclusive ? 'Taxable Amount' : 'Subtotal'}</span>
                      <span>₹{taxableAmount.toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2})}</span>
                    </div>
                    {disc > 0 && (
                      <div className="flex justify-between text-sm text-green-600">
                        <span>Discount</span>
                        <span>-₹{disc.toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2})}</span>
                      </div>
                    )}
                    {tax > 0 && (
                      <>
                      <div className="flex justify-between text-sm">
                        <span className="text-slate-500">CGST</span>
                        <span className="text-slate-900 font-medium">₹{(tax / 2).toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2})}</span>
                      </div>
                      <div className="flex justify-between text-sm">
                        <span className="text-slate-500">SGST</span>
                        <span className="text-slate-900 font-medium">₹{(tax / 2).toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2})}</span>
                      </div>
                      <div className="flex justify-between text-sm">
                        <span className="text-slate-500">Total Tax</span>
                        <span className="text-slate-900 font-medium">₹{tax.toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2})}</span>
                      </div>
                      </>
                    )}
                  </>
                );
              })()}
              <div className="pt-3 border-t border-slate-200 flex justify-between items-center">
                <span className="text-base font-medium text-slate-900">Grand Total</span>
                <span className="text-xl font-bold text-slate-900">₹{parseFloat(purchase.grandTotal || 0).toLocaleString()}</span>
              </div>
              <div className="pt-2 flex justify-between items-center">
                <span className="text-base font-medium text-green-600">Amount Paid</span>
                <span className="text-xl font-bold text-green-600">₹{parseFloat(purchase.amountPaid || 0).toLocaleString()}</span>
              </div>
              <div className="pt-2 flex justify-between items-center">
                <span className="text-base font-medium text-red-600">Balance Due</span>
                <span className="text-xl font-bold text-red-600">₹{parseFloat((purchase.grandTotal || 0) - (purchase.amountPaid || 0)).toLocaleString()}</span>
              </div>
            {purchase.paymentMethod && parseFloat(purchase.amountPaid || 0) > 0 && (
              <div className="pt-3 flex justify-between items-center">
                <span className="text-sm font-medium text-slate-600">Payment Method</span>
                <span className="text-sm font-bold text-slate-900 bg-slate-100 px-2 py-1 rounded">{purchase.paymentMethod}</span>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="bg-indigo-50 border border-indigo-100 rounded-lg p-6 flex items-start">
        <FileText className="w-6 h-6 text-indigo-500 mr-4 flex-shrink-0" />
        <div>
          <h3 className="text-sm font-medium text-indigo-900 mb-1">Inventory Updated</h3>
          <p className="text-sm text-indigo-700">
            This purchase has been securely saved. The stock quantities for all associated products have automatically been incremented in your inventory, and the supplier's ledger has been updated with this transaction.
          </p>
        </div>
      </div>
    
      
        {/* Payment History Section */}
        {purchase.payments && purchase.payments.length > 0 && (
          <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-6 mb-8 mt-8">
            <h3 className="text-lg font-medium text-slate-900 mb-4 border-b pb-2">Payment History</h3>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Date</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Amount</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Type</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Notes</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-slate-200">
                  {purchase.payments.map((p, idx) => (
                    <tr key={idx} className="hover:bg-slate-50">
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-500">
                        {format(new Date(p.date), 'MMM dd, yyyy, h:mm a')}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-slate-900">
                        ₹{parseFloat(p.amount || 0).toLocaleString()}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-500">
                        <span className={`px-2 py-1 rounded text-xs font-semibold ${p.type === 'ALLOCATION' ? 'bg-amber-100 text-amber-800' : 'bg-green-100 text-green-800'}`}>
                          {p.type}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-sm text-slate-500 max-w-md truncate" title={p.notes}>
                        {p.notes || '-'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

      {showPayModal && (
        <div className="fixed z-50 inset-0 overflow-y-auto">
          <div className="flex items-center justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:p-0">
            <div className="fixed inset-0 transition-opacity" aria-hidden="true" onClick={() => setShowPayModal(false)}>
              <div className="absolute inset-0 bg-gray-500 opacity-75"></div>
            </div>
            <div className="relative z-10 inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg w-full">
              <form onSubmit={handlePayment}>
                <div className="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                  <h3 className="text-lg leading-6 font-medium text-slate-900 mb-4">Record Payment for {purchase.purchaseNumber}</h3>
                  <div className="space-y-4">
                    <div>
                      <label className="block text-sm font-medium text-slate-700">Amount (₹)</label>
                      <input type="number" required min="1" max={purchase.grandTotal - (purchase.amountPaid || 0)} step="any" value={payAmount} onChange={e => setPayAmount(e.target.value)} className="mt-1 block w-full border border-slate-300 rounded-md py-2 px-3 focus:outline-none focus:ring-green-500 focus:border-green-500 sm:text-sm" />
                      <p className="text-xs text-slate-500 mt-1">Remaining balance: ₹{(purchase.grandTotal - (purchase.amountPaid || 0)).toLocaleString()}</p>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-slate-700">Payment Method</label>
                      <select value={payMethod} onChange={e => setPayMethod(e.target.value)} className="mt-1 block w-full border border-slate-300 rounded-md py-2 px-3 focus:outline-none focus:ring-green-500 focus:border-green-500 sm:text-sm">
                        <option value="CASH">CASH</option>
                        <option value="UPI">UPI</option>
                        <option value="BANK_TRANSFER">BANK TRANSFER</option>
                          <option value="ADVANCE_ADJUSTMENT">Adjust from Advance</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-slate-700">Notes (Optional)</label>
                      <input type="text" value={payNotes} onChange={e => setPayNotes(e.target.value)} className="mt-1 block w-full border border-slate-300 rounded-md py-2 px-3 focus:outline-none focus:ring-green-500 focus:border-green-500 sm:text-sm" />
                    </div>
                  </div>
                </div>
                <div className="bg-slate-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse">
                  <button type="submit" className="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-green-600 text-base font-medium text-white hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 sm:ml-3 sm:w-auto sm:text-sm">Record Payment</button>
                  <button type="button" onClick={() => setShowPayModal(false)} className="mt-3 w-full inline-flex justify-center rounded-md border border-slate-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-slate-700 hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm">Cancel</button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
