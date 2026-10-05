import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { getDashboardData } from '../api/dashboardApi';
import { LoadingState } from '../components/common/States';
import { 
  IndianRupee, 
  FileText, 
  Users, 
  Package,
  TrendingUp,
  Clock
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { format } from 'date-fns';

export default function Dashboard() {

  const getGreeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 18) return 'Good afternoon';
    return 'Good evening';
  };

  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const response = await getDashboardData();
        setData(response);
      } catch (err) {
        setError('Failed to load dashboard data');
      } finally {
        setLoading(false);
      }
    };
    fetchDashboard();
  }, []);

  if (loading) return <LoadingState message="Loading dashboard..." />;
  if (error) return <div className="p-8 text-red-500">{error}</div>;

  const StatCard = ({ title, value, icon: Icon, colorClass }) => (
    <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium text-slate-500">{title}</p>
          <p className="mt-2 text-3xl font-semibold text-slate-900">{value}</p>
        </div>
        <div className={`p-3 rounded-full ${colorClass}`}>
          <Icon className="w-6 h-6" />
        </div>
      </div>
    </div>
  );

  return (
    <div className="p-4 sm:p-6 lg:p-8 space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-[28px] font-bold text-slate-900 leading-tight">{getGreeting()}, {user?.name}</h1>
          <p className="mt-1.5 text-[15px] text-slate-500">Here's what's happening with your business today.</p>
        </div>
        <Link 
          to="/invoices/create" 
          className="inline-flex items-center justify-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-indigo-600 hover:bg-indigo-700"
        >
          Create Invoice
        </Link>
      </div>

      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-5">
        <StatCard 
          title="Today's Revenue" 
          value={`₹${(data?.todaySales || 0).toLocaleString()}`} 
          icon={IndianRupee}
          colorClass="bg-green-100 text-green-600"
        />
        <Link to="/invoices" className="block transform transition-transform hover:scale-105">
          <StatCard 
            title="Total Invoices" 
            value={data?.totalInvoices || 0} 
            icon={FileText}
            colorClass="bg-indigo-100 text-indigo-600"
          />
        </Link>
        <Link to="/customers" className="block transform transition-transform hover:scale-105">
          <StatCard 
            title="Total Customers" 
            value={data?.totalCustomers || 0} 
            icon={Users}
            colorClass="bg-blue-100 text-blue-600"
          />
        </Link>
        <Link to="/products" className="block transform transition-transform hover:scale-105">
          <StatCard 
            title="Total Products" 
            value={data?.totalProducts || 0} 
            icon={Package}
            colorClass="bg-orange-100 text-orange-600"
          />
        </Link>
        <Link to="/products?filter=low-stock" className="block transform transition-transform hover:scale-105">
          <StatCard 
            title="Low Stock Items" 
            value={data?.lowStockProducts || 0} 
            icon={Package}
            colorClass="bg-red-100 text-red-600"
          />
        </Link>
      </div>

      <div className="mt-8 bg-white rounded-lg border border-slate-200 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-200">
          <h2 className="text-lg font-medium text-slate-900">Recent Invoices</h2>
        </div>
        
        {data?.recentInvoices && data.recentInvoices.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200">
              <thead className="bg-slate-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Invoice</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Customer</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Date</th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-slate-500 uppercase tracking-wider">Amount</th>
                  <th className="px-6 py-3 text-center text-xs font-medium text-slate-500 uppercase tracking-wider">Status</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-slate-200">
                {data.recentInvoices.map((invoice) => (
                  <tr key={invoice.id} className="hover:bg-slate-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-indigo-600">
                      <Link to={`/invoices/${invoice.id}`}>{invoice.invoiceNumber}</Link>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-indigo-600"><Link to={`/customers/${invoice.customerId}`}>{invoice.customerName}</Link></td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-500">
                      {format(new Date(invoice.createdAt), 'MMM dd, yyyy')}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-slate-900 text-right">
                      ₹{invoice.totalAmount.toLocaleString()}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-center">
                      <span className={`inline-flex px-2 text-xs font-semibold leading-5 rounded-full ${
                        invoice.status === 'PAID' ? 'bg-green-100 text-green-800' : 
                        invoice.status === 'PENDING' ? 'bg-amber-100 text-amber-800' : 
                        'bg-slate-100 text-slate-800'
                      }`}>
                        {invoice.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="p-8 text-center">
            <Clock className="w-12 h-12 text-slate-300 mx-auto mb-3" />
            <p className="text-slate-500 mb-4">No invoices yet.</p>
            <Link to="/invoices/create" className="text-indigo-600 hover:text-indigo-500 font-medium">
              Create your first invoice
            </Link>
          </div>
        )}
      </div>
    </div>
  );
}
