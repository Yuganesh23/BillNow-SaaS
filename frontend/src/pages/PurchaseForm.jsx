import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getSuppliers, createSupplier } from '../api/supplierApi';
import { getProducts, createProduct } from '../api/productApi';
import { createPurchase } from '../api/purchaseApi';
import { LoadingState } from '../components/common/States';
import { ArrowLeft, Plus, Trash2, Save, RefreshCw } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function PurchaseForm() {
  const navigate = useNavigate();
  const [suppliers, setSuppliers] = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  
  const [supplierId, setSupplierId] = useState('');
  const [items, setItems] = useState([{ productId: '', quantity: 1, purchasePrice: 0 }]);
  const [discountAmount, setDiscountAmount] = useState(0);
  const [taxAmount, setTaxAmount] = useState(0);
  const [paymentStatus, setPaymentStatus] = useState('PAID');
  const [paymentMethod, setPaymentMethod] = useState('UPI');

  
  // Quick Add States
  const [showAddSupplier, setShowAddSupplier] = useState(false);
  const [newSupplierName, setNewSupplierName] = useState('');
  const [newSupplierGstin, setNewSupplierGstin] = useState('');
  const [newSupplierState, setNewSupplierState] = useState('');
  const [showAddProduct, setShowAddProduct] = useState(false);
  const [newProductName, setNewProductName] = useState('');
  const [newProductPrice, setNewProductPrice] = useState('');
  const [newProductGst, setNewProductGst] = useState('0');
  const [newProductTaxType, setNewProductTaxType] = useState('NONE');
  const [newProductHsn, setNewProductHsn] = useState('');

  const loadSuppliers = async () => {
    try {
      const res = await getSuppliers();
      setSuppliers(res.data);
    } catch(err){}
  };
  
  const loadProducts = async () => {
    try {
      const res = await getProducts();
      setProducts(Array.isArray(res) ? res : (res.data || []));
    } catch(err){}
  };

  
  const handleQuickAddSupplier = async (e) => {
    e.preventDefault();
    try {
      const res = await createSupplier({ businessName: newSupplierName });
      setSuppliers([...suppliers, res.data]);
      setSupplierId(res.data.id);
      setShowAddSupplier(false);
      setNewSupplierName('');
    } catch(err) { alert('Failed to create supplier'); }
  };
  
  const handleQuickAddProduct = async (e) => {
    e.preventDefault();
    try {
      const res = await createProduct({ 
          name: newProductName, 
          sellingPrice: newProductPrice || 0,
          purchasePrice: 0,
          stockQuantity: 0,
          lowStockThreshold: 10,
          sku: 'SKU-' + Date.now(),
          hsnCode: newProductHsn || '',
          gstRate: parseFloat(newProductGst || 0),
          taxType: newProductTaxType || 'NONE'
        });
      setProducts([...products, res.data]);
      setShowAddProduct(false);
      setNewProductName('');
      setNewProductPrice('');
    } catch(err) { alert('Failed to create product'); }
  };

  useEffect(() => {
    Promise.all([getSuppliers(), getProducts()])
      .then(([suppRes, prodRes]) => {
        setSuppliers(suppRes.data);
        setProducts(Array.isArray(prodRes) ? prodRes : (prodRes.data || []));
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  const handleItemChange = (index, field, value) => {
    const newItems = [...items];
    newItems[index][field] = value;
    if (field === 'productId') {
      const prod = products.find(p => p.id.toString() === value);
      if (prod) {
         newItems[index].purchasePrice = prod.price || 0;
         newItems[index].gstRate = prod.gstRate || 0;
         newItems[index].taxType = prod.taxType || 'NONE';
      }
    }
    setItems(newItems);
    
    // Auto calculate tax
    setTimeout(() => {
      let autoBase = 0;
      let autoTax = 0;
      newItems.forEach(item => {
         const p = products.find(prod => prod.id.toString() === item.productId?.toString());
         const qty = parseFloat(item.quantity || 0);
         const price = parseFloat(item.purchasePrice || 0);
         const enteredTotal = price * qty;
         
         if (p && p.taxType !== 'NONE' && parseFloat(p.gstRate || 0) > 0) {
            const rate = parseFloat(p.gstRate);
            if (p.taxType === 'EXCLUSIVE') {
               const tax = (enteredTotal * rate) / 100;
               autoBase += enteredTotal;
               autoTax += tax;
            } else if (p.taxType === 'INCLUSIVE') {
               const base = enteredTotal / (1 + (rate / 100));
               const tax = enteredTotal - base;
               autoBase += base;
               autoTax += tax;
            }
         } else {
            autoBase += enteredTotal;
         }
      });
      setTaxAmount(autoTax.toFixed(2));
    }, 0);
  };

  const subtotal = items.reduce((sum, item) => {
      const p = products.find(prod => prod.id.toString() === item.productId?.toString());
      const qty = parseFloat(item.quantity || 0);
      const price = parseFloat(item.purchasePrice || 0);
      const enteredTotal = price * qty;
      
      if (p && p.taxType === 'INCLUSIVE' && parseFloat(p.gstRate || 0) > 0) {
          const rate = parseFloat(p.gstRate);
          return sum + (enteredTotal / (1 + (rate / 100)));
      }
      return sum + enteredTotal;
  }, 0);
  
  const grandTotal = subtotal + parseFloat(taxAmount || 0) - parseFloat(discountAmount || 0);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!supplierId) return alert('Select a supplier');
    if (items.some(i => !i.productId || i.quantity <= 0)) return alert('Fill all product details correctly');
    
    try {
      const payload = {
        supplierId: parseInt(supplierId),
        items: items.map(i => ({
          productId: parseInt(i.productId),
          quantity: parseFloat(i.quantity),
          purchasePrice: parseFloat(i.purchasePrice)
        })),
        discountAmount: parseFloat(discountAmount || 0),
        taxAmount: parseFloat(taxAmount || 0),
        paymentStatus,
        amountPaid: paymentStatus === 'PAID' ? grandTotal : 0,
          paymentMethod: paymentStatus === 'PENDING' ? null : paymentMethod
      };
      
      const res = await createPurchase(payload);
      navigate(`/purchases/${res.data.id}`);
    } catch (err) {
      console.error(err);
      alert('Failed to save purchase');
    }
  };

  if (loading) return <LoadingState />;

  return (
    <div className="p-4 sm:p-6 lg:p-8 max-w-4xl mx-auto">
      <div className="mb-6 flex items-center">
        <button onClick={() => navigate(-1)} className="mr-4 text-slate-400 hover:text-slate-600">
          <ArrowLeft className="w-6 h-6" />
        </button>
        <h1 className="text-2xl font-bold text-slate-900">Create Purchase</h1>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="bg-white shadow-sm ring-1 ring-slate-200 rounded-lg p-6">
          <label className="block text-sm font-medium text-slate-700 mb-1">Supplier</label>
          <div className="flex space-x-4">
            <select required value={supplierId} onChange={e => setSupplierId(e.target.value)} className="block w-full border border-slate-300 rounded-md py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm">
              <option value="">Select Supplier ▼</option>
              {suppliers.map(s => <option key={s.id} value={s.id}>{s.businessName}</option>)}
            </select>
            <a href="/suppliers" target="_blank" rel="noreferrer" className="inline-flex items-center px-4 py-2 border border-slate-300 shadow-sm text-sm font-medium rounded-md text-slate-700 bg-white hover:bg-slate-50">
              <Plus className="w-4 h-4 mr-1" /> Add
            </a>
            <button type="button" onClick={loadSuppliers} className="inline-flex items-center px-3 py-2 border border-slate-300 shadow-sm text-sm font-medium rounded-md text-slate-700 bg-white hover:bg-slate-50" title="Refresh Suppliers">
              <RefreshCw className="w-4 h-4" />
            </button>
          </div>
        </div>

        <div className="bg-white shadow-sm ring-1 ring-slate-200 rounded-lg p-6">
          <h2 className="text-lg font-medium text-slate-900 mb-4">Purchase Items</h2>
          <div className="space-y-4">
            {items.map((item, index) => (
              <div key={index} className="flex flex-col sm:flex-row items-end space-y-4 sm:space-y-0 sm:space-x-4">
                <div className="flex-1 w-full">
                  <label className="block text-xs font-medium text-slate-700 mb-1">Product</label>
                  <div className="flex space-x-2">
                    <select required value={item.productId} onChange={e => handleItemChange(index, 'productId', e.target.value)} className="block w-full border border-slate-300 rounded-md py-2 px-3 text-sm focus:ring-indigo-500 focus:border-indigo-500">
                      <option value="">Select Product ▼</option>
                      {products.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
                    </select>
                    <a href="/products" target="_blank" rel="noreferrer" className="px-3 py-2 border border-slate-300 rounded-md bg-white text-slate-700 hover:bg-slate-50" title="Add Product in new tab"><Plus className="w-4 h-4" /></a>
                  <button type="button" onClick={loadProducts} className="px-3 py-2 border border-slate-300 rounded-md bg-white text-slate-700 hover:bg-slate-50" title="Refresh Products"><RefreshCw className="w-4 h-4" /></button>
                  </div>
                </div>
                <div className="w-full sm:w-32">
                  <label className="block text-xs font-medium text-slate-700 mb-1">Unit Price</label>
                  <input type="number" required min="0" step="0.01" value={item.purchasePrice} onChange={e => handleItemChange(index, 'purchasePrice', e.target.value)} className="block w-full border border-slate-300 rounded-md py-2 px-3 text-sm focus:ring-indigo-500 focus:border-indigo-500" />
                </div>
                <div className="w-full sm:w-24">
                  <label className="block text-xs font-medium text-slate-700 mb-1">Qty</label>
                  <input type="number" required min="1" step="any" value={item.quantity} onChange={e => handleItemChange(index, 'quantity', e.target.value)} className="block w-full border border-slate-300 rounded-md py-2 px-3 text-sm focus:ring-indigo-500 focus:border-indigo-500" />
                </div>
                <div className="w-full sm:w-32">
                  <label className="block text-xs font-medium text-slate-700 mb-1">Total</label>
                  <div className="py-2 px-3 bg-slate-50 text-slate-900 border border-transparent rounded-md text-sm">
                    ₹{(parseFloat(item.purchasePrice || 0) * parseFloat(item.quantity || 0)).toLocaleString()}
                  </div>
                </div>
                <button type="button" onClick={() => {
                  const newItems = items.filter((_, i) => i !== index);
                  setItems(newItems);
                  let autoTax = 0;
                  newItems.forEach(item => {
                     const p = products.find(prod => prod.id.toString() === item.productId?.toString());
                     if (p && p.taxType !== 'NONE') {
                        const qty = parseFloat(item.quantity || 0);
                        const price = parseFloat(item.purchasePrice || 0);
                        const rate = parseFloat(p.gstRate || 0);
                        if (p.taxType === 'EXCLUSIVE') {
                           autoTax += (price * qty * rate) / 100;
                        } else if (p.taxType === 'INCLUSIVE') {
                           autoTax += (price * qty * rate) / (100 + rate);
                        }
                     }
                  });
                  setTaxAmount(autoTax.toFixed(2));
                }} className="p-2 text-red-500 hover:text-red-700 bg-red-50 rounded-md">
                  <Trash2 className="w-5 h-5" />
                </button>
              </div>
            ))}
          </div>
          <button type="button" onClick={() => setItems([...items, { productId: '', quantity: 1, purchasePrice: 0 }])} className="mt-4 inline-flex items-center text-sm font-medium text-indigo-600 hover:text-indigo-500">
            <Plus className="w-4 h-4 mr-1" /> Add Another Item
          </button>
        </div>

        <div className="bg-white shadow-sm ring-1 ring-slate-200 rounded-lg p-6">
          <div className="flex flex-col sm:flex-row justify-end space-y-4 sm:space-y-0 sm:space-x-8">
            <div className="w-full sm:w-64 space-y-4">
              <div className="flex justify-between items-center">
                <span className="text-sm font-medium text-slate-600">Subtotal</span>
                <span className="text-sm text-slate-900">₹{subtotal.toLocaleString()}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm font-medium text-slate-600">Discount</span>
                <input type="number" min="0" step="0.01" value={discountAmount} onChange={e => setDiscountAmount(e.target.value)} className="w-24 border border-slate-300 rounded-md py-1 px-2 text-right text-sm focus:ring-indigo-500 focus:border-indigo-500" />
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm font-medium text-slate-600">GST / Tax</span>
                <input type="number" min="0" step="0.01" value={taxAmount} onChange={e => setTaxAmount(e.target.value)} className="w-24 border border-slate-300 rounded-md py-1 px-2 text-right text-sm focus:ring-indigo-500 focus:border-indigo-500" />
              </div>
              <div className="border-t border-slate-200 pt-4 flex justify-between items-center">
                <span className="text-base font-bold text-slate-900">Grand Total</span>
                <span className="text-lg font-bold text-indigo-600">₹{grandTotal.toLocaleString()}</span>
              </div>
            </div>
          </div>

          <div className="border-t border-slate-200 mt-6 pt-6 flex flex-col sm:flex-row justify-between items-center gap-4">
            <div className="flex gap-4 w-full sm:w-auto">
              <div className="w-full sm:w-40">
                <label className="block text-xs font-medium text-slate-700 mb-1">Payment Status</label>
                <select value={paymentStatus} onChange={e => setPaymentStatus(e.target.value)} className="block w-full border border-slate-300 rounded-md py-2 px-3 text-sm focus:ring-indigo-500 focus:border-indigo-500">
                  <option value="PAID">PAID</option>
                  <option value="PENDING">PENDING</option>
                  <option value="PARTIAL">PARTIAL</option>
                </select>
              </div>
              {paymentStatus !== 'PENDING' && (
                <div className="w-full sm:w-40">
                  <label className="block text-xs font-medium text-slate-700 mb-1">Method</label>
                  <select value={paymentMethod} onChange={e => setPaymentMethod(e.target.value)} className="block w-full border border-slate-300 rounded-md py-2 px-3 text-sm focus:ring-indigo-500 focus:border-indigo-500">
                  <option value="UPI">UPI</option>
                  <option value="CASH">CASH</option>
                  <option value="BANK_TRANSFER">BANK TRANSFER</option>
                </select>
              </div>
              )}
            </div>
            <button type="submit" className="w-full sm:w-auto inline-flex justify-center items-center px-6 py-3 border border-transparent shadow-sm text-base font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700">
              <Save className="w-5 h-5 mr-2" />
              Save Purchase
            </button>
          </div>
        </div>
      </form>

      {showAddSupplier && (
        <div className="fixed z-50 inset-0 flex items-center justify-center p-4 bg-gray-500 bg-opacity-75">
          <div className="bg-white rounded-lg p-6 w-full max-w-sm">
            <h3 className="text-lg font-medium mb-4">Quick Add Supplier</h3>
            <form onSubmit={handleQuickAddSupplier}>
              <input autoFocus required type="text" placeholder="Business Name" value={newSupplierName} onChange={e => setNewSupplierName(e.target.value)} className="w-full mb-4 border border-slate-300 rounded-md p-2" />
              <input type="text" placeholder="GSTIN (Optional)" value={newSupplierGstin} onChange={e => setNewSupplierGstin(e.target.value)} className="w-full mb-4 border border-slate-300 rounded-md p-2" />
              <input type="text" placeholder="State (e.g. Tamil Nadu)" value={newSupplierState} onChange={e => setNewSupplierState(e.target.value)} className="w-full mb-4 border border-slate-300 rounded-md p-2" />
              <div className="flex justify-end space-x-2">
                <button type="button" onClick={() => setShowAddSupplier(false)} className="px-4 py-2 border rounded-md text-slate-700">Cancel</button>
                <button type="submit" className="px-4 py-2 bg-indigo-600 text-white rounded-md">Save</button>
              </div>
            </form>
          </div>
        </div>
      )}
      {showAddProduct && (
        <div className="fixed z-50 inset-0 flex items-center justify-center p-4 bg-gray-500 bg-opacity-75">
          <div className="bg-white rounded-lg p-6 w-full max-w-sm">
            <h3 className="text-lg font-medium mb-4">Quick Add Product</h3>
            <form onSubmit={handleQuickAddProduct}>
              <input autoFocus required type="text" placeholder="Product Name" value={newProductName} onChange={e => setNewProductName(e.target.value)} className="w-full mb-4 border border-slate-300 rounded-md p-2" />
              <input type="number" placeholder="Selling Price (₹)" value={newProductPrice} onChange={e => setNewProductPrice(e.target.value)} className="w-full mb-4 border border-slate-300 rounded-md p-2" />
              <input type="text" placeholder="HSN Code (Optional)" value={newProductHsn} onChange={e => setNewProductHsn(e.target.value)} className="w-full mb-4 border border-slate-300 rounded-md p-2" />
              <div className="flex space-x-2 mb-4">
                <select value={newProductGst} onChange={e => setNewProductGst(e.target.value)} className="w-1/2 border border-slate-300 rounded-md p-2">
                  <option value="0">0% GST</option>
                  <option value="5">5% GST</option>
                  <option value="12">12% GST</option>
                  <option value="18">18% GST</option>
                  <option value="28">28% GST</option>
                </select>
                <select value={newProductTaxType} onChange={e => setNewProductTaxType(e.target.value)} className="w-1/2 border border-slate-300 rounded-md p-2">
                  <option value="NONE">No Tax</option>
                  <option value="INCLUSIVE">Inclusive</option>
                  <option value="EXCLUSIVE">Exclusive</option>
                </select>
              </div>
              <div className="flex justify-end space-x-2">
                <button type="button" onClick={() => setShowAddProduct(false)} className="px-4 py-2 border rounded-md text-slate-700">Cancel</button>
                <button type="submit" className="px-4 py-2 bg-indigo-600 text-white rounded-md">Save</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
