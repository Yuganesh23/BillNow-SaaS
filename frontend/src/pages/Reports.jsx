import React, { useCallback, useEffect, useState } from 'react';
import { BarChart3, Download, FileSpreadsheet, Landmark, PackageSearch, RefreshCw } from 'lucide-react';
import { downloadReport, getBusinessReport, getCustomersReport, getDayBook, getGstr1, getGstr3b, getPaymentRegister, getProductsReport, getRegister, getStockMovements, getStockReport } from '../api/reportApi';

const iso = date => date.toISOString().slice(0, 10);
const money = value => `₹${Number(value || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;
const tabs = [
  { id: 'registers', label: 'Registers', icon: FileSpreadsheet },
  { id: 'gst', label: 'GST summaries', icon: Landmark },
  { id: 'inventory', label: 'Inventory', icon: PackageSearch },
  { id: 'business', label: 'Business', icon: BarChart3 },
];

export default function Reports() {
  const today = new Date();
  const [from, setFrom] = useState(iso(new Date(today.getFullYear(), today.getMonth(), 1)));
  const [to, setTo] = useState(iso(today));
  const [tab, setTab] = useState('registers');
  const [registerType, setRegisterType] = useState('sales');
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      if (tab === 'registers') {
        if (registerType === 'payments') setData(await getPaymentRegister(from, to));
        else if (registerType === 'day-book') setData(await getDayBook(from, to));
        else setData(await getRegister(registerType, from, to));
      }
      if (tab === 'gst') {
        const [gstr1, gstr3b] = await Promise.all([getGstr1(from, to), getGstr3b(from, to)]);
        setData({ gstr1, gstr3b });
      }
      if (tab === 'inventory') {
        const [stock, movements] = await Promise.all([getStockReport(false), getStockMovements(from, to)]);
        setData({ stock, movements });
      }
      if (tab === 'business') {
        const [summary, products, customers] = await Promise.all([
          getBusinessReport(from, to), getProductsReport(from, to), getCustomersReport(from, to),
        ]);
        setData({ ...summary, productSales: products, customerSales: customers });
      }
    } catch (cause) {
      setError(cause.response?.data?.message || 'Could not load this report.');
    } finally {
      setLoading(false);
    }
  }, [from, to, tab, registerType]);

  useEffect(() => { load(); }, [load]);

  const exportRegister = extension => {
    const path = registerType === 'payments' ? '/reports/v2/payments/export.csv' : registerType === 'day-book'
      ? '/reports/v2/day-book/export.csv' : `/reports/v2/registers/${registerType}/export.${extension}`;
    return downloadReport(path, from, to, `${registerType}-register.${extension}`);
  };

  return <div className="p-4 sm:p-6 lg:p-8 space-y-6">
    <header className="flex flex-col lg:flex-row lg:items-end lg:justify-between gap-4">
      <div><p className="text-sm font-semibold text-indigo-600">Derived from source documents</p>
        <h1 className="text-3xl font-bold text-slate-950">Reports & exports</h1>
        <p className="mt-1 text-sm text-slate-500">Reconcile sales, GST, stock and balances without maintaining duplicate totals.</p></div>
      <div className="flex flex-wrap items-end gap-3 rounded-xl bg-white p-3 ring-1 ring-slate-200">
        <DateField label="From" value={from} onChange={setFrom} />
        <DateField label="To" value={to} onChange={setTo} />
        <button onClick={load} className="h-10 px-4 rounded-lg bg-slate-900 text-white flex items-center gap-2"><RefreshCw size={16}/>Refresh</button>
      </div>
    </header>

    <nav className="flex gap-2 overflow-x-auto pb-1" aria-label="Report sections">
      {tabs.map(item => { const Icon=item.icon; return <button key={item.id} onClick={() => setTab(item.id)}
        className={`shrink-0 flex items-center gap-2 rounded-full px-4 py-2 text-sm font-semibold ${tab===item.id?'bg-indigo-600 text-white':'bg-white text-slate-600 ring-1 ring-slate-200'}`}>
        <Icon size={16}/>{item.label}</button>; })}
    </nav>

    {error && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-4 text-red-700">{error}</div>}
    {loading ? <Loading/> : <>
      {tab === 'registers' && <Registers data={data} type={registerType} setType={setRegisterType} exportReport={exportRegister}/>}
      {tab === 'gst' && <Gst data={data} exportExcel={() => downloadReport('/reports/v2/gst/export.xlsx', from, to, 'gst-summary.xlsx')}/>}
      {tab === 'inventory' && <Inventory data={data}/>}
      {tab === 'business' && <Business data={data}/>}
    </>}
  </div>;
}

function DateField({ label, value, onChange }) { return <label className="text-xs font-semibold text-slate-500">{label}<input type="date" value={value} onChange={e=>onChange(e.target.value)} className="mt-1 block h-10 rounded-lg border border-slate-200 px-3 text-sm text-slate-800"/></label>; }
function Loading() { return <div className="grid min-h-72 place-items-center rounded-2xl bg-white ring-1 ring-slate-200"><div className="text-center text-slate-500"><RefreshCw className="mx-auto mb-3 animate-spin"/>Preparing report…</div></div>; }
function Empty() { return <div className="py-16 text-center text-slate-500">No report rows for this period.</div>; }

function Registers({ data, type, setType, exportReport }) {
  const rows=Array.isArray(data)?data:(data?.content || []);
  const isPayment=type==='payments', isDayBook=type==='day-book';
  const tableRows=isPayment?rows.map(r=>[r.invoiceNumber,r.date?.slice(0,10),r.partyName,r.method,money(r.amount),r.reference||'—'])
    :isDayBook?rows.map(r=>[r.date?.slice(0,10),r.kind,r.reference,r.partyName,money(r.debit),money(r.credit)])
    :rows.map(r=>[r.number,r.date?.slice(0,10),r.partyName,r.documentType,money(r.taxableAmount),money(r.cgst),money(r.sgst),money(r.igst),money(r.totalAmount)]);
  const headers=isPayment?['Invoice','Date','Party','Method','Amount','Reference']:isDayBook?['Date','Type','Reference','Party','Debit','Credit']:['Number','Date','Party','Type','Taxable','CGST','SGST','IGST','Total'];
  return <Panel title="Document registers" actions={<><select value={type} onChange={e=>setType(e.target.value)} className="rounded-lg border border-slate-200 px-3 py-2 text-sm"><option value="sales">Sales</option><option value="purchases">Purchases</option><option value="notes">Credit/debit notes</option><option value="payments">Payments</option><option value="day-book">Day book</option></select><ExportButton onClick={()=>exportReport('csv')} label="CSV"/>{!isPayment&&!isDayBook&&<ExportButton onClick={()=>exportReport('xlsx')} label="Excel"/>}</>}>
    {!rows.length?<Empty/>:<Table headers={headers} rows={tableRows}/>}
    {!!data?.totalElements && <p className="border-t border-slate-100 px-5 py-3 text-xs text-slate-500">Showing {rows.length} of {data.totalElements} documents</p>}
  </Panel>;
}

function Gst({ data, exportExcel }) {
  const g=data?.gstr1, b=data?.gstr3b;
  return <div className="space-y-5"><div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">{g?.filingWarning}</div>
    <Panel title="GSTR-1 style outward summary" actions={<ExportButton onClick={exportExcel} label="Excel"/>}>
      <Table headers={['Category','Documents','Taxable','CGST','SGST','IGST','Cess']} rows={(g?.outwardSupplies||[]).map(r=>[r.category,r.documentCount,money(r.taxableValue),money(r.cgst),money(r.sgst),money(r.igst),money(r.cess)])}/>
    </Panel>
    <div className="grid sm:grid-cols-2 xl:grid-cols-5 gap-3">{[['Outward taxable',b?.outwardTaxable],['Output CGST',b?.outwardCgst],['Output SGST',b?.outwardSgst],['Output IGST',b?.outwardIgst],['Net payable',b?.netTaxPayable]].map(([label,value])=><Metric key={label} label={label} value={money(value)}/>)}</div>
    <Panel title="HSN summary"><Table headers={['HSN','Description','UQC','Qty','Taxable','Total tax']} rows={(g?.hsnSummary||[]).map(r=>[r.hsn,r.description,r.uqc,r.quantity,money(r.taxableValue),money(Number(r.cgst)+Number(r.sgst)+Number(r.igst)+Number(r.cess))])}/></Panel>
  </div>;
}

function Inventory({ data }) { const rows=data?.stock||[], movements=data?.movements||[]; return <div className="space-y-5"><Panel title="Stock valuation · weighted average purchase cost"><div className="grid sm:grid-cols-3 gap-3 p-5"><Metric label="Products" value={rows.length}/><Metric label="Low stock" value={rows.filter(r=>r.lowStock).length}/><Metric label="Stock value" value={money(rows.reduce((sum,r)=>sum+Number(r.stockValue||0),0))}/></div>{!rows.length?<Empty/>:<Table headers={['Product','SKU','HSN','Quantity','Average cost','Stock value','State']} rows={rows.map(r=>[r.productName,r.sku,r.hsn||'—',r.quantity,money(r.weightedAverageCost),money(r.stockValue),r.lowStock?'Low stock':'Healthy'])}/>}</Panel><Panel title="Stock movement ledger"><Table headers={['Date','Product','Type','Quantity','Before','After','Reason']} rows={movements.map(r=>[r.date?.slice(0,10),r.productName,r.movementType,r.quantity,r.stockBefore,r.stockAfter,r.reason||'—'])}/></Panel></div>; }

function Business({ data }) { const p=data?.profitAndLoss; return <div className="space-y-5"><div className="grid sm:grid-cols-2 xl:grid-cols-4 gap-3"><Metric label="Gross sales" value={money(p?.sales)}/><Metric label="Credit notes" value={money(p?.creditNotes)}/><Metric label="Purchases" value={money(p?.purchases)}/><Metric label="Sales less purchases" value={money(p?.grossProfit)}/></div><div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">{p?.expenseLimitation}</div><div className="grid xl:grid-cols-2 gap-5"><Panel title="Sales by customer"><Table headers={['Customer','Invoices','Sales']} rows={(data?.customerSales||[]).map(r=>[r.customerName,r.invoiceCount,money(r.totalPurchase)])}/></Panel><Panel title="Sales by product"><Table headers={['Product','SKU','Quantity','Revenue']} rows={(data?.productSales||[]).map(r=>[r.productName,r.sku,r.quantitySold,money(r.revenue)])}/></Panel><Panel title="Receivables"><BalanceTable rows={data?.receivables}/></Panel><Panel title="Payables"><BalanceTable rows={data?.payables}/></Panel></div></div>; }
function BalanceTable({ rows=[] }) { return !rows.length?<Empty/>:<Table headers={['Party','Invoiced','Paid','Outstanding']} rows={rows.map(r=>[r.partyName,money(r.invoiced),money(r.paid),money(r.outstanding)])}/>; }
function Panel({ title, actions, children }) { return <section className="overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-slate-200"><div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 px-5 py-4"><h2 className="font-bold text-slate-900">{title}</h2><div className="flex gap-2">{actions}</div></div>{children}</section>; }
function Metric({ label, value }) { return <div className="rounded-xl bg-white p-4 ring-1 ring-slate-200"><p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p><p className="mt-2 text-xl font-bold text-slate-950">{value}</p></div>; }
function ExportButton({ onClick, label }) { return <button onClick={onClick} className="flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"><Download size={15}/>{label}</button>; }
function Table({ headers, rows=[] }) { return <div className="overflow-x-auto"><table className="min-w-full text-sm"><thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500"><tr>{headers.map(h=><th key={h} className="whitespace-nowrap px-4 py-3">{h}</th>)}</tr></thead><tbody className="divide-y divide-slate-100">{rows.map((row,i)=><tr key={i} className="hover:bg-slate-50">{row.map((cell,j)=><td key={j} className="whitespace-nowrap px-4 py-3 text-slate-700">{cell ?? '—'}</td>)}</tr>)}</tbody></table></div>; }
