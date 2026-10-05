import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getCustomers } from '../api/customerApi';
import { getInvoices } from '../api/invoiceApi';
import { LoadingState } from '../components/common/States';
import { ArrowLeft, Clock, ShoppingBag } from 'lucide-react';
import { format } from 'date-fns';

export default function CustomerDetail() {
  const { id } = useParams();
  const [customer, setCustomer] = useState(null);
  const [invoices, setInvoices] = useState([]);
  const [frequentProducts, setFrequentProducts] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [allCustomers, allInvoices] = await Promise.all([
          getCustomers(),
          getInvoices()
        ]);
        
        const cust = allCustomers.find(c => c.id.toString() === id);
        setCustomer(cust);
        
        if (cust) {
          const custInvoices = allInvoices.filter(inv => inv.customerId === cust.id).sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
          setInvoices(custInvoices);
          
          // Calculate most frequent products
          const productCounts = {};
          custInvoices.forEach(inv => {
            if (inv.items) {
              inv.items.forEach(item => {
                const pId = item.productId || item.productName;
                if (!productCounts[pId]) {
                  productCounts[pId] = { name: item.productName, qty: 0, revenue: 0 };
                }
                productCounts[pId].qty += item.quantity;
                productCounts[pId].revenue += item.totalPrice;
              });
            }
          });
          
          const sortedProducts = Object.values(productCounts).sort((a, b) => b.qty - a.qty);
          setFrequentProducts(sortedProducts.slice(0, 5));
        }
      } catch (err) {
        console.error('Error fetching customer details', err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [id]);

  if (loading) return <LoadingState message="Loading customer details..." />;
  if (!customer) return <div className="p-8 text-red-500">Customer not found.</div>;

  return (
    <div className="p-4 sm:p-6 lg:p-8 max-w-7xl mx-auto space-y-6">
      <div className="flex items-center">
        <Link to="/customers" className="mr-4 text-slate-400 hover:text-slate-600">
          <ArrowLeft className="w-6 h-6" />
        </Link>
        <div>
          <h1 className="text-2xl font-bold text-slate-900">{customer.name}</h1>
          <p className="mt-1 text-sm text-slate-500">
            {customer.whatsappNumber} | {customer.email || 'No Email'}
          </p>
          {customer.address && <p className="mt-1 text-sm text-slate-500">Address: {customer.address}</p>}
        </div>
      </div>
      
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-white shadow-sm ring-1 ring-slate-200 rounded-lg overflow-hidden">
          <div className="px-6 py-4 border-b border-slate-200">
            <h2 className="text-lg font-medium text-slate-900">Invoice History</h2>
            <p className="text-sm text-slate-500">Total Invoices: {invoices.length}</p>
          </div>
          {invoices.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Invoice</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Date & Time</th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-slate-500 uppercase tracking-wider">Amount</th>
                    <th className="px-6 py-3 text-center text-xs font-medium text-slate-500 uppercase tracking-wider">Status</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-slate-200">
                  {invoices.map((inv) => (
                    <tr key={inv.id} className="hover:bg-slate-50 cursor-pointer transition-colors">
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-indigo-600">
                        <Link to={`/invoices/${inv.id}`}>{inv.invoiceNumber || `INV-0000${inv.id}`}</Link>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-500">
                        {format(new Date(inv.createdAt), 'MMM dd, yyyy HH:mm a')}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-900 text-right">
                        ₹{inv.totalAmount.toLocaleString()}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-center">
                        <span className={`inline-flex px-2 text-xs font-semibold leading-5 rounded-full ${
                          inv.status === 'PAID' ? 'bg-green-100 text-green-800' : 
                          inv.status === 'PENDING' ? 'bg-amber-100 text-amber-800' : 
                          'bg-slate-100 text-slate-800'
                        }`}>
                          {inv.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="p-8 text-center text-slate-500">
              <Clock className="w-12 h-12 mx-auto mb-3 text-slate-300" />
              No invoices found for this customer.
            </div>
          )}
        </div>
        
        <div className="bg-white shadow-sm ring-1 ring-slate-200 rounded-lg overflow-hidden h-fit">
          <div className="px-6 py-4 border-b border-slate-200">
            <h2 className="text-lg font-medium text-slate-900">Frequently Purchased</h2>
            <p className="text-sm text-slate-500">Top products by quantity</p>
          </div>
          {frequentProducts.length > 0 ? (
            <ul className="divide-y divide-slate-200">
              {frequentProducts.map((prod, idx) => (
                <li key={idx} className="px-6 py-4 flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-slate-900">{prod.name}</p>
                    <p className="text-xs text-slate-500">Qty: {prod.qty}</p>
                  </div>
                  <div className="text-right font-medium text-indigo-600 text-sm">
                    ₹{prod.revenue.toLocaleString()}
                  </div>
                </li>
              ))}
            </ul>
          ) : (
            <div className="p-8 text-center text-slate-500">
              <ShoppingBag className="w-12 h-12 mx-auto mb-3 text-slate-300" />
              No purchase history.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
