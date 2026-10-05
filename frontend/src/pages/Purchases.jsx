import React, { useState, useEffect } from 'react';
import { getPurchases } from '../api/purchaseApi';
import { LoadingState } from '../components/common/States';
import { Plus, Search, Eye } from 'lucide-react';
import { Link } from 'react-router-dom';
import { format } from 'date-fns';

export default function Purchases() {
  const [purchases, setPurchases] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    fetchPurchases();
  }, []);

  const fetchPurchases = async () => {
    try {
      const { data } = await getPurchases();
      setPurchases(data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const filteredPurchases = purchases.filter(p => 
    p.purchaseNumber.toLowerCase().includes(searchTerm.toLowerCase()) || 
    p.supplierName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (loading) return <LoadingState />;

  return (
    <div className="p-4 sm:p-6 lg:p-8">
      <div className="sm:flex sm:items-center sm:justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Purchases</h1>
          <p className="mt-2 text-sm text-slate-700">Manage incoming stock from suppliers.</p>
        </div>
        <div className="mt-4 sm:mt-0">
          <Link to="/purchases/new" className="inline-flex items-center justify-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700">
            <Plus className="w-5 h-5 mr-2 -ml-1" />
            New Purchase
          </Link>
        </div>
      </div>

      <div className="mb-6 max-w-md relative">
        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
          <Search className="h-5 w-5 text-slate-400" />
        </div>
        <input
          type="text"
          className="block w-full pl-10 pr-3 py-2 border border-slate-300 rounded-md leading-5 bg-white text-slate-900 placeholder-slate-500 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
          placeholder="Search purchase or supplier..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
      </div>

      <div className="bg-white shadow-sm ring-1 ring-slate-200 rounded-lg overflow-hidden">
        <table className="min-w-full divide-y divide-slate-200">
          <thead className="bg-slate-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Purchase No.</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Supplier</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Date</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Products</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-slate-500 uppercase tracking-wider">Total</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-slate-500 uppercase tracking-wider">Paid</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-slate-500 uppercase tracking-wider">Balance</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Status</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-slate-500 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
          <tbody className="bg-white divide-y divide-slate-200">
            {filteredPurchases.map((purchase) => (
              <tr key={purchase.id} className="hover:bg-slate-50">
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-slate-900">
                    {purchase.purchaseNumber}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-500">
                    <Link to={`/suppliers/${purchase.supplierId}`} className="text-indigo-600 hover:underline">{purchase.supplierName}</Link>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-500">
                    {format(new Date(purchase.createdAt), 'MMM dd, yyyy, h:mm a')}
                  </td>
                  <td className="px-6 py-4 text-sm text-slate-500 max-w-xs truncate" title={purchase.items?.map(i => i.productName).join(', ')}>
                    {purchase.items?.map(i => i.productName).join(', ') || '-'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm text-slate-900 font-medium">
                    ₹{parseFloat(purchase.grandTotal || 0).toLocaleString()}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm text-green-600 font-medium">
                    ₹{parseFloat(purchase.amountPaid || 0).toLocaleString()}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm text-red-600 font-medium">
                    ₹{parseFloat((purchase.grandTotal || 0) - (purchase.amountPaid || 0)).toLocaleString()}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`inline-flex px-2 text-xs font-semibold leading-5 rounded-full ${
                      purchase.status === 'PAID' ? 'bg-green-100 text-green-800' : 
                      purchase.status === 'PENDING' ? 'bg-amber-100 text-amber-800' : 
                      'bg-slate-100 text-slate-800'
                    }`}>
                      {purchase.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <Link to={`/purchases/${purchase.id}`} className="text-indigo-600 hover:text-indigo-900">
                      <Eye className="w-4 h-4 ml-auto" />
                    </Link>
                  </td>
                </tr>
            ))}
            {filteredPurchases.length === 0 && (
              <tr>
                <td colSpan="6" className="px-6 py-4 text-center text-sm text-slate-500">No purchases found.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
