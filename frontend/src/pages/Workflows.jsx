import React, { useEffect, useState } from 'react';
import { ArrowRight, FileClock, RefreshCw, WalletCards } from 'lucide-react';
import { workflowApi } from '../api/workflowApi';

const money = (value) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(value || 0);

export default function Workflows() {
  const [quotes, setQuotes] = useState([]);
  const [aging, setAging] = useState([]);
  const [state, setState] = useState('loading');
  const [error, setError] = useState('');

  const load = async () => {
    setState('loading'); setError('');
    try {
      const [quoteResponse, agingResponse] = await Promise.all([workflowApi.listQuotations(), workflowApi.aging()]);
      setQuotes(quoteResponse.data); setAging(agingResponse.data); setState('ready');
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Unable to load billing workflows.'); setState('error');
    }
  };

  useEffect(() => { load(); }, []);

  const convert = async (id) => {
    try { await workflowApi.convertQuotation(id); await load(); }
    catch (requestError) { setError(requestError.response?.data?.message || 'Quotation conversion failed.'); }
  };

  if (state === 'loading') return <div className="p-6 sm:p-8 text-slate-600">Loading workflows…</div>;
  if (state === 'error') return <div className="p-6 sm:p-8"><p className="text-red-600">{error}</p><button onClick={load} className="mt-4 rounded-lg bg-indigo-600 px-4 py-2 text-white">Try again</button></div>;

  return <div className="space-y-6 p-4 sm:p-8">
    <div><h1 className="text-2xl font-bold text-slate-900">Daily workflows</h1><p className="mt-1 text-sm text-slate-500">Quotes, receivables, recurring billing and customer credit in one place.</p></div>
    {error && <div className="rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</div>}
    <div className="grid gap-4 md:grid-cols-3">
      <Summary icon={FileClock} label="Quotations" value={quotes.length} />
      <Summary icon={WalletCards} label="Outstanding" value={money(aging.reduce((sum, row) => sum + Number(row.total), 0))} />
      <Summary icon={RefreshCw} label="Recurring billing" value="Idempotent runs" />
    </div>
    <section className="overflow-hidden rounded-xl border border-slate-200 bg-white">
      <div className="border-b border-slate-200 px-4 py-4 sm:px-6"><h2 className="font-semibold text-slate-900">Quotations</h2></div>
      {quotes.length === 0 ? <Empty text="No quotations yet." /> : <div className="overflow-x-auto"><table className="min-w-[720px] w-full text-sm">
        <thead className="bg-slate-50 text-left text-slate-500"><tr><th className="p-4">Number</th><th>Customer</th><th>Valid until</th><th>Status</th><th className="text-right">Total</th><th /></tr></thead>
        <tbody>{quotes.map((quote) => <tr key={quote.id} className="border-t border-slate-100"><td className="p-4 font-medium">{quote.number}</td><td>{quote.customerName}</td><td>{quote.validUntil || '—'}</td><td>{quote.status}</td><td className="text-right">{money(quote.totalAmount)}</td><td className="p-4 text-right">{!quote.convertedInvoiceId && <button onClick={() => convert(quote.id)} className="inline-flex items-center gap-1 text-indigo-600">Convert <ArrowRight size={16} /></button>}</td></tr>)}</tbody>
      </table></div>}
    </section>
    <section className="rounded-xl border border-slate-200 bg-white p-4 sm:p-6"><h2 className="font-semibold text-slate-900">Receivables aging</h2>
      {aging.length === 0 ? <Empty text="No outstanding customer balances." /> : <div className="mt-4 grid gap-3 md:grid-cols-2">{aging.map((row) => <div key={row.customerId} className="rounded-lg border border-slate-200 p-4"><div className="flex justify-between"><strong>{row.customerName}</strong><strong>{money(row.total)}</strong></div><div className="mt-3 grid grid-cols-4 gap-2 text-xs text-slate-500"><span>0–30<br />{money(row.days0To30)}</span><span>31–60<br />{money(row.days31To60)}</span><span>61–90<br />{money(row.days61To90)}</span><span>90+<br />{money(row.days90Plus)}</span></div></div>)}</div>}
    </section>
  </div>;
}

function Summary({ icon: Icon, label, value }) { return <div className="rounded-xl border border-slate-200 bg-white p-5"><Icon className="text-indigo-600" size={22} /><p className="mt-3 text-sm text-slate-500">{label}</p><p className="mt-1 text-xl font-semibold text-slate-900">{value}</p></div>; }
function Empty({ text }) { return <p className="p-8 text-center text-sm text-slate-500">{text}</p>; }
