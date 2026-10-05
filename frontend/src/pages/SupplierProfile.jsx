import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getSupplierById, getSupplierLedger, recordSupplierPayment, deleteSupplier, updateSupplier } from '../api/supplierApi';
import { getPurchases } from '../api/purchaseApi';
import { LoadingState } from '../components/common/States';
import { ArrowLeft, Phone, Mail, MapPin, FileText, CreditCard, Trash2, IndianRupee, Edit2 } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { format } from 'date-fns';

export default function SupplierProfile() {
  const navigate = useNavigate();
  const [showPayModal, setShowPayModal] = useState(false);
  const [payAmount, setPayAmount] = useState("");
  const [payMethod, setPayMethod] = useState("CASH");
  const [payNotes, setPayNotes] = useState("");

    const [showEditModal, setShowEditModal] = useState(false);
  const [editFormData, setEditFormData] = useState({});

  const handleEditOpen = () => {
    setEditFormData({
      businessName: supplier?.businessName || '',
      contactPerson: supplier?.contactPerson || '',
      phone: supplier?.phone || '',
      whatsapp: supplier?.whatsapp || '',
      email: supplier?.email || '',
      gstin: supplier?.gstin || '',
      address: supplier?.address || '',
      notes: supplier?.notes || ''
    });
    setShowEditModal(true);
  };

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    try {
      await updateSupplier(id, editFormData);
      setShowEditModal(false);
      fetchData();
    } catch(err) { alert('Failed to update supplier: ' + (err.response?.data?.message || err.message)); }
  };

  const handlePayment = async (e) => {
    e.preventDefault();
    try {
      await recordSupplierPayment(id, payAmount, payMethod, payNotes);
      setShowPayModal(false);
      fetchData();
    } catch (err) {
      alert("Failed to record payment");
    }
  };

  const handleDelete = async () => {
    if(window.confirm("Are you sure you want to delete this supplier? This action cannot be undone if they have no transaction history.")) {
      try {
        await deleteSupplier(id);
        navigate("/suppliers");
      } catch (err) {
        alert(err.response?.data?.message || "Cannot delete supplier with existing transactions.");
      }
    }
  };

  const { id } = useParams();
  const [supplier, setSupplier] = useState(null);
  const [ledger, setLedger] = useState([]);
  const [purchases, setPurchases] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchData();
  }, [id]);

  const fetchData = async () => {
    try {
      const [suppRes, ledgerRes, purRes] = await Promise.all([
        getSupplierById(id),
        getSupplierLedger(id),
        getPurchases()
      ]);
      setSupplier(suppRes.data);
      setLedger(ledgerRes.data);
      setPurchases(purRes.data.filter(p => p.supplierId.toString() === id));
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <LoadingState />;
  if (!supplier) return <div className="p-8 text-red-500">Supplier not found.</div>;

  return (
    <div className="p-4 sm:p-6 lg:p-8 max-w-7xl mx-auto">
            <div className="mb-6 flex items-center justify-between">
        <div className="flex items-center">
          <Link to="/suppliers" className="mr-4 text-slate-400 hover:text-slate-600">
            <ArrowLeft className="w-6 h-6" />
          </Link>
          <h1 className="text-2xl font-bold text-slate-900">{supplier.businessName}</h1>
        </div>
        <div className="flex space-x-3">
            <button onClick={handleEditOpen} className="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700">
              <Edit2 className="w-4 h-4 mr-2" />
              Edit
            </button>
          <button onClick={() => setShowPayModal(true)} className="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-green-600 hover:bg-green-700">
            <IndianRupee className="w-4 h-4 mr-2" />
            Record Payment
          </button>
          <button onClick={handleDelete} className="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-red-700 bg-red-100 hover:bg-red-200">
            <Trash2 className="w-4 h-4 mr-2" />
            Delete
          </button>
        </div>
      </div>


      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
        {/* Contact Info */}
        <div className="bg-white shadow-sm ring-1 ring-slate-200 rounded-lg p-6">
          <h2 className="text-lg font-medium text-slate-900 mb-4">Contact Info</h2>
          <div className="space-y-3 text-sm text-slate-600">
            {supplier.contactPerson && <p className="font-medium text-slate-900">{supplier.contactPerson}</p>}
            {supplier.phone && <p className="flex items-center"><Phone className="w-4 h-4 mr-2" /> {supplier.phone}</p>}
            {supplier.email && <p className="flex items-center"><Mail className="w-4 h-4 mr-2" /> {supplier.email}</p>}
            {supplier.gstin && <p className="flex items-center"><FileText className="w-4 h-4 mr-2" /> GSTIN: {supplier.gstin}</p>}
            {supplier.address && <p className="flex items-start"><MapPin className="w-4 h-4 mr-2 mt-0.5" /> {supplier.address}</p>}
          </div>
        </div>

        {/* Financial Summary */}
        <div className="bg-white shadow-sm ring-1 ring-slate-200 rounded-lg p-6 lg:col-span-2 flex flex-col justify-center">
          <h2 className="text-lg font-medium text-slate-900 mb-4 uppercase tracking-wide text-xs">Purchase Summary</h2>
          <div className="grid grid-cols-3 gap-4 text-center">
            <div className="p-4 bg-slate-50 rounded-lg">
              <p className="text-xs text-slate-500 mb-1">Total Purchases</p>
              <p className="text-xl font-bold text-slate-900">₹{parseFloat(supplier.totalPurchases || 0).toLocaleString()}</p>
            </div>
            <div className="p-4 bg-green-50 rounded-lg">
              <p className="text-xs text-green-600 mb-1">Amount Paid</p>
              <p className="text-xl font-bold text-green-700">₹{parseFloat(supplier.amountPaid || 0).toLocaleString()}</p>
            </div>
            <div className={`p-4 rounded-lg ${parseFloat(supplier.outstandingBalance) > 0 ? 'bg-red-50' : 'bg-slate-50'}`}>
              <p className={`text-xs mb-1 ${parseFloat(supplier.outstandingBalance) > 0 ? 'text-red-600' : 'text-slate-500'}`}>Outstanding</p>
              <p className={`text-xl font-bold ${parseFloat(supplier.outstandingBalance) > 0 ? 'text-red-700' : 'text-slate-900'}`}>
                ₹{parseFloat(supplier.outstandingBalance || 0).toLocaleString()}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Tabs / Ledger */}
      <div className="bg-white shadow-sm ring-1 ring-slate-200 rounded-lg overflow-hidden mb-8">
        <div className="px-6 py-4 border-b border-slate-200 bg-slate-50">
          <h2 className="text-lg font-medium text-slate-900">Supplier Ledger</h2>
        </div>
        <table className="min-w-full divide-y divide-slate-200">
          <thead className="bg-white">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase">Date</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase">Description</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase">Products</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-slate-500 uppercase">Debit (Purchase)</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-slate-500 uppercase">Credit (Payment)</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-slate-500 uppercase">Balance</th>
              </tr>
            </thead>
          <tbody className="bg-white divide-y divide-slate-200">
            {ledger.map((l) => (
              <tr key={l.id} className="hover:bg-slate-50">
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-500">
                    {format(new Date(l.transactionDate), 'MMM dd, yyyy, h:mm a')}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-900">
                    {l.notes} {l.reference && <span className="text-xs text-indigo-600 ml-2">({l.reference})</span>}
                  </td>
                  <td className="px-6 py-4 text-sm text-slate-500 max-w-xs truncate" title={l.productNames}>
                    {l.productNames || '-'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-red-600">
                    {l.debitAmount ? `₹${parseFloat(l.debitAmount).toLocaleString()}` : '-'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-green-600">
                    {l.creditAmount ? `₹${parseFloat(l.creditAmount).toLocaleString()}` : '-'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-right font-medium text-slate-900">
                    ₹{parseFloat(l.runningBalance).toLocaleString()}
                  </td>
                </tr>
            ))}
            {ledger.length === 0 && (
              <tr>
                <td colSpan="6" className="px-6 py-4 text-center text-sm text-slate-500">No transactions yet.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

    
      {/* Payment Modal */}
      {showPayModal && (
        <div className="fixed z-50 inset-0 overflow-y-auto">
          <div className="flex items-center justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:p-0">
            <div className="fixed inset-0 transition-opacity" aria-hidden="true" onClick={() => setShowPayModal(false)}>
              <div className="absolute inset-0 bg-gray-500 opacity-75"></div>
            </div>
            <div className="relative z-10 inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg w-full">
              <form onSubmit={handlePayment}>
                <div className="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                  <h3 className="text-lg leading-6 font-medium text-slate-900 mb-4">Record Payment to {supplier.businessName}</h3>
                  <div className="space-y-4">
                    <div>
                      <label className="block text-sm font-medium text-slate-700">Amount (₹)</label>
                      <input type="number" required min="1" step="any" value={payAmount} onChange={e => setPayAmount(e.target.value)} className="mt-1 block w-full border border-slate-300 rounded-md py-2 px-3 focus:outline-none focus:ring-green-500 focus:border-green-500 sm:text-sm" />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-slate-700">Payment Method</label>
                      <select value={payMethod} onChange={e => setPayMethod(e.target.value)} className="mt-1 block w-full border border-slate-300 rounded-md py-2 px-3 focus:outline-none focus:ring-green-500 focus:border-green-500 sm:text-sm">
                        <option value="CASH">CASH</option>
                        <option value="UPI">UPI</option>
                        <option value="BANK_TRANSFER">BANK TRANSFER</option>
                          <option value="ADVANCE">ADVANCE</option>
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


      {/* Edit Supplier Modal */}
      {showEditModal && (
        <div className="fixed z-50 inset-0 overflow-y-auto">
          <div className="flex items-center justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:p-0">
            <div className="fixed inset-0 transition-opacity" aria-hidden="true" onClick={() => setShowEditModal(false)}>
              <div className="absolute inset-0 bg-gray-500 opacity-75"></div>
            </div>
            <div className="relative z-10 inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-2xl w-full">
              <form onSubmit={handleEditSubmit}>
                <div className="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                  <h3 className="text-lg leading-6 font-medium text-slate-900 mb-4">Edit Supplier</h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="md:col-span-2">
                      <label className="block text-sm font-medium text-slate-700">Business Name *</label>
                      <input type="text" required value={editFormData.businessName} onChange={e => setEditFormData({...editFormData, businessName: e.target.value})} className="mt-1 block w-full border border-slate-300 rounded-md py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-slate-700">Contact Person</label>
                      <input type="text" value={editFormData.contactPerson} onChange={e => setEditFormData({...editFormData, contactPerson: e.target.value})} className="mt-1 block w-full border border-slate-300 rounded-md py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-slate-700">Phone</label>
                      <input type="text" value={editFormData.phone} onChange={e => setEditFormData({...editFormData, phone: e.target.value})} className="mt-1 block w-full border border-slate-300 rounded-md py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-slate-700">WhatsApp</label>
                      <input type="text" value={editFormData.whatsapp} onChange={e => setEditFormData({...editFormData, whatsapp: e.target.value})} className="mt-1 block w-full border border-slate-300 rounded-md py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-slate-700">Email</label>
                      <input type="email" value={editFormData.email} onChange={e => setEditFormData({...editFormData, email: e.target.value})} className="mt-1 block w-full border border-slate-300 rounded-md py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
                    </div>
                    <div className="md:col-span-2">
                      <label className="block text-sm font-medium text-slate-700">GSTIN</label>
                      <input type="text" value={editFormData.gstin} onChange={e => setEditFormData({...editFormData, gstin: e.target.value})} className="mt-1 block w-full border border-slate-300 rounded-md py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
                    </div>
                    <div className="md:col-span-2">
                      <label className="block text-sm font-medium text-slate-700">Address</label>
                      <textarea value={editFormData.address} onChange={e => setEditFormData({...editFormData, address: e.target.value})} rows="2" className="mt-1 block w-full border border-slate-300 rounded-md py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"></textarea>
                    </div>
                    <div className="md:col-span-2">
                      <label className="block text-sm font-medium text-slate-700">Notes</label>
                      <textarea value={editFormData.notes} onChange={e => setEditFormData({...editFormData, notes: e.target.value})} rows="2" className="mt-1 block w-full border border-slate-300 rounded-md py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"></textarea>
                    </div>
                  </div>
                </div>
                <div className="bg-slate-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse">
                  <button type="submit" className="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-indigo-600 text-base font-medium text-white hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 sm:ml-3 sm:w-auto sm:text-sm">Save Changes</button>
                  <button type="button" onClick={() => setShowEditModal(false)} className="mt-3 w-full inline-flex justify-center rounded-md border border-slate-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-slate-700 hover:bg-slate-50 focus:outline-none sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm">Cancel</button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
