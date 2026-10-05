import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCustomers, createCustomer } from '../api/customerApi';
import { getProducts } from '../api/productApi';
import { createInvoice } from '../api/invoiceApi';
import { createPayment } from '../api/paymentApi';
import { calculateInvoiceTotals } from '../utils/invoiceCalc';
import { useBranch } from '../context/BranchContext';
import { Plus, Trash2, ArrowLeft, Save, QrCode } from 'lucide-react';
import QRScannerModal from '../components/QRScannerModal';
import { Link } from 'react-router-dom';

export default function CreateInvoice() {
  const navigate = useNavigate();
  const { branches, activeBranchId } = useBranch();
  const [customers, setCustomers] = useState([]);
  const [products, setProducts] = useState([]);
  
  const [selectedCustomerId, setSelectedCustomerId] = useState('');
  const [items, setItems] = useState([{ id: Date.now(), productId: '', quantity: 1, product: null }]);
  const [discountAmount, setDiscountAmount] = useState(0);
  const [discountType, setDiscountType] = useState('AMOUNT');
  const [paymentMethod, setPaymentMethod] = useState('CASH');
  
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  const [showCustomerModal, setShowCustomerModal] = useState(false);
  const [newCustomer, setNewCustomer] = useState({ name: '', whatsappNumber: '', email: '', address: '', gstin: '', state: '' });
  const [isAddingCustomer, setIsAddingCustomer] = useState(false);
  const [showScanner, setShowScanner] = useState(false);

  const handleAddCustomer = async () => {
    setIsAddingCustomer(true);
    try {
      const added = await createCustomer(newCustomer);
      setCustomers([...customers, added]);
      setSelectedCustomerId(added.id);
      setShowCustomerModal(false);
      setNewCustomer({ name: '', whatsappNumber: '', email: '' });
    } catch(err) {
      alert('Failed to add customer');
    }
    setIsAddingCustomer(false);
  };

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [custData, prodData] = await Promise.all([getCustomers(), getProducts()]);
        setCustomers(custData);
        setProducts(prodData);
      } catch (err) {
        console.error('Failed to load initial data');
      }
    };
    fetchData();
  }, []);

    const handleScanBarcode = (decodedText) => {
    const product = products.find(p => p.sku === decodedText);
    if (product) {
      const emptyItem = items.find(i => !i.productId);
      if (emptyItem) {
        handleItemChange(emptyItem.id, 'productId', product.id.toString());
      } else {
        const newId = Date.now();
        setItems([...items, { id: newId, productId: product.id.toString(), quantity: 1, product }]);
      }
    } else {
      alert("Product not found for SKU/Barcode: " + decodedText);
    }
    setShowScanner(false);
  };

  

  const handleAddItem = () => {
    setItems([...items, { id: Date.now(), productId: '', quantity: 1, product: null }]);
  };

  const handleRemoveItem = (id) => {
    if (items.length > 1) {
      setItems(items.filter(item => item.id !== id));
    }
  };

  const handleItemChange = (id, field, value) => {
    setItems(items.map(item => {
      if (item.id === id) {
        const updatedItem = { ...item, [field]: value };
        if (field === 'productId') {
          updatedItem.product = products.find(p => p.id === parseInt(value)) || null;
        }
        return updatedItem;
      }
      return item;
    }));
  };

  
    const activeShop = branches?.find(b => b.id.toString() === activeBranchId);
    const shopState = activeShop?.state || '';
    const activeCustomer = customers?.find(c => c.id.toString() === selectedCustomerId);
    const customerState = activeCustomer?.state || '';

    const totals = calculateInvoiceTotals(items, discountAmount, discountType, shopState, customerState);

  
  const isValid = selectedCustomerId && 
                  items.every(item => item.productId && item.quantity > 0) &&
                  items.length > 0;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!isValid) return;
    
    setIsSubmitting(true);
    try {
      const payload = {
          customerId: parseInt(selectedCustomerId),
          items: totals.items.map(item => ({
            productId: parseInt(item.productId),
            quantity: parseFloat(item.quantity),
            taxableAmount: item.taxableAmount || 0,
            cgst: item.cgst || 0,
            sgst: item.sgst || 0,
            igst: item.igst || 0,
            gstRate: item.gstRate || 0
          })),
          discountAmount: totals.discountAmount || 0,
          taxableAmount: totals.taxableAmount || 0,
          cgstTotal: totals.cgstTotal || 0,
          sgstTotal: totals.sgstTotal || 0,
          igstTotal: totals.igstTotal || 0,
          isInterState: totals.isInterState || false
        };
      
      const response = await createInvoice(payload);
      if (paymentMethod) {
          await createPayment({
            invoiceId: response.id,
            paymentMethod: paymentMethod,
            amount: response.totalAmount || totals.totalAmount
          });
        }
        navigate(`/invoices/${response.id}`);
    } catch (err) {
      alert('Failed to create invoice');
      setIsSubmitting(false);
    }
  };

  return (
    <div className="p-4 sm:p-6 lg:p-8 max-w-5xl mx-auto">
      <div className="mb-6 flex items-center justify-between">
        <div className="flex items-center">
          <Link to="/invoices" className="mr-4 text-slate-400 hover:text-slate-600">
            <ArrowLeft className="w-6 h-6" />
          </Link>
          <h1 className="text-2xl font-bold text-slate-900">Create Invoice</h1>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-8">
        {/* Customer Section */}
        <div className="bg-white shadow-sm ring-1 ring-slate-200 rounded-lg p-6">
          <h2 className="text-lg font-medium text-slate-900 mb-4">Customer Details</h2>
          <div className="max-w-md flex items-end gap-2">
            <div className="flex-1">
              <label className="block text-sm font-medium text-slate-700">Select Customer <span className="text-red-500">*</span></label>
              <select
                required
                value={selectedCustomerId}
                onChange={(e) => setSelectedCustomerId(e.target.value)}
                className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-slate-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md"
              >
                <option value="">Select a customer</option>
                {customers.map(c => (
                  <option key={c.id} value={c.id}>{c.name} ({c.whatsappNumber})</option>
                ))}
              </select>
            </div>
            <button
              type="button"
              onClick={() => setShowCustomerModal(true)}
              className="mb-1 inline-flex items-center p-2 border border-slate-300 rounded-md shadow-sm text-sm font-medium text-slate-700 bg-white hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
              title="Add New Customer"
            >
              <Plus className="h-5 w-5" />
            </button>
          </div>
        </div>

        {/* Items Section */}
        <div className="bg-white shadow-sm ring-1 ring-slate-200 rounded-lg p-6">
          <h2 className="text-lg font-medium text-slate-900 mb-4">Invoice Items</h2>
          
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 mb-4">
              <thead className="bg-slate-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-slate-500 uppercase">Product <span className="text-red-500">*</span></th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-slate-500 uppercase w-32">Price</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-slate-500 uppercase w-32">Quantity <span className="text-red-500">*</span></th>
                  <th className="px-4 py-3 text-right text-xs font-medium text-slate-500 uppercase w-32">Total</th>
                  <th className="px-4 py-3 w-16"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {items.map((item, index) => (
                  <tr key={item.id}>
                    <td className="px-4 py-3">
                      <select
                        required
                        value={item.productId}
                        onChange={(e) => handleItemChange(item.id, 'productId', e.target.value)}
                        className="block w-full pl-3 pr-10 py-2 text-base border-slate-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md"
                      >
                        <option value="">Select product</option>
                        {products.map(p => (
                          <option key={p.id} value={p.id}>{p.name} {p.category ? `(${p.category})` : ''}</option>
                        ))}
                      </select>
                    </td>
                    <td className="px-4 py-3">
                      <div className="text-sm text-slate-900">
                        ₹{item.product?.sellingPrice || 0}
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <input
                        type="number" min="0.01" step="any"
                        required
                        value={item.quantity}
                        onChange={(e) => handleItemChange(item.id, 'quantity', e.target.value)}
                        className="block w-full border-slate-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                      />
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div className="text-sm font-medium text-slate-900">
                        ₹{totals.items[index].itemSubtotal.toLocaleString()}
                      </div>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <button
                        type="button"
                        onClick={() => handleRemoveItem(item.id)}
                        disabled={items.length === 1}
                        className="text-red-500 hover:text-red-700 disabled:opacity-50"
                      >
                        <Trash2 className="w-5 h-5" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          
          <div className="mt-4 flex gap-3">
            <button
              type="button"
              onClick={handleAddItem}
              className="inline-flex items-center px-4 py-2 border border-slate-300 shadow-sm text-sm font-medium rounded-md text-slate-700 bg-white hover:bg-slate-50"
            >
              <Plus className="w-4 h-4 mr-2" />
              Add Item
            </button>
            <button
              type="button"
              onClick={() => setShowScanner(true)}
              className="inline-flex items-center px-4 py-2 border border-slate-300 shadow-sm text-sm font-medium rounded-md text-indigo-700 bg-indigo-50 hover:bg-indigo-100"
            >
              <QrCode className="w-4 h-4 mr-2" />
              Scan Barcode
            </button>
          </div>
        </div>

        {/* Totals Section */}
        <div className="bg-slate-50 shadow-sm ring-1 ring-slate-200 rounded-lg p-6 flex flex-col md:flex-row justify-between">
          <div className="w-full md:w-1/2 mb-6 md:mb-0 pr-0 md:pr-8">
             <div className="flex flex-col mb-4">
                <label className="block text-sm font-medium text-slate-700 mb-1">Discount</label>
                <div className="flex items-center space-x-2 max-w-xs">
                  <select 
                    value={discountType} 
                    onChange={(e) => setDiscountType(e.target.value)}
                    className="bg-slate-50 border border-slate-300 text-slate-900 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  >
                    <option value="AMOUNT">Amount (₹)</option>
                    <option value="PERCENTAGE">Percentage (%)</option>
                  </select>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    placeholder={discountType === 'PERCENTAGE' ? '%' : '₹'}
                    value={discountAmount}
                    onChange={(e) => setDiscountAmount(e.target.value)}
                    className="flex-1 block w-full border border-slate-300 py-2 px-3 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  />
                </div>
               </div>

               <div className="mt-4">
                  <label className="block text-sm font-medium text-slate-700">Mark as Paid (Payment Method)</label>
                  <select
                    value={paymentMethod}
                    onChange={(e) => setPaymentMethod(e.target.value)}
                    className="mt-1 block w-full max-w-xs bg-white text-slate-900 border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  >
                                        <option value="CASH">Cash</option>
                    <option value="UPI">UPI</option>
                    <option value="CARD">Card</option>
                  </select>
               </div>
          </div>
          
          <div className="w-full md:w-1/3 space-y-3">
            <div className="flex justify-between text-sm text-slate-600">
              <span>Subtotal</span>
              <span>₹{totals.subtotal.toLocaleString()}</span>
            </div>
            <div className="flex justify-between text-sm text-slate-600">
              <span>Discount</span>
              <span className="text-red-500">-₹{totals.discountAmount.toLocaleString()}</span>
            </div>
            <div className="pt-3 border-t border-slate-200 flex justify-between">
              <span className="text-base font-medium text-slate-900">Grand Total</span>
              <span className="text-xl font-bold text-indigo-600">₹{totals.totalAmount.toLocaleString()}</span>
            </div>
          </div>
        </div>

        <div className="flex justify-end pt-4">
          <button
            type="submit"
            disabled={!isValid || isSubmitting}
            className="inline-flex items-center px-6 py-3 border border-transparent shadow-sm text-base font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isSubmitting ? 'Saving...' : 'Save & Create Invoice'}
            {!isSubmitting && <Save className="ml-2 w-5 h-5" />}
          </button>
        </div>
      </form>

      {showScanner && (
        <QRScannerModal 
          onScan={handleScanBarcode}
          onClose={() => setShowScanner(false)}
        />
      )}
      
      {showCustomerModal && (
        <div className="fixed z-50 inset-0 overflow-y-auto" aria-labelledby="modal-title" role="dialog" aria-modal="true">
          <div className="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
            <div className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity" aria-hidden="true" onClick={() => setShowCustomerModal(false)}></div>
            <span className="hidden sm:inline-block sm:align-middle sm:h-screen" aria-hidden="true">&#8203;</span>
            <div className="relative z-10 inline-block align-bottom bg-white rounded-lg px-4 pt-5 pb-4 text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full sm:p-6">
              <div>
                <h3 className="text-lg leading-6 font-medium text-gray-900" id="modal-title">Add New Customer</h3>
                <div className="mt-4 space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-700">Name</label>
                    <input type="text" className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" value={newCustomer.name} onChange={e => setNewCustomer({...newCustomer, name: e.target.value})} />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700">WhatsApp Number</label>
                    <input type="text" className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" value={newCustomer.whatsappNumber} onChange={e => setNewCustomer({...newCustomer, whatsappNumber: e.target.value})} />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700">GSTIN (Optional)</label>
                    <input type="text" className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" value={newCustomer.gstin} onChange={e => setNewCustomer({...newCustomer, gstin: e.target.value})} />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700">State</label>
                    <input type="text" placeholder="e.g. Tamil Nadu" className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" value={newCustomer.state} onChange={e => setNewCustomer({...newCustomer, state: e.target.value})} />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700">Email (Optional)</label>
                    <input type="email" className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" value={newCustomer.email} onChange={e => setNewCustomer({...newCustomer, email: e.target.value})} />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700">Address (Optional)</label>
                    <textarea rows="3" className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" value={newCustomer.address} onChange={e => setNewCustomer({...newCustomer, address: e.target.value})} />
                  </div>
                </div>
              </div>
              <div className="mt-5 sm:mt-6 sm:grid sm:grid-cols-2 sm:gap-3 sm:grid-flow-row-dense">
                <button type="button" onClick={handleAddCustomer} disabled={!newCustomer.name || !newCustomer.whatsappNumber || isAddingCustomer} className="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-indigo-600 text-base font-medium text-white hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 sm:col-start-2 sm:text-sm disabled:opacity-50">
                  {isAddingCustomer ? 'Adding...' : 'Add Customer'}
                </button>
                <button type="button" onClick={() => setShowCustomerModal(false)} className="mt-3 w-full inline-flex justify-center rounded-md border border-slate-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-slate-700 hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 sm:mt-0 sm:col-start-1 sm:text-sm">
                  Cancel
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}












