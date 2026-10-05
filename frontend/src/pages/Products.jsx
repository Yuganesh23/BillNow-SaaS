import React, { useState, useEffect } from 'react';
import { getProducts, createProduct, updateProduct, deleteProduct } from '../api/productApi';
import { getSuppliers, createSupplier } from '../api/supplierApi';
import { createPurchase } from '../api/purchaseApi';
import { LoadingState } from '../components/common/States';
import { Plus, Search, Edit2, Trash2, Camera, X, RefreshCw } from 'lucide-react';
import { Html5QrcodeScanner } from 'html5-qrcode';
import { Link, useSearchParams } from 'react-router-dom';

export default function Products() {
  const [products, setProducts] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [searchParams] = useSearchParams();
  const filterParam = searchParams.get('filter');
  
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [showAddSupplier, setShowAddSupplier] = useState(false);
  const [newSupplierName, setNewSupplierName] = useState('');
  const [showScanner, setShowScanner] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);
  const [formData, setFormData] = useState({
    name: '', sku: '', category: '', description: '', purchasePrice: '', sellingPrice: '', stockQuantity: '', lowStockThreshold: 10, supplierId: '', totalCost: ''
  });

  const fetchProducts = async () => {
    setLoading(true);
    try {
      const data = await getProducts();
      setProducts(data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const fetchSuppliers = async () => {
    try {
      const { data } = await getSuppliers();
      setSuppliers(data);
    } catch (err) {
      console.error(err);
    }
  };

  useEffect(() => {
    fetchProducts();
    fetchSuppliers();
  }, []);

  const handleOpenModal = (product = null) => {
    if (product) {
      setEditingProduct(product);
      setFormData({
        name: product.name,
        sku: product.sku,
        category: product.category || '',
        description: product.description || '',
        purchasePrice: product.purchasePrice || '',
        sellingPrice: product.sellingPrice || '',
        stockQuantity: product.stockQuantity,
        lowStockThreshold: product.lowStockThreshold || 10,
        supplierId: product.supplierId || '',
        totalCost: '',
        hsnCode: product.hsnCode || '',
        gstRate: product.gstRate || '0',
        taxType: product.taxType || 'NONE'
      });
    } else {
      setEditingProduct(null);
      setFormData({
        name: '', sku: '', category: '', description: '', purchasePrice: '', sellingPrice: '', stockQuantity: '', lowStockThreshold: 10, supplierId: '', totalCost: '', hsnCode: '', gstRate: '0', taxType: 'NONE'
      });
    }
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setShowScanner(false);
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => {
      const next = { ...prev, [name]: value };
      if (name === 'totalCost' || name === 'stockQuantity') {
        const total = parseFloat(next.totalCost);
        const qty = parseFloat(next.stockQuantity);
        if (!isNaN(total) && !isNaN(qty) && qty > 0) {
          next.purchasePrice = (total / qty).toFixed(2);
        }
      }
      return next;
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      // If creating a new product with a supplier, we initialize product with 0 stock
      // and let the auto-generated Purchase bill add the stock to prevent doubling it.
      const requestedStock = parseFloat(formData.stockQuantity) || 0;
      const hasSupplier = formData.supplierId && !editingProduct;
      
      const payload = {
        ...formData,
        purchasePrice: parseFloat(formData.purchasePrice) || 0,
        sellingPrice: parseFloat(formData.sellingPrice) || 0,
        stockQuantity: hasSupplier ? 0 : requestedStock,
        lowStockThreshold: parseInt(formData.lowStockThreshold) || 10,
        supplierId: formData.supplierId ? parseInt(formData.supplierId) : null
      };

      if (editingProduct) {
        await updateProduct(editingProduct.id, payload);
      } else {
        const newProdRes = await createProduct(payload);
        const newProd = Array.isArray(newProdRes) ? newProdRes[0] : (newProdRes.data || newProdRes);
        
        // Auto-create purchase for initial stock
        if (payload.supplierId && requestedStock > 0) {
          try {
            await createPurchase({
              supplierId: payload.supplierId,
              discountAmount: 0,
              taxAmount: 0,
              paymentStatus: 'PENDING',
              paymentMethod: 'CASH',
              amountPaid: 0,
              items: [{
                productId: newProd.id,
                quantity: requestedStock,
                purchasePrice: payload.purchasePrice
              }]
            });
          } catch(err) {
            console.error("Auto-purchase failed:", err); alert("Auto-purchase failed: " + (err.response?.data?.message || err.message));
          }
        }
      }
      fetchProducts();
      handleCloseModal();
    } catch (err) {
      console.error(err);
      alert('Failed to save product');
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this product?')) {
      try {
        await deleteProduct(id);
        fetchProducts();
      } catch (err) {
        console.error(err);
      }
    }
  };

  const handleQuickAddSupplier = async (e) => {
    e.preventDefault();
    try {
      const res = await createSupplier({ businessName: newSupplierName });
      setSuppliers([...suppliers, res.data]);
      setFormData(prev => ({ ...prev, supplierId: res.data.id }));
      setShowAddSupplier(false);
      setNewSupplierName('');
    } catch(err) { alert('Failed to create supplier'); }
  };

  const initScanner = () => {
    setShowScanner(true);
    setTimeout(() => {
      const scanner = new Html5QrcodeScanner("reader", { qrbox: { width: 250, height: 250 }, fps: 10 });
      scanner.render((text) => {
        setFormData(prev => ({ ...prev, sku: text }));
        scanner.clear();
        setShowScanner(false);
      }, (err) => console.warn(err));
    }, 100);
  };

  const filteredProducts = products.filter(p => {
    const matchesSearch = p.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
                          p.sku.toLowerCase().includes(searchTerm.toLowerCase());
    if (filterParam === 'low_stock' || filterParam === 'low-stock') {
      return matchesSearch && p.stockQuantity <= p.lowStockThreshold;
    }
    return matchesSearch;
  });

  return (
    <div className="p-4 sm:p-6 lg:p-8">
      <div className="sm:flex sm:items-center sm:justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Products</h1>
          <p className="mt-2 text-sm text-slate-600">Manage your product catalog and inventory.</p>
        </div>
        <div className="mt-4 sm:mt-0">
          <button onClick={() => handleOpenModal()} className="inline-flex items-center justify-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700">
            <Plus className="w-4 h-4 mr-2" /> Add Product
          </button>
        </div>
      </div>

      <div className="mb-6 relative">
        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
          <Search className="h-5 w-5 text-slate-400" />
        </div>
        <input type="text" placeholder="Search products by name or SKU..." value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} className="block w-full pl-10 pr-3 py-2 border border-slate-300 rounded-md leading-5 bg-white placeholder-slate-500 focus:outline-none focus:placeholder-slate-400 focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
      </div>

      {loading ? (
        <LoadingState />
      ) : (
        <div className="bg-white shadow-sm ring-1 ring-slate-200 rounded-lg overflow-hidden">
          <table className="min-w-full divide-y divide-slate-200">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Product</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">SKU</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Supplier</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Price</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Stock</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-slate-500 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-slate-200">
              {filteredProducts.map((product) => (
                <tr key={product.id} className="hover:bg-slate-50">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-slate-900">{product.name}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-slate-900">{product.sku}</div>
                    {product.category && <div className="text-xs text-slate-500">{product.category}</div>}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-500">
                    {product.supplierName ? <Link to={`/suppliers/${product.supplierId}`} className="text-indigo-600 hover:underline">{product.supplierName}</Link> : '-'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-slate-900">₹{parseFloat(product.sellingPrice).toLocaleString()}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                      product.stockQuantity <= 0 ? 'bg-red-100 text-red-800' :
                      product.stockQuantity <= product.lowStockThreshold ? 'bg-yellow-100 text-yellow-800' :
                      'bg-green-100 text-green-800'
                    }`}>
                      {product.stockQuantity} in stock
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <button onClick={() => handleOpenModal(product)} className="text-indigo-600 hover:text-indigo-900 mr-4">
                      <Edit2 className="w-4 h-4 inline" />
                    </button>
                    <button onClick={() => handleDelete(product.id)} className="text-red-600 hover:text-red-900">
                      <Trash2 className="w-4 h-4 inline" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Product Modal */}
      {isModalOpen && !showAddSupplier && (
        <div className="fixed z-50 inset-0 overflow-y-auto">
          <div className="flex items-center justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:p-0">
            <div className="fixed inset-0 transition-opacity" aria-hidden="true" onClick={handleCloseModal}>
              <div className="absolute inset-0 bg-gray-500 opacity-75"></div>
            </div>
            <div className="relative z-10 inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg w-full">
              <form onSubmit={handleSubmit}>
                <div className="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                  <h3 className="text-lg leading-6 font-medium text-slate-900 mb-4">{editingProduct ? 'Edit Product' : 'Add New Product'}</h3>
                  
                  <div className="space-y-4">
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-1">Name</label>
                      <input type="text" required name="name" value={formData.name} onChange={handleInputChange} className="block w-full border border-slate-300 rounded-md py-2 px-3 text-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500" />
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="block text-sm font-medium text-slate-700 mb-1">SKU / Barcode</label>
                        <div className="flex">
                          <input type="text" required name="sku" value={formData.sku} onChange={handleInputChange} className="block w-full border border-slate-300 rounded-l-md py-2 px-3 text-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500" />
                          <button type="button" onClick={initScanner} className="px-3 border border-l-0 border-slate-300 rounded-r-md bg-slate-50 text-slate-500 hover:bg-slate-100">
                            <Camera className="w-4 h-4" />
                          </button>
                        </div>
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-slate-700 mb-1">Category</label>
                        <input type="text" name="category" value={formData.category} onChange={handleInputChange} className="block w-full border border-slate-300 rounded-md py-2 px-3 text-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500" />
                      </div>
                    </div>

                    {showScanner && (
                      <div className="relative">
                        <button type="button" onClick={() => setShowScanner(false)} className="absolute top-2 right-2 z-10 p-1 bg-white rounded-full shadow-sm text-slate-500 hover:text-slate-700">
                          <X className="w-4 h-4" />
                        </button>
                        <div id="reader" className="w-full bg-slate-50 rounded-md overflow-hidden"></div>
                      </div>
                    )}

                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-1">Supplier</label>
                      <div className="flex space-x-2">
                        <select name="supplierId" value={formData.supplierId} onChange={handleInputChange} className="block w-full border border-slate-300 rounded-md py-2 px-3 text-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500">
                          <option value="">No Supplier</option>
                          {suppliers.map(s => <option key={s.id} value={s.id}>{s.businessName}</option>)}
                        </select>
                        <a href="/suppliers" target="_blank" rel="noreferrer" className="px-3 py-2 border border-slate-300 rounded-md bg-white text-slate-700 hover:bg-slate-50" title="Add supplier"><Plus className="w-4 h-4" /></a>
                        <button type="button" onClick={fetchSuppliers} className="px-3 py-2 border border-slate-300 rounded-md bg-white text-slate-700 hover:bg-slate-50" title="Refresh suppliers"><RefreshCw className="w-4 h-4" /></button>
                      </div>
                    </div>

                    <div className="grid grid-cols-3 gap-4">
                      <div>
                        <label className="block text-sm font-medium text-slate-700 mb-1">Stock Qty</label>
                        <input type="number" required={!editingProduct} disabled={!!editingProduct} name="stockQuantity" min="0" step="any" value={formData.stockQuantity} onChange={handleInputChange} className={`block w-full border rounded-md py-2 px-3 text-sm focus:outline-none ${editingProduct ? "bg-slate-100 border-slate-200 text-slate-500 cursor-not-allowed" : "border-slate-300 focus:ring-indigo-500 focus:border-indigo-500"}`} />
                          {editingProduct && <p className="text-[10px] text-slate-500 mt-1">To add stock, use New Purchase.</p>}
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-slate-700 mb-1">Low Stock Alert</label>
                        <input type="number" required name="lowStockThreshold" min="0" step="1" value={formData.lowStockThreshold} onChange={handleInputChange} className="block w-full border border-slate-300 rounded-md py-2 px-3 text-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500" />
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-slate-700 mb-1">Total Purchase Cost</label>
                        <input type="number" name="totalCost" value={formData.totalCost || ''} onChange={handleInputChange} min="0" step="any" placeholder="Auto-calc" className="block w-full border border-slate-300 rounded-md py-2 px-3 text-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500" />
                      </div>
                    </div>

                    <div className="grid grid-cols-3 gap-4">
                      <div>
                        <label className="block text-sm font-medium text-slate-700 mb-1">HSN Code</label>
                        <input type="text" name="hsnCode" value={formData.hsnCode || ''} onChange={handleInputChange} className="block w-full border border-slate-300 rounded-md py-2 px-3 text-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500" />
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-slate-700 mb-1">GST Rate (%)</label>
                        <select name="gstRate" value={formData.gstRate || '0'} onChange={handleInputChange} className="block w-full border border-slate-300 rounded-md py-2 px-3 text-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500">
                          <option value="0">0%</option>
                          <option value="5">5%</option>
                          <option value="12">12%</option>
                          <option value="18">18%</option>
                          <option value="28">28%</option>
                        </select>
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-slate-700 mb-1">Tax Type</label>
                        <select name="taxType" value={formData.taxType || 'NONE'} onChange={handleInputChange} className="block w-full border border-slate-300 rounded-md py-2 px-3 text-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500">
                          <option value="NONE">None</option>
                          <option value="INCLUSIVE">Inclusive</option>
                          <option value="EXCLUSIVE">Exclusive</option>
                        </select>
                      </div>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="block text-sm font-medium text-slate-700 mb-1">Purchase Price</label>
                        <input type="number" required name="purchasePrice" min="0" step="any" value={formData.purchasePrice} onChange={handleInputChange} className="block w-full border border-slate-300 rounded-md py-2 px-3 text-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500" />
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-slate-700 mb-1">Selling Price</label>
                        <input type="number" required name="sellingPrice" min="0" step="any" value={formData.sellingPrice} onChange={handleInputChange} className="block w-full border border-slate-300 rounded-md py-2 px-3 text-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500" />
                      </div>
                    </div>
                  </div>
                </div>
                <div className="bg-slate-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse">
                  <button type="submit" className="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-indigo-600 text-base font-medium text-white hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 sm:ml-3 sm:w-auto sm:text-sm">
                    Save
                  </button>
                  <button type="button" onClick={handleCloseModal} className="mt-3 w-full inline-flex justify-center rounded-md border border-slate-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-slate-700 hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm">
                    Cancel
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* Quick Add Supplier Modal */}
      {showAddSupplier && (
        <div className="fixed z-[60] inset-0 flex items-center justify-center p-4 bg-gray-500 bg-opacity-75">
          <div className="bg-white rounded-lg p-6 w-full max-w-sm">
            <h3 className="text-lg font-medium mb-4">Quick Add Supplier</h3>
            <form onSubmit={handleQuickAddSupplier}>
              <input autoFocus required type="text" placeholder="Business Name" value={newSupplierName} onChange={e => setNewSupplierName(e.target.value)} className="w-full mb-4 border border-slate-300 rounded-md p-2" />
              <div className="flex justify-end space-x-2">
                <button type="button" onClick={() => setShowAddSupplier(false)} className="px-4 py-2 border rounded-md text-slate-700">Cancel</button>
                <button type="submit" className="px-4 py-2 bg-indigo-600 text-white rounded-md">Save</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
