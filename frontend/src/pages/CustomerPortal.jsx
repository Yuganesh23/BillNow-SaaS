import React, { useEffect, useState } from 'react';
import { Download, FileText, LogOut, ReceiptText, ShieldCheck } from 'lucide-react';
import { format } from 'date-fns';

const API_ROOT = import.meta.env.VITE_API_BASE_URL || '/api';
const TOKEN_KEY = 'billnowCustomerPortalToken';

function money(value) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(Number(value || 0));
}

export default function CustomerPortal() {
  const [summary, setSummary] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [downloading, setDownloading] = useState(null);

  useEffect(() => {
    const query = new URLSearchParams(window.location.search);
    const supplied = query.get('token');
    if (supplied) {
      sessionStorage.setItem(TOKEN_KEY, supplied);
      window.history.replaceState({}, document.title, window.location.pathname);
    }
    const token = supplied || sessionStorage.getItem(TOKEN_KEY);
    if (!token) {
      setError('This portal link is missing or has already been cleared. Ask the business for a new link.');
      setLoading(false);
      return;
    }
    fetch(`${API_ROOT}/portal`, { headers: { 'X-Portal-Token': token }, cache: 'no-store' })
      .then(async response => {
        if (!response.ok) throw new Error((await response.json().catch(() => ({}))).message || 'This portal link is invalid or expired.');
        return response.json();
      })
      .then(setSummary)
      .catch(error => setError(error.message))
      .finally(() => setLoading(false));
  }, []);

  const download = async (invoice) => {
    const token = sessionStorage.getItem(TOKEN_KEY);
    setDownloading(invoice.id);
    try {
      const response = await fetch(`${API_ROOT}/portal/invoices/${invoice.id}/pdf`, {
        headers: { 'X-Portal-Token': token }, cache: 'no-store'
      });
      if (!response.ok) throw new Error('Could not download this invoice.');
      const url = URL.createObjectURL(await response.blob());
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = `invoice-${invoice.invoiceNumber.replace(/[^a-z0-9._-]/gi, '-')}.pdf`;
      anchor.click();
      URL.revokeObjectURL(url);
    } catch (error) {
      setError(error.message);
    } finally {
      setDownloading(null);
    }
  };

  const leave = () => {
    sessionStorage.removeItem(TOKEN_KEY);
    window.location.assign('/');
  };

  return (
    <main className="min-h-screen bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-4 sm:px-6">
          <div className="flex items-center gap-3"><span className="grid h-10 w-10 place-items-center rounded-xl bg-indigo-600 text-white"><ReceiptText /></span><div><p className="font-bold">XenBill</p><p className="text-xs text-slate-500">Secure customer portal</p></div></div>
          <button onClick={leave} className="inline-flex items-center gap-2 rounded-lg px-3 py-2 text-sm text-slate-600 hover:bg-slate-100"><LogOut className="h-4 w-4" /> Leave</button>
        </div>
      </header>
      <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6">
        {loading && <div className="rounded-2xl bg-white p-10 text-center shadow-sm">Loading your invoices…</div>}
        {error && <div className="rounded-2xl border border-red-200 bg-red-50 p-6 text-red-800"><h1 className="font-bold">Portal unavailable</h1><p className="mt-2 text-sm">{error}</p></div>}
        {summary && <>
          <section className="rounded-2xl bg-gradient-to-br from-indigo-700 to-violet-700 p-6 text-white shadow-lg sm:p-8">
            <div className="flex items-start justify-between gap-5"><div><p className="text-sm text-indigo-100">{summary.shopName}</p><h1 className="mt-2 text-2xl font-bold sm:text-3xl">Hello, {summary.customerName}</h1><p className="mt-2 max-w-xl text-sm text-indigo-100">View your invoice history and download original PDFs securely.</p></div><ShieldCheck className="h-9 w-9 shrink-0 text-indigo-200" /></div>
            <p className="mt-6 text-xs text-indigo-200">Access expires {format(new Date(summary.expiresAt), 'PPp')}</p>
          </section>
          <section className="mt-6">
            <div className="mb-3 flex items-center justify-between"><h2 className="text-lg font-bold">Invoices</h2><span className="text-sm text-slate-500">{summary.invoices.length} total</span></div>
            {summary.invoices.length === 0 ? <div className="rounded-2xl bg-white p-10 text-center text-slate-500 shadow-sm"><FileText className="mx-auto mb-3 h-10 w-10 text-slate-300" />No invoices are available yet.</div> :
              <div className="grid gap-3">{summary.invoices.map(invoice => <article key={invoice.id} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:flex sm:items-center sm:justify-between sm:p-5">
                <div><div className="flex items-center gap-2"><p className="font-semibold">{invoice.invoiceNumber}</p><span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">{invoice.status}</span></div><p className="mt-1 text-sm text-slate-500">{format(new Date(invoice.createdAt), 'PPp')}</p></div>
                <div className="mt-4 flex items-end justify-between gap-4 sm:mt-0 sm:text-right"><div><p className="font-bold">{money(invoice.totalAmount)}</p><p className="text-xs text-slate-500">Outstanding {money(invoice.outstandingAmount)}</p></div><button onClick={() => download(invoice)} disabled={downloading === invoice.id} className="inline-flex items-center gap-2 rounded-lg bg-indigo-50 px-3 py-2 text-sm font-semibold text-indigo-700 hover:bg-indigo-100 disabled:opacity-60"><Download className="h-4 w-4" />{downloading === invoice.id ? 'Downloading…' : 'PDF'}</button></div>
              </article>)}</div>}
          </section>
        </>}
      </div>
    </main>
  );
}
