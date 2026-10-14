import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { format } from 'date-fns';
import { ArrowLeft, Printer, Download, MessageCircle, ShieldCheck, Truck } from 'lucide-react';
import { getInvoiceById } from '../api/invoiceApi';
import axiosClient from '../api/axiosClient';
import { LoadingState } from '../components/common/States';
import { useAuth } from '../context/AuthContext';
import { useBranch } from '../context/BranchContext';

const money = (value) => Number(value || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

export default function InvoiceDetails() {
  const { id } = useParams();
  const { user } = useAuth();
  const { branches } = useBranch();
  const [invoice, setInvoice] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('');
  const [logoUrl, setLogoUrl] = useState('');
  const [sendingWhatsApp, setSendingWhatsApp] = useState(false);
  const [shareMessage, setShareMessage] = useState('');
  const [eInvoice, setEInvoice] = useState(null);
  const [eWayBill, setEWayBill] = useState(null);
  const [complianceBusy, setComplianceBusy] = useState(false);
  const [showTransport, setShowTransport] = useState(false);
  const [transport, setTransport] = useState({ transporterId: '', transporterName: '', transportDocumentNumber: '', transportDocumentDate: '', transportMode: 'ROAD', vehicleNumber: '', vehicleType: 'REGULAR', distanceKm: '' });

  useEffect(() => {
    const fetchInvoice = async () => {
      try {
        const data = await getInvoiceById(id);
        setInvoice(data);
        if (user?.role === 'SHOP_OWNER' || user?.role === 'MANAGER') {
          axiosClient.get(`/compliance/invoices/${id}`).then(({ data: record }) => setEInvoice(record)).catch(() => {});
          axiosClient.get(`/compliance/e-way-bills/invoices/${id}`).then(({ data: record }) => setEWayBill(record)).catch(() => {});
        }
        if (data.status === 'PAID') {
          axiosClient.get(`/payments/invoice/${id}`)
            .then((response) => setPaymentMethod(response.data.paymentMethod))
            .catch(() => setPaymentMethod(''));
        }
      } catch {
        setError('Failed to load invoice.');
      } finally {
        setLoading(false);
      }
    };
    fetchInvoice();
  }, [id, user?.role]);

  const invoiceShop = branches?.find((branch) => branch.id === invoice?.shopId);
  const shopDetails = invoiceShop || user;

  useEffect(() => {
    let active = true;
    if (shopDetails?.logoBase64) {
      setLogoUrl(shopDetails.logoBase64);
      return () => { active = false; };
    }
    if (!shopDetails?.logoObjectKey) {
      setLogoUrl('');
      return () => { active = false; };
    }
    axiosClient.get('/storage/object-url', { params: { key: shopDetails.logoObjectKey } })
      .then(({ data }) => { if (active) setLogoUrl(data.url); })
      .catch(() => { if (active) setLogoUrl(''); });
    return () => { active = false; };
  }, [shopDetails?.logoBase64, shopDetails?.logoObjectKey]);

  const handleDownloadPDF = async () => {
    try {
      const response = await axiosClient.get(`/invoices/${id}/pdf`, { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      link.download = `invoice_${invoice.invoiceNumber || id}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch {
      setShareMessage('PDF download failed. Please try again.');
    }
  };

  const handleWhatsApp = async () => {
    if (!invoice.customerWhatsapp) {
      setShareMessage('Add a WhatsApp number to this customer before sending.');
      return;
    }
    setSendingWhatsApp(true);
    setShareMessage('');
    try {
      const response = await axiosClient.post(`/invoices/${id}/send-whatsapp`);
      setShareMessage(response.data || 'Invoice PDF sent on WhatsApp.');
      setInvoice((current) => ({ ...current, whatsappStatus: 'SENT' }));
    } catch (requestError) {
      setShareMessage(requestError.response?.data?.message || requestError.response?.data || 'WhatsApp send failed. Check the active branch WhatsApp settings.');
    } finally {
      setSendingWhatsApp(false);
    }
  };

  const generateEInvoice = async () => {
    setComplianceBusy(true); setShareMessage('');
    try {
      const { data } = await axiosClient.post(`/compliance/invoices/${id}/generate`);
      setEInvoice(data);
      setShareMessage(data.status === 'GENERATED' ? 'E-invoice generated and the invoice is now compliance locked.' : data.lastError);
    } catch (requestError) {
      setShareMessage(requestError.response?.data?.message || 'E-invoice generation failed.');
    } finally { setComplianceBusy(false); }
  };

  const generateEWayBill = async (event) => {
    event.preventDefault(); setComplianceBusy(true); setShareMessage('');
    try {
      const payload = { ...transport, distanceKm: Number(transport.distanceKm), transportDocumentDate: transport.transportDocumentDate || null };
      const { data } = await axiosClient.post(`/compliance/e-way-bills/invoices/${id}/generate`, payload);
      setEWayBill(data); setShowTransport(false);
      setShareMessage(data.status === 'GENERATED' ? 'E-way bill generated.' : data.lastError);
    } catch (requestError) {
      setShareMessage(requestError.response?.data?.message || 'E-way bill generation failed.');
    } finally { setComplianceBusy(false); }
  };

  if (loading) return <LoadingState message="Loading invoice..." />;
  if (error || !invoice) return <div className="p-8 text-red-500">{error || 'Invoice not found.'}</div>;

  const displayName = shopDetails?.invoiceName || shopDetails?.shopName || shopDetails?.name || 'BillNow';
  const displayAddress = shopDetails?.shopAddress || shopDetails?.address || '';
  const displayMobile = shopDetails?.shopMobileNumber || shopDetails?.mobileNumber || '';
  const displayEmail = shopDetails?.shopEmail || shopDetails?.email || '';
  const isInterState = Boolean(invoice.isInterState);

  return (
    <div className="p-4 sm:p-6 lg:p-8 max-w-5xl mx-auto">
      <div className="mb-5 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between print:hidden">
        <div className="flex min-w-0 items-center gap-3">
          <Link to="/invoices" className="shrink-0 text-slate-400 hover:text-slate-700"><ArrowLeft className="w-6 h-6" /></Link>
          <h1 className="truncate text-xl sm:text-2xl font-bold text-slate-900">Invoice {invoice.invoiceNumber || `#${invoice.id}`}</h1>
          <span className={`shrink-0 rounded-full px-2 py-1 text-xs font-semibold ${invoice.status === 'PAID' ? 'bg-green-100 text-green-800' : invoice.status === 'CANCELLED' ? 'bg-red-100 text-red-800' : 'bg-amber-100 text-amber-800'}`}>{invoice.status}</span>
        </div>
        <div className="flex flex-wrap gap-2 sm:justify-end">
          <button type="button" onClick={() => window.print()} className="invoice-action"><Printer className="w-4 h-4" /> Print</button>
          <button type="button" onClick={handleDownloadPDF} className="invoice-action"><Download className="w-4 h-4" /> PDF</button>
          <button type="button" onClick={handleWhatsApp} disabled={sendingWhatsApp} className="invoice-action border-green-300 text-green-700 hover:bg-green-50 disabled:opacity-50"><MessageCircle className="w-4 h-4" /> {sendingWhatsApp ? 'Sending…' : 'WhatsApp'}</button>
        </div>
      </div>

      {shareMessage && <div role="status" className="mb-4 rounded-md border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 print:hidden">{shareMessage}</div>}

      {(user?.role === 'SHOP_OWNER' || user?.role === 'MANAGER') && (
        <section className="mb-4 rounded-lg border border-slate-200 bg-white p-4 print:hidden">
          <div className="flex flex-wrap items-center gap-3">
            <ShieldCheck className="h-5 w-5 text-indigo-600" />
            <div className="mr-auto"><p className="text-sm font-semibold text-slate-900">GST compliance</p><p className="text-xs text-slate-500">Generate only after verifying party and transport details.</p></div>
            {eInvoice?.status === 'GENERATED' ? <span className="rounded-full bg-green-100 px-3 py-1 text-xs font-semibold text-green-800">IRN {eInvoice.acknowledgementNumber}</span> : <button type="button" disabled={complianceBusy} onClick={generateEInvoice} className="invoice-action disabled:opacity-50"><ShieldCheck className="h-4 w-4" /> Generate e-invoice</button>}
            {eWayBill?.status === 'GENERATED' ? <span className="rounded-full bg-blue-100 px-3 py-1 text-xs font-semibold text-blue-800">EWB {eWayBill.eWayBillNumber}</span> : <button type="button" disabled={complianceBusy} onClick={() => setShowTransport((value) => !value)} className="invoice-action disabled:opacity-50"><Truck className="h-4 w-4" /> E-way bill</button>}
          </div>
          {showTransport && <form onSubmit={generateEWayBill} className="mt-4 grid grid-cols-1 gap-3 border-t border-slate-100 pt-4 sm:grid-cols-2 lg:grid-cols-4">
            <input value={transport.transporterId} onChange={(e) => setTransport({ ...transport, transporterId: e.target.value })} placeholder="Transporter GSTIN/ID" className="rounded-md border border-slate-300 px-3 py-2 text-sm" />
            <input value={transport.transporterName} onChange={(e) => setTransport({ ...transport, transporterName: e.target.value })} placeholder="Transporter name" className="rounded-md border border-slate-300 px-3 py-2 text-sm" />
            <select value={transport.transportMode} onChange={(e) => setTransport({ ...transport, transportMode: e.target.value })} className="rounded-md border border-slate-300 px-3 py-2 text-sm"><option value="ROAD">Road</option><option value="RAIL">Rail</option><option value="AIR">Air</option><option value="SHIP">Ship</option></select>
            <input value={transport.vehicleNumber} onChange={(e) => setTransport({ ...transport, vehicleNumber: e.target.value })} placeholder="Vehicle number" className="rounded-md border border-slate-300 px-3 py-2 text-sm" />
            <input value={transport.transportDocumentNumber} onChange={(e) => setTransport({ ...transport, transportDocumentNumber: e.target.value })} placeholder="Transport document no." className="rounded-md border border-slate-300 px-3 py-2 text-sm" />
            <input type="date" value={transport.transportDocumentDate} onChange={(e) => setTransport({ ...transport, transportDocumentDate: e.target.value })} className="rounded-md border border-slate-300 px-3 py-2 text-sm" />
            <input required min="0" max="4000" type="number" value={transport.distanceKm} onChange={(e) => setTransport({ ...transport, distanceKm: e.target.value })} placeholder="Distance (km)" className="rounded-md border border-slate-300 px-3 py-2 text-sm" />
            <button disabled={complianceBusy} className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50">{complianceBusy ? 'Submitting…' : 'Generate e-way bill'}</button>
          </form>}
        </section>
      )}

      <article className="bg-white shadow-sm ring-1 ring-slate-200 rounded-lg p-5 sm:p-7 print:shadow-none print:ring-0">
        <header className="grid grid-cols-1 gap-5 border-b border-slate-200 pb-5 sm:grid-cols-[auto_1fr_auto] sm:items-start">
          <div className="flex sm:w-28 sm:justify-start">
            {logoUrl ? <img src={logoUrl} alt={`${displayName} logo`} className="h-16 max-w-28 object-contain" /> : <div className="h-16 w-16 rounded-lg bg-slate-100 flex items-center justify-center text-xs text-slate-400">Logo</div>}
          </div>
          <div className="sm:text-center">
            <h2 className="text-xl font-bold text-indigo-600">{displayName}</h2>
            <div className="mt-1 text-xs leading-5 text-slate-500">{displayAddress && <p>{displayAddress}</p>}{displayMobile && <p>Mobile: {displayMobile}</p>}{displayEmail && <p>{displayEmail}</p>}</div>
          </div>
          <div className="sm:min-w-44 sm:text-right">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-500">Invoice to</h3>
            <p className="mt-1 text-sm font-bold text-slate-900">{invoice.customerName}</p>
            {invoice.customerWhatsapp && <p className="mt-1 text-xs text-slate-500">WhatsApp: {invoice.customerWhatsapp}</p>}
            <p className="mt-3 text-xs text-slate-500">{invoice.createdAt ? format(new Date(invoice.createdAt), 'dd MMM yyyy, hh:mm a') : 'N/A'}</p>
            <p className="mt-1 inline-block rounded border border-slate-200 bg-slate-100 px-2 py-1 text-xs font-medium text-slate-600">{invoice.invoiceNumber || `INV-${invoice.id}`}</p>
          </div>
        </header>

        <div className="mobile-table-scroll py-4">
          <table className="min-w-full">
            <thead className="border-b border-slate-200"><tr><th className="py-3 text-left text-xs font-medium uppercase text-slate-500">Item</th><th className="py-3 text-center text-xs font-medium uppercase text-slate-500">HSN</th><th className="py-3 text-center text-xs font-medium uppercase text-slate-500">Qty</th><th className="py-3 text-right text-xs font-medium uppercase text-slate-500">Rate</th><th className="py-3 text-right text-xs font-medium uppercase text-slate-500">Total</th></tr></thead>
            <tbody className="divide-y divide-slate-100">{invoice.items?.map((item, index) => <tr key={item.id || index}><td className="py-4 text-sm text-slate-900">{item.productName || `Product ID: ${item.productId}`}</td><td className="py-4 text-center text-sm text-slate-500">{item.hsnCode || '-'}</td><td className="py-4 text-center text-sm text-slate-500">{item.quantity}</td><td className="py-4 text-right text-sm text-slate-500">₹{money(item.unitPrice)}</td><td className="py-4 text-right text-sm text-slate-900">₹{money(item.totalPrice)}</td></tr>)}</tbody>
          </table>
        </div>

        <div className="flex justify-end border-t border-slate-200 pt-6"><div className="w-full max-w-xs space-y-3">
          <div className="flex justify-between text-sm text-slate-600"><span>Taxable Amount</span><span>₹{money(invoice.taxableAmount || invoice.subtotal)}</span></div>
          {Number(invoice.discountAmount || 0) > 0 && <div className="flex justify-between text-sm text-slate-600"><span>Discount</span><span className="text-red-500">-₹{money(invoice.discountAmount)}</span></div>}
          {isInterState ? <div className="flex justify-between text-sm text-slate-600"><span>IGST Total</span><span>+₹{money(invoice.igstTotal)}</span></div> : <><div className="flex justify-between text-sm text-slate-600"><span>CGST Total</span><span>+₹{money(invoice.cgstTotal)}</span></div><div className="flex justify-between text-sm text-slate-600"><span>SGST Total</span><span>+₹{money(invoice.sgstTotal)}</span></div></>}
          <div className="flex items-center justify-between border-t border-slate-200 pt-3"><span className="font-medium text-slate-900">Grand Total</span><span className="text-xl font-bold text-indigo-600">₹{money(invoice.totalAmount)}</span></div>
          {paymentMethod && <div className="flex justify-between pt-2 text-sm"><span className="text-slate-600">Payment Method</span><span className="rounded bg-slate-100 px-2 py-1 font-bold text-slate-900">{paymentMethod}</span></div>}
        </div></div>
        <footer className="mt-12 border-t border-slate-200 pt-7 text-center text-sm text-slate-500"><p className="text-lg font-medium text-slate-700">Thank you!</p><p>We appreciate your business.</p></footer>
      </article>
    </div>
  );
}
