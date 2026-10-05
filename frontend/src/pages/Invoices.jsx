import React, { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { getInvoices, cancelInvoice } from '../api/invoiceApi';
import { LoadingState, EmptyState } from '../components/common/States';
import { FileText, Plus, Search, Eye, XCircle } from 'lucide-react';
import { format } from 'date-fns';

export default function Invoices() {
  const [invoices, setInvoices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [searchParams] = useSearchParams();
  const billerIdFilter = searchParams.get('billerId');

  const fetchInvoices = async () => {
    setLoading(true);
    try {
      const data = await getInvoices();
      setInvoices(data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInvoices();
  }, []);

  const handleCancel = async (id) => {
    if (window.confirm('Are you sure you want to cancel this invoice?')) {
      try {
        await cancelInvoice(id);
        fetchInvoices();
      } catch (err) {
        alert('Failed to cancel invoice');
      }
    }
  };

  const filteredInvoices = invoices.filter(i => {
    const matchesSearch = i.invoiceNumber?.toLowerCase().includes(searchTerm.toLowerCase()) || 
                          i.customerName?.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || i.status === statusFilter;
    const matchesBiller = !billerIdFilter || i.billerId === parseInt(billerIdFilter);
    return matchesSearch && matchesStatus && matchesBiller;
  });

  if (loading && invoices.length === 0) return <LoadingState message="Loading invoices..." />;

  return (
    <div className="p-4 sm:p-6 lg:p-8">
      <div className="sm:flex sm:items-center sm:justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">{billerIdFilter ? "Invoices (Filtered by Biller)" : "Invoices"}</h1>
          <p className="mt-2 text-sm text-slate-700">Manage your invoices and track payments.</p>
        </div>
        <div className="mt-4 sm:mt-0">
          <Link
            to="/invoices/create"
            className="inline-flex items-center justify-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700"
          >
            <Plus className="w-5 h-5 mr-2 -ml-1" />
            Create Invoice
          </Link>
        </div>
      </div>

      <div className="mb-6 flex flex-col sm:flex-row gap-4">
        <div className="relative flex-1 max-w-md">
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <Search className="h-5 w-5 text-slate-400" />
          </div>
          <input
            type="text"
            className="block w-full pl-10 pr-3 py-2 border border-slate-300 rounded-md leading-5 bg-white placeholder-slate-500 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
            placeholder="Search by invoice number or customer..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="block w-full sm:w-48 pl-3 pr-10 py-2 text-base border-slate-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md"
        >
          <option value="ALL">All Statuses</option>
          <option value="PENDING">Pending</option>
          <option value="PAID">Paid</option>
          <option value="OVERDUE">Overdue</option>
          <option value="CANCELLED">Cancelled</option>
        </select>
      </div>

      {invoices.length === 0 ? (
        <EmptyState 
          icon={FileText}
          title="No invoices yet"
          description="Create your first invoice to start tracking sales."
          action={
            <Link
              to="/invoices/create"
              className="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700"
            >
              Create Invoice
            </Link>
          }
        />
      ) : (
        <div className="bg-white shadow-sm ring-1 ring-slate-200 rounded-lg overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200">
              <thead className="bg-slate-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Invoice</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Customer</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Date</th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-slate-500 uppercase tracking-wider">Amount</th>
                  <th className="px-6 py-3 text-center text-xs font-medium text-slate-500 uppercase tracking-wider">Status</th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-slate-500 uppercase tracking-wider">Actions</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-slate-200">
                {filteredInvoices.map((invoice) => (
                  <tr key={invoice.id} className="hover:bg-slate-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-indigo-600">
                      <Link to={`/invoices/${invoice.id}`}>{invoice.invoiceNumber || `INV-${invoice.id}`}</Link>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-900">{invoice.customerName}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-500">
                      {invoice.createdAt ? format(new Date(invoice.createdAt), 'MMM dd, yyyy') : '-'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-900 text-right">
                      ₹{parseFloat(invoice.totalAmount || 0).toLocaleString()}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-center">
                      <span className={`inline-flex px-2 text-xs font-semibold leading-5 rounded-full ${
                        invoice.status === 'PAID' ? 'bg-green-100 text-green-800' : 
                        invoice.status === 'PENDING' ? 'bg-amber-100 text-amber-800' : 
                        invoice.status === 'CANCELLED' ? 'bg-red-100 text-red-800' :
                        'bg-slate-100 text-slate-800'
                      }`}>
                        {invoice.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <Link to={`/invoices/${invoice.id}`} className="text-indigo-600 hover:text-indigo-900 mr-4">
                        <Eye className="w-4 h-4 inline" />
                      </Link>
                      {invoice.status !== 'CANCELLED' && (
                        <button onClick={() => handleCancel(invoice.id)} className="text-red-600 hover:text-red-900" title="Cancel Invoice">
                          <XCircle className="w-4 h-4 inline" />
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
