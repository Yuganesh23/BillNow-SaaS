import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getInvoiceById } from '../api/invoiceApi';
import { LoadingState } from '../components/common/States';
import { ArrowLeft, Printer, Download, CreditCard, MessageCircle, Mail } from 'lucide-react';
import { format } from 'date-fns';
import { createPayment } from '../api/paymentApi';
import { useAuth } from '../context/AuthContext';
import { useBranch } from '../context/BranchContext';

export default function InvoiceDetails() {
  const { id } = useParams();
  const { user } = useAuth();
  const { branches } = useBranch();

  const handleDownloadPDF = () => {
    import('../api/axiosClient').then(({ default: axiosClient }) => {
        axiosClient.get(`/invoices/${id}/pdf`, { responseType: 'blob' })
          .then(response => {
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `invoice_${invoice.invoiceNumber || id}.pdf`);
            document.body.appendChild(link);
            link.click();
            link.remove();
          })
          .catch(err => console.error('Error downloading PDF', err));
    });
  };

  const [invoice, setInvoice] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState('CASH');


  useEffect(() => {
    const fetchInvoice = async () => {
      try {
        const data = await getInvoiceById(id);
        setInvoice(data);
        
        // Fetch payment method if paid
        if (data.status === 'PAID') {
          import('../api/axiosClient').then(({ default: axiosClient }) => {
            axiosClient.get(`/payments/invoice/${id}`)
              .then(res => setPaymentMethod(res.data.paymentMethod))
              .catch(err => console.log('No payment details found', err));
          });
        }
      } catch (err) {
        setError('Failed to load invoice');
      } finally {
        setLoading(false);
      }
    };
    fetchInvoice();
  }, [id]);

  if (loading) return <LoadingState message="Loading invoice..." />;
  if (!invoice) return <div className="p-8 text-red-500">Invoice not found.</div>;


  const invoiceShop = branches?.find(b => b.id === invoice?.shopId);
  const shopDetails = invoiceShop || user;
  const displayName = shopDetails?.invoiceName || shopDetails?.shopName || shopDetails?.name || 'BillNow';
  const displayAddress = shopDetails?.shopAddress || shopDetails?.address || '123 Business Avenue, Suite 100';
  const displayMobile = shopDetails?.shopMobileNumber || shopDetails?.mobileNumber || 'N/A';
  const displayEmail = shopDetails?.shopEmail || shopDetails?.email || 'contact@billnow.example.com';
  const displayLogo = shopDetails?.logoBase64;

  return (
    <div className="p-4 sm:p-6 lg:p-8 max-w-4xl mx-auto">
      <div className="mb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex items-center">
          <Link to="/invoices" className="mr-4 text-slate-400 hover:text-slate-600">
            <ArrowLeft className="w-6 h-6" />
          </Link>
          <h1 className="text-2xl font-bold text-slate-900">
            Invoice {invoice.invoiceNumber || `#${invoice.id}`}
          </h1>
          <span className={`ml-4 inline-flex px-2 text-xs font-semibold leading-5 rounded-full ${
            invoice.status === 'PAID' ? 'bg-green-100 text-green-800' : 
            invoice.status === 'PENDING' ? 'bg-amber-100 text-amber-800' : 
            invoice.status === 'CANCELLED' ? 'bg-red-100 text-red-800' :
            'bg-slate-100 text-slate-800'
          }`}>
            {invoice.status}
          </span>
        </div>
        <div className="flex gap-3">
          <button className="inline-flex items-center px-4 py-2 border border-slate-300 shadow-sm text-sm font-medium rounded-md text-slate-700 bg-white hover:bg-slate-50">
            <Printer className="w-4 h-4 mr-2" />
            Print
          </button>
          <button onClick={handleDownloadPDF} className="inline-flex items-center px-4 py-2 border border-slate-300 shadow-sm text-sm font-medium rounded-md text-slate-700 bg-white hover:bg-slate-50">
            <Download className="w-4 h-4 mr-2" />
            PDF
          </button>
          {invoice.status !== 'PAID' && invoice.status !== 'CANCELLED' && (
            <button className="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700">
              <CreditCard className="w-4 h-4 mr-2" />
              Record Payment
            </button>
          )}
        </div>
      </div>

      <div className="bg-white shadow-sm ring-1 ring-slate-200 rounded-lg p-6 print:shadow-none print:ring-0">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 border-b border-slate-200 pb-4 items-start">
            <div className="flex justify-start">
              {displayLogo ? (
                <img src={displayLogo} alt="Shop Logo" className="h-16 w-auto object-contain" />
              ) : (
                <div className="h-16 w-16 bg-slate-100 flex items-center justify-center rounded-lg text-slate-400 text-sm">Logo</div>
              )}
            </div>
            
            <div className="flex flex-col items-center text-center">
              <h2 className="text-xl font-bold text-indigo-600">{displayName}</h2>
              <p className="mt-1 text-xs text-slate-500">
                {displayAddress}<br/>
                Mobile: {displayMobile}<br/>
                {displayEmail}
              </p>
            </div>
            
            <div className="text-right">
              <h3 className="text-sm font-medium text-slate-500 mb-1 uppercase tracking-wider">Invoice To:</h3>
              <p className="text-sm font-bold text-slate-900">{invoice.customerName}</p>
              {invoice.customerWhatsapp && (
                <p className="text-xs text-slate-500 mt-1">WhatsApp: {invoice.customerWhatsapp}</p>
              )}
              <div className="mt-4 text-sm text-slate-500">
                <p>Date: {invoice.createdAt ? format(new Date(invoice.createdAt), 'MMM dd, yyyy HH:mm a') : 'N/A'}</p>
                <p className="mt-1 text-xs font-medium bg-slate-100 inline-block px-2 py-1 rounded text-slate-600 border border-slate-200">
                  {invoice.invoiceNumber || `INV-0000${invoice.id}`}
                </p>
              </div>
            </div>
          </div>
        </div>

                <div className="py-4">
          <table className="min-w-full">
            <thead className="border-b border-slate-200">
              <tr>
                <th className="py-3 text-left text-xs font-medium text-slate-500 uppercase">Item</th>
                <th className="py-3 text-center text-xs font-medium text-slate-500 uppercase">HSN</th>
                <th className="py-3 text-center text-xs font-medium text-slate-500 uppercase">Qty</th>
                <th className="py-3 text-right text-xs font-medium text-slate-500 uppercase">Rate</th>
                <th className="py-3 text-right text-xs font-medium text-slate-500 uppercase">Total</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {invoice.items?.map((item, index) => (
                <tr key={index}>
                  <td className="py-4 text-sm text-slate-900">{item.productName || `Product ID: ${item.productId}`}</td>
                  <td className="py-4 text-sm text-slate-500 text-center">{item.hsnCode || '-'}</td>
                  <td className="py-4 text-sm text-slate-500 text-center">{item.quantity}</td>
                  <td className="py-4 text-sm text-slate-500 text-right">₹{parseFloat(item.unitPrice || 0).toLocaleString()}</td>
                  <td className="py-4 text-sm text-slate-900 text-right">₹{parseFloat(item.totalPrice || 0).toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="flex justify-end pt-6">
          <div className="w-64 space-y-3">
            <div className="flex justify-between text-sm text-slate-600">
              <span>Taxable Amount</span>
              <span>₹{parseFloat(invoice.taxableAmount || invoice.subtotal || 0).toLocaleString()}</span>
            </div>
            {parseFloat(invoice.discountAmount || 0) > 0 && (
              <div className="flex justify-between text-sm text-slate-600">
                <span>Discount</span>
                <span className="text-red-500">-₹{parseFloat(invoice.discountAmount).toLocaleString()}</span>
              </div>
            )}
            <div className="flex justify-between text-sm text-slate-600">
              <span>CGST Total</span>
              <span>+₹{parseFloat(invoice.cgstTotal || 0).toLocaleString()}</span>
            </div>
            <div className="flex justify-between text-sm text-slate-600">
              <span>SGST Total</span>
              <span>+₹{parseFloat(invoice.sgstTotal || 0).toLocaleString()}</span>
            </div>
            {invoice.isInterState && (
              <div className="flex justify-between text-sm text-slate-600">
                <span>IGST Total</span>
                <span>+₹{parseFloat(invoice.igstTotal || 0).toLocaleString()}</span>
              </div>
            )}
            <div className="pt-3 border-t border-slate-200 flex justify-between items-center">
              <span className="text-base font-medium text-slate-900">Total</span>
              <span className="text-xl font-bold text-indigo-600">₹{parseFloat(invoice.totalAmount || 0).toLocaleString()}</span>
            </div>
            {paymentMethod && (
              <div className="pt-3 flex justify-between items-center">
                <span className="text-sm font-medium text-slate-600">Payment Method</span>
                <span className="text-sm font-bold text-slate-900 bg-slate-100 px-2 py-1 rounded">{paymentMethod}</span>
              </div>
            )}
          </div>
        </div>

        <div className="mt-12 text-center text-sm text-slate-500 border-t border-slate-200 pt-8">
          <p className="font-medium text-slate-700 text-lg">Thank you!</p>
          <p className="mt-1">We appreciate your business.</p>
        </div>
      </div>

  );
}




