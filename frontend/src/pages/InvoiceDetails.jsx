import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getInvoiceById } from '../api/invoiceApi';
import { LoadingState } from '../components/common/States';
import { ButtonSpinner } from '../components/common/XenBillLoader';
import { 
  ArrowLeft, 
  Printer, 
  Download, 
  CreditCard, 
  MessageCircle, 
  Mail, 
  CheckCircle2, 
  Clock, 
  XCircle,
  FileText,
  Building2,
  Phone,
  Calendar
} from 'lucide-react';
import { format } from 'date-fns';
import { useAuth } from '../context/AuthContext';
import { useBranch } from '../context/BranchContext';
import BrandLogo from '../components/common/BrandLogo';

export default function InvoiceDetails() {
  const { id } = useParams();
  const { user } = useAuth();
  const { branches } = useBranch();

  const [invoice, setInvoice] = useState(null);
  const [loading, setLoading] = useState(true);
  const [downloadingPdf, setDownloadingPdf] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState('CASH');
  const [sendingWhatsApp, setSendingWhatsApp] = useState(false);
  const [sendingEmail, setSendingEmail] = useState(false);
  const [shareMessage, setShareMessage] = useState('');

  const handleDownloadPDF = async () => {
    setDownloadingPdf(true);
    setShareMessage('');
    try {
      const { default: axiosClient } = await import('../api/axiosClient');
      const response = await axiosClient.get(`/invoices/${id}/pdf`, { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `invoice_${invoice?.invoiceNumber || id}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Error downloading PDF', err);
      setShareMessage('Failed to download invoice PDF. Please try again.');
    } finally {
      setDownloadingPdf(false);
    }
  };

  const handlePrint = () => {
    window.print();
  };

  const handleEmail = async () => {
    const email = invoice?.customerEmail || '';
    if (!email || !email.includes('@')) {
      setShareMessage('Customer does not have a valid email address. Please update customer details.');
      return;
    }

    setSendingEmail(true);
    setShareMessage('');
    try {
      const { default: axiosClient } = await import('../api/axiosClient');
      const response = await axiosClient.post(`/invoices/${id}/send-email`);
      const msg = typeof response.data === 'string'
        ? response.data
        : (response.data?.message || 'Invoice PDF sent successfully via Email!');
      setShareMessage(String(msg));
    } catch (err) {
      console.error('Email send error:', err);
      let errMsg = 'Email sending failed.';
      if (err.response?.data) {
        if (typeof err.response.data === 'string') {
          errMsg = err.response.data;
        } else if (typeof err.response.data.message === 'string') {
          errMsg = err.response.data.message;
        } else if (typeof err.response.data.error === 'string') {
          errMsg = err.response.data.error;
        }
      } else if (err.message) {
        errMsg = err.message;
      }
      setShareMessage(String(errMsg));
    } finally {
      setSendingEmail(false);
    }
  };

  const handleWhatsApp = async () => {
    const rawNumber = invoice?.customerWhatsapp || '';
    const digits = rawNumber.replace(/\D/g, '');

    if (!digits) {
      setShareMessage('Customer does not have a WhatsApp number. Please add one in Customers.');
      return;
    }

    if (digits.length < 10) {
      setShareMessage(`Invalid WhatsApp number "${rawNumber}". Indian numbers must be 10 digits (found ${digits.length} digits). Please update customer.`);
      return;
    }

    setSendingWhatsApp(true);
    setShareMessage('');
    try {
      const { default: axiosClient } = await import('../api/axiosClient');
      const response = await axiosClient.post(`/invoices/${id}/send-whatsapp`);
      const msg = typeof response.data === 'string'
        ? response.data
        : (response.data?.message || 'Invoice PDF sent successfully via WhatsApp!');
      setShareMessage(String(msg));
    } catch (err) {
      console.error('WhatsApp send error:', err);
      let errMsg = 'WhatsApp sending failed.';
      if (err.response?.data) {
        if (typeof err.response.data === 'string') {
          errMsg = err.response.data;
        } else if (typeof err.response.data.message === 'string') {
          errMsg = err.response.data.message;
        } else if (typeof err.response.data.error === 'string') {
          errMsg = err.response.data.error;
        }
      } else if (err.message) {
        errMsg = err.message;
      }
      setShareMessage(String(errMsg));
    } finally {
      setSendingWhatsApp(false);
    }
  };

  useEffect(() => {
    const fetchInvoice = async () => {
      try {
        const data = await getInvoiceById(id);
        setInvoice(data);
        
        // Fetch payment method if paid
        if (data.status === 'PAID') {
          import('../api/axiosClient').then(({ default: axiosClient }) => {
            axiosClient.get(`/payments/invoice/${id}`)
              .then(res => setPaymentMethod(res.data?.paymentMethod || 'CASH'))
              .catch(err => console.log('No payment details found', err));
          });
        }
      } catch (err) {
        console.error('Failed to load invoice', err);
      } finally {
        setLoading(false);
      }
    };
    fetchInvoice();
  }, [id]);

  if (loading) return <LoadingState message="Loading invoice details..." />;
  if (!invoice) return (
    <div className="p-8 text-center">
      <div className="inline-flex p-4 rounded-full bg-red-50 text-red-500 mb-4">
        <XCircle className="w-8 h-8" />
      </div>
      <h2 className="text-lg font-bold text-slate-900">Invoice not found</h2>
      <p className="text-sm text-slate-500 mt-1">The requested invoice ID could not be loaded.</p>
      <Link to="/invoices" className="mt-4 inline-flex items-center text-sm font-semibold text-emerald-600 hover:text-emerald-700">
        <ArrowLeft className="w-4 h-4 mr-1.5" /> Back to Invoices
      </Link>
    </div>
  );

  const invoiceShop = branches?.find(b => b.id === invoice?.shopId);
  const shopDetails = invoiceShop || user?.shop || user;
  const displayName = shopDetails?.invoiceName || shopDetails?.shopName || shopDetails?.name || 'XenBill Store';
  const displayAddress = shopDetails?.shopAddress || shopDetails?.address || 'Main Street, Business District';
  const displayMobile = shopDetails?.shopMobileNumber || shopDetails?.mobileNumber || '';
  const displayEmail = shopDetails?.shopEmail || shopDetails?.email || '';
  const displayLogo = shopDetails?.logoBase64 || branches?.[0]?.logoBase64;
  const displayGstin = shopDetails?.gstin || user?.shop?.gstin || branches?.[0]?.gstin;

  const invoiceNumberText = invoice.invoiceNumber || `INV-${String(invoice.id).padStart(5, '0')}`;
  const formattedDate = invoice.createdAt ? format(new Date(invoice.createdAt), 'dd MMM yyyy, hh:mm a') : 'N/A';

  return (
    <div className="p-3 sm:p-6 lg:p-8 max-w-4xl mx-auto w-full">
      {/* Top Action Bar (Screen Only) */}
      <div className="mb-5 sm:mb-6 print:hidden">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 sm:gap-4">
          {/* Title & Status */}
          <div className="flex items-center justify-between sm:justify-start">
            <div className="flex items-center gap-2 sm:gap-3">
              <Link
                to="/invoices"
                className="p-1.5 -ml-1 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-lg transition-colors"
                title="Back to Invoices"
                aria-label="Back to Invoices"
              >
                <ArrowLeft className="w-5 h-5 sm:w-6 sm:h-6" />
              </Link>
              <div>
                <h1 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
                  <span>{invoiceNumberText}</span>
                </h1>
              </div>
            </div>

            {/* Status Badge */}
            <span
              className={`inline-flex items-center gap-1 px-2.5 py-1 text-xs font-semibold rounded-full border ${
                invoice.status === 'PAID'
                  ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                  : invoice.status === 'PENDING'
                  ? 'bg-amber-50 text-amber-700 border-amber-200'
                  : invoice.status === 'CANCELLED'
                  ? 'bg-rose-50 text-rose-700 border-rose-200'
                  : 'bg-slate-100 text-slate-700 border-slate-200'
              }`}
            >
              {invoice.status === 'PAID' && <CheckCircle2 className="w-3.5 h-3.5" />}
              {invoice.status === 'PENDING' && <Clock className="w-3.5 h-3.5" />}
              {invoice.status === 'CANCELLED' && <XCircle className="w-3.5 h-3.5" />}
              {invoice.status}
            </span>
          </div>

          {/* Action Buttons Toolbar: 2x2 grid on mobile, horizontal toolbar on tablet/desktop */}
          <div className="grid grid-cols-2 sm:flex sm:items-center gap-2 sm:gap-2.5 w-full sm:w-auto">
            {/* Print Button */}
            <button
              type="button"
              onClick={handlePrint}
              className="inline-flex items-center justify-center h-10 px-3.5 py-2 border border-slate-300 shadow-sm text-sm font-semibold rounded-lg text-slate-700 bg-white hover:bg-slate-50 hover:border-slate-400 active:bg-slate-100 transition-colors focus:outline-none focus:ring-2 focus:ring-emerald-500/20"
            >
              <Printer className="w-4 h-4 mr-2 text-slate-500" />
              <span>Print</span>
            </button>

            {/* PDF Download Button */}
            <button
              type="button"
              onClick={handleDownloadPDF}
              disabled={downloadingPdf}
              className="inline-flex items-center justify-center h-10 px-3.5 py-2 border border-slate-300 shadow-sm text-sm font-semibold rounded-lg text-slate-700 bg-white hover:bg-slate-50 hover:border-slate-400 active:bg-slate-100 disabled:opacity-50 transition-colors focus:outline-none focus:ring-2 focus:ring-emerald-500/20"
            >
              {downloadingPdf ? <ButtonSpinner /> : <Download className="w-4 h-4 mr-2 text-slate-500" />}
              <span>{downloadingPdf ? 'Saving…' : 'PDF'}</span>
            </button>

            {/* WhatsApp Button */}
            <button
              type="button"
              onClick={handleWhatsApp}
              disabled={sendingWhatsApp}
              className="inline-flex items-center justify-center h-10 px-3.5 py-2 border border-emerald-300 shadow-sm text-sm font-semibold rounded-lg text-emerald-800 bg-emerald-50/70 hover:bg-emerald-100 hover:border-emerald-400 active:bg-emerald-100 disabled:opacity-50 transition-colors focus:outline-none focus:ring-2 focus:ring-emerald-500/20"
            >
              {sendingWhatsApp ? <ButtonSpinner className="w-4 h-4 mr-2 text-emerald-700" /> : <MessageCircle className="w-4 h-4 mr-2 text-emerald-600" />}
              <span>{sendingWhatsApp ? 'Sending…' : 'WhatsApp'}</span>
            </button>

            {/* Email Button */}
            <button
              type="button"
              onClick={handleEmail}
              disabled={sendingEmail}
              className="inline-flex items-center justify-center h-10 px-3.5 py-2 border border-blue-300 shadow-sm text-sm font-semibold rounded-lg text-blue-800 bg-blue-50/70 hover:bg-blue-100 hover:border-blue-400 active:bg-blue-100 disabled:opacity-50 transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            >
              {sendingEmail ? <ButtonSpinner className="w-4 h-4 mr-2 text-blue-700" /> : <Mail className="w-4 h-4 mr-2 text-blue-600" />}
              <span>{sendingEmail ? 'Sending…' : 'Email'}</span>
            </button>
          </div>
        </div>
      </div>

      {/* Share / Action Status Feedback Alert */}
      {shareMessage && (
        <div
          role="alert"
          className={`mb-4 rounded-xl border p-3.5 sm:p-4 text-sm font-medium flex items-start justify-between gap-3 print:hidden ${
            String(shareMessage).toLowerCase().includes('success')
              ? 'border-emerald-200 bg-emerald-50 text-emerald-900 shadow-sm'
              : 'border-amber-200 bg-amber-50 text-amber-900 shadow-sm'
          }`}
        >
          <div className="flex items-center gap-2">
            {String(shareMessage).toLowerCase().includes('success') ? (
              <CheckCircle2 className="w-5 h-5 text-emerald-600 flex-shrink-0" />
            ) : (
              <Clock className="w-5 h-5 text-amber-600 flex-shrink-0" />
            )}
            <span>{String(shareMessage)}</span>
          </div>
          <button
            type="button"
            onClick={() => setShareMessage('')}
            className="text-xs uppercase tracking-wider font-semibold opacity-60 hover:opacity-100 ml-2"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Official Tax Invoice Document Container */}
      <div
        id="invoice-print-area"
        className="bg-white shadow-sm ring-1 ring-slate-200/80 rounded-2xl p-4 sm:p-8 md:p-10 print:shadow-none print:ring-0 print:border-none print:p-0 print:m-0 print:rounded-none"
      >
        {/* Document Header: Shop Info (Left) & Invoice Meta (Right) */}
        <div className="border-b border-slate-200 pb-6 mb-6">
          <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-6">
            {/* Shop Details */}
            <div className="flex items-start gap-4 flex-1">
              {displayLogo ? (
                <img
                  src={displayLogo}
                  alt={displayName}
                  className="h-16 w-16 sm:h-20 sm:w-20 object-contain rounded-xl border border-slate-200 bg-slate-50 p-1 flex-shrink-0"
                />
              ) : (
                <div className="h-16 w-16 sm:h-20 sm:w-20 rounded-xl bg-gradient-to-br from-emerald-500 to-emerald-700 flex items-center justify-center text-white font-extrabold text-2xl shadow-sm flex-shrink-0">
                  {displayName.charAt(0).toUpperCase()}
                </div>
              )}

              <div className="min-w-0 flex-1">
                <h2 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight break-words">
                  {displayName}
                </h2>
                <div className="mt-1 text-xs sm:text-sm text-slate-500 leading-relaxed space-y-0.5">
                  <p className="break-words">{displayAddress}</p>
                  {displayMobile && <p className="font-medium text-slate-600">Mobile: {displayMobile}</p>}
                  {displayEmail && <p className="break-words">Email: {displayEmail}</p>}
                  {displayGstin && (
                    <p className="mt-1 inline-block text-xs font-semibold px-2 py-0.5 rounded bg-slate-100 text-slate-800 border border-slate-200">
                      GSTIN: {displayGstin}
                    </p>
                  )}
                </div>
              </div>
            </div>

            {/* Invoice Meta Block */}
            <div className="sm:text-right flex flex-row sm:flex-col justify-between sm:justify-start items-end sm:items-end border-t sm:border-t-0 pt-4 sm:pt-0 border-slate-100">
              <div>
                <p className="text-xs uppercase tracking-widest font-bold text-emerald-700">Tax Invoice</p>
                <p className="text-lg sm:text-xl font-extrabold text-slate-900 mt-0.5">
                  {invoiceNumberText}
                </p>
              </div>

              <div className="text-right mt-0 sm:mt-3 text-xs sm:text-sm text-slate-500">
                <p className="font-medium text-slate-700">{formattedDate}</p>
                <span
                  className={`mt-1.5 inline-flex items-center px-2 py-0.5 text-xs font-bold rounded-md ${
                    invoice.status === 'PAID'
                      ? 'bg-emerald-100 text-emerald-800'
                      : invoice.status === 'PENDING'
                      ? 'bg-amber-100 text-amber-800'
                      : 'bg-slate-100 text-slate-800'
                  }`}
                >
                  {invoice.status}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Bill To Customer Section */}
        <div className="mb-6 rounded-xl bg-slate-50 border border-slate-200/80 p-4 sm:p-5 print:bg-transparent print:border-slate-300">
          <p className="text-xs uppercase font-bold tracking-wider text-slate-400 mb-2">Billed To</p>
          <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-2">
            <div>
              <p className="text-base font-bold text-slate-900 break-words">
                {invoice.customerName || 'Walk-in Customer'}
              </p>
              <div className="mt-1 text-xs sm:text-sm text-slate-600 space-y-0.5">
                {invoice.customerWhatsapp && (
                  <p className="flex items-center gap-1.5">
                    <span className="text-slate-400 font-medium">WhatsApp:</span> {invoice.customerWhatsapp}
                  </p>
                )}
                {invoice.customerEmail && (
                  <p className="flex items-center gap-1.5 break-all">
                    <span className="text-slate-400 font-medium">Email:</span> {invoice.customerEmail}
                  </p>
                )}
              </div>
            </div>

            {paymentMethod && (
              <div className="sm:text-right mt-2 sm:mt-0">
                <span className="text-xs text-slate-500 block">Payment Mode</span>
                <span className="inline-block mt-1 px-2.5 py-1 text-xs font-bold rounded-md bg-white border border-slate-200 text-slate-800 shadow-2xs">
                  {paymentMethod}
                </span>
              </div>
            )}
          </div>
        </div>

        {/* Invoice Items: Formal Table (Desktop & Print) */}
        <div className="hidden sm:block print:block overflow-x-auto mb-6">
          <table className="min-w-full divide-y divide-slate-200">
            <thead>
              <tr className="bg-slate-50/80 text-slate-600 print:bg-transparent">
                <th scope="col" className="py-3 px-3 text-left text-xs font-bold uppercase tracking-wider w-12">#</th>
                <th scope="col" className="py-3 px-3 text-left text-xs font-bold uppercase tracking-wider">Item Description</th>
                <th scope="col" className="py-3 px-3 text-center text-xs font-bold uppercase tracking-wider w-24">HSN/SAC</th>
                <th scope="col" className="py-3 px-3 text-center text-xs font-bold uppercase tracking-wider w-20">Qty</th>
                <th scope="col" className="py-3 px-3 text-right text-xs font-bold uppercase tracking-wider w-28">Rate</th>
                <th scope="col" className="py-3 px-3 text-right text-xs font-bold uppercase tracking-wider w-32">Total</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-slate-800">
              {invoice.items?.map((item, index) => (
                <tr key={index} className="hover:bg-slate-50/50 print:hover:bg-transparent">
                  <td className="py-3.5 px-3 text-xs text-slate-400">{index + 1}</td>
                  <td className="py-3.5 px-3 text-sm font-semibold text-slate-900 break-words">
                    {item.productName || `Product ID: ${item.productId}`}
                  </td>
                  <td className="py-3.5 px-3 text-xs text-slate-500 text-center font-mono">
                    {item.hsnCode || '-'}
                  </td>
                  <td className="py-3.5 px-3 text-sm text-slate-700 text-center font-medium">
                    {item.quantity}
                  </td>
                  <td className="py-3.5 px-3 text-sm text-slate-700 text-right whitespace-nowrap font-medium">
                    ₹{parseFloat(item.unitPrice || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </td>
                  <td className="py-3.5 px-3 text-sm font-bold text-slate-900 text-right whitespace-nowrap">
                    ₹{parseFloat(item.totalPrice || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Mobile Invoice Items: Clean Card View (< sm screen only, hidden on print) */}
        <div className="block sm:hidden print:hidden mb-6 space-y-2.5">
          <p className="text-xs uppercase font-bold tracking-wider text-slate-400 px-1">Items ({invoice.items?.length || 0})</p>
          {invoice.items?.map((item, index) => (
            <div
              key={index}
              className="p-3.5 rounded-xl border border-slate-200/80 bg-white shadow-2xs flex flex-col gap-2"
            >
              <div className="flex items-start justify-between gap-2">
                <span className="text-sm font-bold text-slate-900 break-words flex-1">
                  {item.productName || `Product ID: ${item.productId}`}
                </span>
                <span className="text-sm font-extrabold text-slate-900 whitespace-nowrap">
                  ₹{parseFloat(item.totalPrice || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </span>
              </div>

              <div className="flex items-center justify-between text-xs text-slate-500 pt-1 border-t border-slate-100">
                <div className="flex items-center gap-2">
                  <span className="font-medium text-slate-600">Qty: {item.quantity}</span>
                  {item.hsnCode && (
                    <span className="px-1.5 py-0.5 rounded bg-slate-100 text-slate-600 font-mono text-[10px]">
                      HSN {item.hsnCode}
                    </span>
                  )}
                </div>
                <span>
                  Rate: ₹{parseFloat(item.unitPrice || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </span>
              </div>
            </div>
          ))}
        </div>

        {/* Totals & Tax Summary Breakdown */}
        <div className="border-t border-slate-200 pt-5 print:pt-4 break-inside-avoid">
          <div className="flex flex-col sm:flex-row justify-between items-start gap-6">
            {/* Notes / Payment info */}
            <div className="text-xs text-slate-500 max-w-sm hidden sm:block print:block">
              <p className="font-bold text-slate-700 uppercase tracking-wider mb-1">Terms & Conditions</p>
              <p className="leading-relaxed">
                Goods once sold cannot be returned without original invoice. Computer generated tax invoice powered by XenBill.
              </p>
            </div>

            {/* Calculations Table */}
            <div className="w-full sm:max-w-xs space-y-2.5">
              <div className="flex justify-between text-xs sm:text-sm text-slate-600">
                <span>Taxable Amount</span>
                <span className="font-medium text-slate-900">
                  ₹{parseFloat(invoice.taxableAmount || invoice.subtotal || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </span>
              </div>

              {parseFloat(invoice.discountAmount || 0) > 0 && (
                <div className="flex justify-between text-xs sm:text-sm text-emerald-700">
                  <span>Discount</span>
                  <span className="font-medium">
                    -₹{parseFloat(invoice.discountAmount).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </span>
                </div>
              )}

              <div className="flex justify-between text-xs sm:text-sm text-slate-600">
                <span>CGST</span>
                <span className="font-medium text-slate-900">
                  +₹{parseFloat(invoice.cgstTotal || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </span>
              </div>

              <div className="flex justify-between text-xs sm:text-sm text-slate-600">
                <span>SGST</span>
                <span className="font-medium text-slate-900">
                  +₹{parseFloat(invoice.sgstTotal || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </span>
              </div>

              {invoice.isInterState && (
                <div className="flex justify-between text-xs sm:text-sm text-slate-600">
                  <span>IGST</span>
                  <span className="font-medium text-slate-900">
                    +₹{parseFloat(invoice.igstTotal || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </span>
                </div>
              )}

              {/* Grand Total Highlight */}
              <div className="pt-2">
                <div className="flex justify-between items-center p-3 sm:p-3.5 bg-emerald-50/70 border border-emerald-200/80 rounded-xl">
                  <span className="text-sm sm:text-base font-bold text-slate-900">Total Amount</span>
                  <span className="text-lg sm:text-2xl font-extrabold text-emerald-700">
                    ₹{parseFloat(invoice.totalAmount || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Invoice Footer */}
        <div className="mt-8 sm:mt-12 text-center text-xs text-slate-400 border-t border-slate-100 pt-6">
          <p className="font-semibold text-slate-700 text-sm">Thank you for your business!</p>
          <div className="mt-2 flex items-center justify-center gap-1.5 opacity-75">
            <span>Powered by</span>
            <BrandLogo variant="horizontal" size="xs" theme="dark" />
          </div>
        </div>
      </div>
    </div>
  );
}
