import React, { useState, useEffect } from 'react';
import { BarChart3, TrendingUp, TrendingDown, MapPin, UserPlus, IndianRupee, FileText, Tag, CreditCard, Smartphone, Banknote, ArrowRight, X } from 'lucide-react';
import { getSalesReport, getProductsReport, getCustomersReport, getPaymentsReport, getNewCustomersReport } from '../api/reportApi';

export default function Reports() {
  const [salesReport, setSalesReport] = useState(null);
  const [productsReport, setProductsReport] = useState([]);
  const [customersReport, setCustomersReport] = useState([]);
  const [paymentsReport, setPaymentsReport] = useState(null);
  const [newCustomersReport, setNewCustomersReport] = useState([]);
  const [loading, setLoading] = useState(true);
  const [timeFilter, setTimeFilter] = useState("ALL");
  const [activeModal, setActiveModal] = useState(null);

  useEffect(() => {
    const fetchReports = async () => {
      setLoading(true);
      try {
        let startDate = null;
        let endDate = null;
        const today = new Date();
        
        if (timeFilter === 'DAILY') {
          startDate = today.toISOString().split('T')[0];
          endDate = startDate;
        } else if (timeFilter === 'WEEKLY') {
          const firstDay = new Date(today.setDate(today.getDate() - today.getDay()));
          startDate = firstDay.toISOString().split('T')[0];
          const lastDay = new Date(today.setDate(today.getDate() - today.getDay() + 6));
          endDate = lastDay.toISOString().split('T')[0];
        } else if (timeFilter === 'MONTHLY') {
          startDate = new Date(today.getFullYear(), today.getMonth(), 1).toISOString().split('T')[0];
          endDate = new Date(today.getFullYear(), today.getMonth() + 1, 0).toISOString().split('T')[0];
        }

        const [sales, products, customers] = await Promise.all([
          getSalesReport(startDate, endDate),
          getProductsReport(startDate, endDate),
          getCustomersReport(startDate, endDate)
        ]);

        setSalesReport(sales.data || sales);
        setProductsReport(products.data || products || []);
        setCustomersReport(customers.data || customers || []);
        
        try {
          const paymentsRes = await getPaymentsReport(startDate, endDate);
          setPaymentsReport(paymentsRes.data || paymentsRes);
        } catch (e) {
          console.error('Failed to fetch payments', e);
          setPaymentsReport(null);
        }
        
        try {
          const res = await getNewCustomersReport(startDate, endDate);
          setNewCustomersReport(res.data || res || []);
        } catch (e) {
          console.error('Failed to fetch new customers', e);
          setNewCustomersReport([]);
        }

      } catch (err) {
        console.error('Failed to fetch reports', err);
      } finally {
        setLoading(false);
      }
    };
    fetchReports();
  }, [timeFilter]);

  if (loading && !salesReport) {
    return (
      <div className="p-4 sm:p-6 lg:p-8 flex justify-center items-center h-96">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  if (!salesReport) {
    return (
      <div className="p-4 sm:p-6 lg:p-8">
        <div className="mb-8 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-slate-900">Reports</h1>
            <p className="mt-2 text-sm text-slate-700">Business insights and financial summaries.</p>
          </div>
        </div>
        <div className="text-center p-12 bg-white rounded-lg shadow-sm ring-1 ring-slate-200">
          <BarChart3 className="mx-auto h-12 w-12 text-slate-400" />
          <h3 className="mt-2 text-sm font-medium text-slate-900">No reporting data yet</h3>
          <p className="mt-1 text-sm text-slate-500">Create invoices to start seeing business insights.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6 lg:p-8">
      <div className="mb-8 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Reports & Insights</h1>
          <p className="mt-2 text-sm text-slate-700">Comprehensive overview of your business performance.</p>
        </div>
        <div className="flex bg-slate-100 p-1 rounded-lg">
          {['ALL', 'DAILY', 'WEEKLY', 'MONTHLY'].map(filter => (
            <button
              key={filter}
              onClick={() => setTimeFilter(filter)}
              className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${timeFilter === filter ? 'bg-white shadow-sm text-indigo-600' : 'text-slate-600 hover:text-slate-900'}`}
            >
              {filter.charAt(0) + filter.slice(1).toLowerCase()}
            </button>
          ))}
        </div>
      </div>
      
      {/* Highest Revenue Banner */}
      {salesReport.highestRevenueDate && (
        <div className="bg-emerald-50 rounded-lg p-6 mb-8 border border-emerald-100 flex justify-between items-center">
          <div>
            <p className="text-emerald-800 text-sm font-medium mb-1">Best Sales Day</p>
            <p className="text-emerald-600 text-xs">{salesReport.highestRevenueDay || '-'}</p>
          </div>
          <div className="text-right">
            <p className="text-3xl font-bold text-emerald-700">₹{parseFloat(salesReport.highestRevenueAmount || 0).toLocaleString()}</p>
            
          </div>
        </div>
      )}

      {/* KPI Cards */}
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4 mb-8">
        <div className="bg-white p-6 shadow-sm ring-1 ring-slate-200 rounded-lg">
          <div className="flex items-center">
            <div className="flex-shrink-0 bg-emerald-100 rounded-md p-3">
              <IndianRupee className="h-6 w-6 text-emerald-600" />
            </div>
            <div className="ml-5 w-0 flex-1">
              <dt className="text-sm font-medium text-slate-500 truncate">Total Sales</dt>
              <dd className="text-2xl font-bold text-slate-900">₹{parseFloat(salesReport?.totalRevenue || 0).toLocaleString()}</dd>
            </div>
          </div>
        </div>
        <div className="bg-white p-6 shadow-sm ring-1 ring-slate-200 rounded-lg">
          <div className="flex items-center">
            <div className="flex-shrink-0 bg-indigo-100 rounded-md p-3">
              <FileText className="h-6 w-6 text-indigo-600" />
            </div>
            <div className="ml-5 w-0 flex-1">
              <dt className="text-sm font-medium text-slate-500 truncate">Total Invoices</dt>
              <dd className="text-2xl font-bold text-slate-900">{salesReport?.totalInvoices || 0}</dd>
            </div>
          </div>
        </div>
        <div className="bg-white p-6 shadow-sm ring-1 ring-slate-200 rounded-lg">
          <div className="flex items-center">
            <div className="flex-shrink-0 bg-orange-100 rounded-md p-3">
              <TrendingUp className="h-6 w-6 text-orange-600" />
            </div>
            <div className="ml-5 w-0 flex-1">
              <dt className="text-sm font-medium text-slate-500 truncate">Items Sold</dt>
              <dd className="text-2xl font-bold text-slate-900">{salesReport?.totalItemsSold || 0}</dd>
            </div>
          </div>
        </div>
        <div className="bg-white p-6 shadow-sm ring-1 ring-slate-200 rounded-lg">
          <div className="flex items-center">
            <div className="flex-shrink-0 bg-teal-100 rounded-md p-3">
              <IndianRupee className="h-6 w-6 text-teal-600" />
            </div>
            <div className="ml-5 w-0 flex-1">
              <dt className="text-sm font-medium text-slate-500 truncate">Net Profit</dt>
              <dd className="text-2xl font-bold text-slate-900">₹{parseFloat((salesReport?.totalRevenue || 0) * 0.4).toLocaleString()}</dd>
            </div>
          </div>
        </div>
      </div>

      {/* Row 1: Top Products & Payment Breakdown */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
        {/* Top Products */}
        <div className="bg-white shadow-sm ring-1 ring-slate-200 rounded-lg overflow-hidden flex flex-col">
          <div className="px-5 py-4 border-b border-slate-200">
            <h3 className="text-lg font-medium text-slate-900">Top Selling Products</h3>
          </div>
          <ul className="divide-y divide-slate-200 flex-1">
            {productsReport.length > 0 ? productsReport.slice(0, 5).map(product => (
              <li key={product.productId} className="px-5 py-4 flex justify-between items-center">
                <div>
                  <p className="text-sm font-bold text-slate-900">{product.productName}</p>
                  <p className="text-xs text-slate-500">SKU: {product.sku}</p>
                </div>
                <div className="text-right">
                  <p className="text-sm font-bold text-indigo-600">₹{parseFloat(product.revenue || 0).toLocaleString()}</p>
                  <p className="text-xs text-slate-500">{product.quantitySold} sold</p>
                </div>
              </li>
            )) : <li className="px-5 py-4 text-sm text-slate-500">No product data available</li>}
          </ul>
          <div className="px-5 py-3 border-t border-slate-100 bg-slate-50 text-center">
            <button onClick={() => setActiveModal("topProducts")} className="text-sm font-medium text-indigo-600 hover:text-indigo-800 flex items-center justify-center w-full">View all <ArrowRight className="w-4 h-4 ml-1" /></button>
          </div>
        </div>

        {/* Payment Breakdown */}
        <div className="bg-white shadow-sm ring-1 ring-slate-200 rounded-lg overflow-hidden flex flex-col">
          <div className="px-5 py-4 border-b border-slate-200">
            <h3 className="text-lg font-medium text-slate-900">Payment Breakdown</h3>
          </div>
          <div className="p-6 flex-1 flex flex-col justify-center space-y-6">
            {(() => {
              const cash = parseFloat(paymentsReport?.cashAmount || 0);
              const upi = parseFloat(paymentsReport?.upiAmount || 0);
              const card = parseFloat(paymentsReport?.cardAmount || 0);
              const total = cash + upi + card || 1; // prevent div by 0
              
              const payments = [
                { label: 'Cash', amount: cash, color: 'bg-emerald-500' },
                { label: 'UPI', amount: upi, color: 'bg-blue-500' },
                { label: 'Card', amount: card, color: 'bg-purple-500' },
              ].sort((a,b) => b.amount - a.amount);

              return payments.map((p, i) => (
                <div key={i} className="flex items-center">
                  <div className="w-16 text-sm font-medium text-slate-700">{p.label}</div>
                  <div className="w-24 text-sm font-bold text-slate-900 text-right mr-4">₹{p.amount.toLocaleString()}</div>
                  <div className="flex-1 h-3 bg-slate-100 rounded-full overflow-hidden flex">
                    <div className={`h-full ${p.color} rounded-full`} style={{ width: `${(p.amount / total) * 100}%` }}></div>
                  </div>
                </div>
              ));
            })()}
          </div>
          
        </div>
      </div>

      {/* Row 2: Top Customers & Business Insights */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
        {/* Top Customers */}
        <div className="bg-white shadow-sm ring-1 ring-slate-200 rounded-lg overflow-hidden flex flex-col">
          <div className="px-5 py-4 border-b border-slate-200">
            <h3 className="text-lg font-medium text-slate-900">Top Customers</h3>
          </div>
          <ul className="divide-y divide-slate-200 flex-1">
            {customersReport.length > 0 ? customersReport.slice(0, 5).map(customer => (
              <li key={customer.customerId} className="px-5 py-4 flex justify-between items-center">
                <div>
                  <p className="text-sm font-bold text-slate-900">{customer.customerName}</p>
                  <p className="text-xs text-slate-500 mt-0.5 truncate max-w-[200px]">{customer.address || 'No address'}</p>
                </div>
                <div className="text-right">
                  <p className="text-sm font-bold text-green-600">₹{parseFloat(customer.totalPurchase || 0).toLocaleString()}</p>
                  <p className="text-xs text-slate-500">{customer.invoiceCount} invoices</p>
                </div>
              </li>
            )) : <li className="px-5 py-4 text-sm text-slate-500">No customer data available</li>}
          </ul>
          <div className="px-5 py-3 border-t border-slate-100 bg-slate-50 text-center">
            <button onClick={() => setActiveModal("topCustomers")} className="text-sm font-medium text-indigo-600 hover:text-indigo-800 flex items-center justify-center w-full">View all <ArrowRight className="w-4 h-4 ml-1" /></button>
          </div>
        </div>

        {/* Business Insights */}
        <div className="bg-white shadow-sm ring-1 ring-slate-200 rounded-lg overflow-hidden flex flex-col">
          <div className="px-5 py-4 border-b border-slate-200">
            <h3 className="text-lg font-medium text-slate-900">Business Insights</h3>
          </div>
          <div className="p-6 space-y-6 flex-1 bg-slate-50">
            {(() => {
              // Top Location safely
              const locs = (customersReport || []).reduce((acc, curr) => {
                const addr = curr.address || 'Unknown';
                acc[addr] = (acc[addr] || 0) + (curr.invoiceCount || 0);
                return acc;
              }, {});
              const sortedLocs = Object.entries(locs).sort((a,b)=>b[1]-a[1]);
              const topLoc = sortedLocs.length > 0 ? sortedLocs[0] : null;

              // Slow Product safely
              const slowProd = (productsReport && productsReport.length > 0) ? [...productsReport].sort((a,b)=>(a.quantitySold||0) - (b.quantitySold||0))[0] : null;

              // Newest Customer safely
              const newCust = (newCustomersReport && newCustomersReport.length > 0) ? newCustomersReport[0] : null;

              return (
                <>
                  <div className="bg-white p-4 rounded-lg shadow-sm border border-slate-100 flex justify-between items-center">
                    <div>
                      <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1">Top Customer Location</p>
                      <p className="text-lg font-bold text-slate-900 flex items-center">
                        <MapPin className="w-4 h-4 mr-2 text-indigo-500" />
                        {topLoc ? topLoc[0] : 'N/A'}
                      </p>
                    </div>
                    <button onClick={() => setActiveModal('locations')} className="text-xs font-medium text-indigo-600 hover:text-indigo-800 cursor-pointer">View all &rarr;</button>
                  </div>

                  <div className="bg-white p-4 rounded-lg shadow-sm border border-slate-100 flex justify-between items-center">
                    <div>
                      <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1">Slowest Moving Product</p>
                      <p className="text-lg font-bold text-slate-900 flex items-center">
                        <TrendingDown className="w-4 h-4 mr-2 text-orange-500" />
                        {slowProd ? slowProd.productName : 'N/A'}
                      </p>
                    </div>
                    <button onClick={() => setActiveModal('slowProducts')} className="text-xs font-medium text-indigo-600 hover:text-indigo-800 cursor-pointer">View all &rarr;</button>
                  </div>

                  <div className="bg-white p-4 rounded-lg shadow-sm border border-slate-100 flex justify-between items-center">
                    <div>
                      <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1">Most Recent Customer</p>
                      <p className="text-lg font-bold text-slate-900 flex items-center">
                        <UserPlus className="w-4 h-4 mr-2 text-green-500" />
                        {newCust ? newCust.customerName : 'N/A'}
                      </p>
                    </div>
                    <button onClick={() => setActiveModal('recentCustomers')} className="text-xs font-medium text-indigo-600 hover:text-indigo-800 cursor-pointer">View all &rarr;</button>
                  </div>
                </>
              );
            })()}
          </div>
        </div>
      </div>
      
      {/* Dynamic Modals for Business Insights */}
      {activeModal && (
        <>
          {/* Backdrop */}
          <div className="fixed inset-0 bg-slate-800/60 backdrop-blur-sm z-40 transition-opacity" onClick={() => setActiveModal(null)} />
          
          {/* Modal Container */}
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 pointer-events-none">
            <div className="bg-white rounded-xl shadow-2xl w-full max-w-lg overflow-hidden flex flex-col max-h-[85vh] pointer-events-auto ring-1 ring-slate-900/5">
              
              <div className="px-6 py-4 border-b border-slate-100 flex justify-between items-center bg-slate-50/80">
                <h3 className="text-lg font-bold text-slate-800 flex items-center">
                  {activeModal === 'locations' && <><MapPin className="w-5 h-5 mr-2 text-slate-500"/> Top Store Locations</>}
                  {activeModal === 'topProducts' && <><TrendingUp className="w-5 h-5 mr-2 text-slate-500"/> Top Selling Products</>}
                  {activeModal === 'topCustomers' && <><UserPlus className="w-5 h-5 mr-2 text-slate-500"/> Top Customers</>}
                  {activeModal === 'slowProducts' && <><TrendingDown className="w-5 h-5 mr-2 text-slate-500"/> Slow Moving Products</>}
                  {activeModal === 'recentCustomers' && <><UserPlus className="w-5 h-5 mr-2 text-slate-500"/> New Customers</>}
                </h3>
                <button onClick={() => setActiveModal(null)} className="text-slate-400 hover:text-slate-600 transition-colors p-1 rounded-md hover:bg-slate-200">
                  <X className="w-5 h-5" />
                </button>
              </div>
              
              <div className="overflow-y-auto flex-1 p-0">
                <ul className="divide-y divide-slate-100">
                  {(() => {
                    if (activeModal === 'locations') {
                      const locs = (customersReport || []).reduce((acc, curr) => {
                        const addr = curr.address || 'Unknown';
                        acc[addr] = (acc[addr] || 0) + (curr.invoiceCount || 0);
                        return acc;
                      }, {});
                      return Object.entries(locs).sort((a,b)=>b[1]-a[1]).map(([loc, count], idx) => (
                        <li key={idx} className="px-6 py-4 flex justify-between items-center hover:bg-slate-50 transition-colors">
                          <span className="text-sm font-medium text-slate-900">{loc}</span>
                          <span className="text-sm font-bold text-teal-600">{count} visits</span>
                        </li>
                      ));
                    }

                    if (activeModal === 'topProducts') {
                      const sortedProd = (productsReport || []).slice().sort((a,b) => (b.revenue||0) - (a.revenue||0));
                      return sortedProd.map(product => (
                        <li key={product.productId} className="px-6 py-4 flex justify-between items-center hover:bg-slate-50 transition-colors">
                          <div>
                            <p className="text-sm font-bold text-slate-900">{product.productName}</p>
                            <p className="text-xs text-slate-500 mt-0.5">SKU: {product.sku}</p>
                          </div>
                          <div className="text-right">
                            <p className="text-sm font-bold text-indigo-600">₹{parseFloat(product.revenue || 0).toLocaleString()}</p>
                            <p className="text-xs text-slate-500">{product.quantitySold} sold</p>
                          </div>
                        </li>
                      ));
                    }
                    if (activeModal === 'topCustomers') {
                      const sortedCust = (customersReport || []).slice().sort((a,b) => (b.totalPurchase||0) - (a.totalPurchase||0));
                      return sortedCust.map(customer => (
                        <li key={customer.customerId} className="px-6 py-4 flex justify-between items-center hover:bg-slate-50 transition-colors">
                          <div>
                            <p className="text-sm font-bold text-slate-900">{customer.customerName}</p>
                            <p className="text-xs text-slate-500 mt-0.5 truncate max-w-[200px]">{customer.address || 'No address'}</p>
                          </div>
                          <div className="text-right">
                            <p className="text-sm font-bold text-green-600">₹{parseFloat(customer.totalPurchase || 0).toLocaleString()}</p>
                            <p className="text-xs text-slate-500">{customer.invoiceCount} invoices</p>
                          </div>
                        </li>
                      ));
                    }
                    if (activeModal === 'slowProducts') {
                      const sortedProd = (productsReport || []).slice().sort((a,b) => (a.quantitySold||0) - (b.quantitySold||0));
                      return sortedProd.map(product => (
                        <li key={product.productId} className="px-6 py-4 flex justify-between items-center hover:bg-slate-50 transition-colors">
                          <div>
                            <p className="text-sm font-bold text-slate-900">{product.productName}</p>
                            <p className="text-xs text-slate-500 mt-0.5">SKU: {product.sku}</p>
                          </div>
                          <span className="text-sm font-bold text-orange-600">{product.quantitySold} sold</span>
                        </li>
                      ));
                    }
                    if (activeModal === 'recentCustomers') {
                      const recents = newCustomersReport || [];
                      return recents.map(customer => (
                        <li key={customer.customerId} className="px-6 py-4 flex justify-between items-center hover:bg-slate-50 transition-colors">
                          <div>
                            <p className="text-sm font-bold text-slate-900">{customer.customerName}</p>
                            <p className="text-xs text-slate-500 mt-0.5">{customer.address || 'Unknown'}</p>
                          </div>
                          <span className="text-sm font-medium text-teal-600">Joined Recently</span>
                        </li>
                      ));
                    }
                  })()}
                </ul>
              </div>

            </div>
          </div>
        </>
      )}
    </div>
  );
}
