import React, { useCallback, useEffect, useState } from 'react';
import { BarChart3, Download, FileSpreadsheet, Landmark, PackageSearch, RefreshCw, Save, Settings2 } from 'lucide-react';
import {
  downloadReport, getBusinessReport, getCustomersReport, getDayBook, getGstr1, getGstr3b,
  getPaymentRegister, getProductsReport, getRegister, getRegisterParties, getReportExportSettings,
  getStockMovements, getStockReport, saveReportExportSettings,
} from '../api/reportApi';

const iso = date => date.toISOString().slice(0, 10);
const money = value => `₹${Number(value || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;
const emptyFilters = { partyId: '', taxRate: '', documentType: '' };
const tabs = [
  { id: 'registers', label: 'Registers', icon: FileSpreadsheet },
  { id: 'gst', label: 'GST summaries', icon: Landmark },
  { id: 'inventory', label: 'Inventory', icon: PackageSearch },
  { id: 'business', label: 'Business', icon: BarChart3 },
  { id: 'exports', label: 'Export setup', icon: Settings2 },
];

export default function Reports() {
  const today = new Date();
  const [from, setFrom] = useState(iso(new Date(today.getFullYear(), today.getMonth(), 1)));
  const [to, setTo] = useState(iso(today));
  const [tab, setTab] = useState('registers');
  const [registerType, setRegisterType] = useState('sales');
  const [filters, setFilters] = useState(emptyFilters);
  const [parties, setParties] = useState([]);
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [exportSettings, setExportSettings] = useState(null);
  const [savingSettings, setSavingSettings] = useState(false);
  const [settingsMessage, setSettingsMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!['sales', 'purchases', 'notes'].includes(registerType)) { setParties([]); return; }
    let active = true;
    getRegisterParties(registerType).then(rows => { if (active) setParties(rows); })
      .catch(() => { if (active) setParties([]); });
    return () => { active = false; };
  }, [registerType]);

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try {
      if (tab === 'registers') {
        if (registerType === 'payments') setData(await getPaymentRegister(from, to, page));
        else if (registerType === 'day-book') setData(await getDayBook(from, to, page));
        else setData(await getRegister(registerType, from, to, { ...filters, page, size: 25 }));
      } else if (tab === 'gst') {
        const [gstr1, gstr3b] = await Promise.all([getGstr1(from, to), getGstr3b(from, to)]);
        setData({ gstr1, gstr3b });
      } else if (tab === 'inventory') {
        const [stock, movements] = await Promise.all([getStockReport(false), getStockMovements(from, to)]);
        setData({ stock, movements });
      } else if (tab === 'business') {
        const [summary, products, customers] = await Promise.all([
          getBusinessReport(from, to), getProductsReport(from, to), getCustomersReport(from, to),
        ]);
        setData({ ...summary, productSales: products, customerSales: customers });
      } else if (tab === 'exports') {
        const settings = await getReportExportSettings();
        setExportSettings(settings); setData(settings);
      }
    } catch (cause) {
      setError(cause.response?.data?.message || 'Could not load this report.');
    } finally { setLoading(false); }
  }, [from, to, tab, registerType, filters, page]);

  useEffect(() => { load(); }, [load]);

  const changeDate = (setter, value) => { setPage(0); setter(value); };
  const changeType = value => { setRegisterType(value); setFilters(emptyFilters); setPage(0); };
  const changeFilter = (name, value) => { setFilters(current => ({ ...current, [name]: value })); setPage(0); };
  const exportRegister = extension => {
    const path = registerType === 'payments' ? '/reports/v2/payments/export.csv' : registerType === 'day-book'
      ? '/reports/v2/day-book/export.csv' : `/reports/v2/registers/${registerType}/export.${extension}`;
    return downloadReport(path, from, to, `${registerType}-register.${extension}`, filters);
  };
  const saveSettings = async () => {
    setSavingSettings(true); setSettingsMessage(''); setError('');
    try {
      const saved = await saveReportExportSettings(exportSettings);
      setExportSettings(saved); setSettingsMessage('Ledger mappings saved for this shop.');
    } catch (cause) { setError(cause.response?.data?.message || 'Could not save export settings.'); }
    finally { setSavingSettings(false); }
  };

  return <div className="p-4 sm:p-6 lg:p-8 space-y-6">
    <header className="flex flex-col lg:flex-row lg:items-end lg:justify-between gap-4">
      <div><p className="text-sm font-semibold text-indigo-600">Derived from source documents</p>
        <h1 className="text-3xl font-bold text-slate-950">Reports & exports</h1>
        <p className="mt-1 text-sm text-slate-500">Reconcile sales, GST, stock and balances without maintaining duplicate totals.</p></div>
      <div className="flex flex-wrap items-end gap-3 rounded-xl bg-white p-3 ring-1 ring-slate-200">
        <DateField label="From" value={from} onChange={value => changeDate(setFrom, value)} />
        <DateField label="To" value={to} onChange={value => changeDate(setTo, value)} />
        <button onClick={load} className="h-10 px-4 rounded-lg bg-slate-900 text-white flex items-center gap-2"><RefreshCw size={16}/>Refresh</button>
      </div>
    </header>
    <nav className="flex gap-2 overflow-x-auto pb-1" aria-label="Report sections">
      {tabs.map(item => { const Icon=item.icon; return <button key={item.id} onClick={() => { setTab(item.id); setPage(0); }} className={`shrink-0 flex items-center gap-2 rounded-full px-4 py-2 text-sm font-semibold ${tab===item.id?'bg-indigo-600 text-white':'bg-white text-slate-600 ring-1 ring-slate-200'}`}><Icon size={16}/>{item.label}</button>; })}
    </nav>
    {error && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-4 text-red-700">{error}</div>}
    {loading ? <Loading/> : <>
      {tab === 'registers' && <Registers data={data} type={registerType} setType={changeType} filters={filters} setFilter={changeFilter} parties={parties} exportReport={exportRegister} setPage={setPage}/>}
      {tab === 'gst' && <Gst data={data} exportExcel={() => downloadReport('/reports/v2/gst/export.xlsx', from, to, 'gst-summary.xlsx')}/>}
      {tab === 'inventory' && <Inventory data={data}/>}
      {tab === 'business' && <Business data={data}/>}
      {tab === 'exports' && <ExportSetup settings={exportSettings} setSettings={setExportSettings} save={saveSettings} saving={savingSettings} message={settingsMessage} from={from} to={to}/>}
    </>}
  </div>;
}

function DateField({ label, value, onChange }) { return <label className="text-xs font-semibold text-slate-500">{label}<input type="date" value={value} onChange={e=>onChange(e.target.value)} className="mt-1 block h-10 rounded-lg border border-slate-200 px-3 text-sm text-slate-800"/></label>; }
function Loading() { return <div className="grid min-h-72 place-items-center rounded-2xl bg-white ring-1 ring-slate-200"><div className="text-center text-slate-500"><RefreshCw className="mx-auto mb-3 animate-spin"/>Preparing report…</div></div>; }
function Empty() { return <div className="py-16 text-center text-slate-500">No report rows for this period.</div>; }

function Registers({ data, type, setType, filters, setFilter, parties, exportReport, setPage }) {
  const rows=data?.content || [];
  const isPayment=type==='payments', isDayBook=type==='day-book', isDocument=!isPayment&&!isDayBook;
  const tableRows=isPayment?rows.map(r=>[r.invoiceNumber,r.date?.slice(0,10),r.partyName,r.method,money(r.amount),r.reference||'—'])
    :isDayBook?rows.map(r=>[r.date?.slice(0,10),r.kind,r.reference,r.partyName,money(r.debit),money(r.credit)])
    :rows.map(r=>[r.number,r.date?.slice(0,10),r.partyName,r.documentType,money(r.taxableAmount),money(r.cgst),money(r.sgst),money(r.igst),money(r.totalAmount)]);
  const headers=isPayment?['Invoice','Date','Party','Method','Amount','Reference']:isDayBook?['Date','Type','Reference','Party','Debit','Credit']:['Number','Date','Party','Type','Taxable','CGST','SGST','IGST','Total'];
  return <div className="space-y-4">
    {isDocument && <div className="grid gap-3 rounded-xl bg-white p-4 ring-1 ring-slate-200 sm:grid-cols-3">
      <SelectField label={type==='purchases'?'Supplier':'Customer'} value={filters.partyId} onChange={v=>setFilter('partyId',v)}><option value="">All parties</option>{parties.map(p=><option key={p.id} value={p.id}>{p.name}{p.gstin?` · ${p.gstin}`:''}</option>)}</SelectField>
      <SelectField label="GST rate" value={filters.taxRate} onChange={v=>setFilter('taxRate',v)}><option value="">All rates</option>{[0,5,12,18,28].map(rate=><option key={rate} value={rate}>{rate}%</option>)}</SelectField>
      {type !== 'purchases' && <SelectField label="Document type" value={filters.documentType} onChange={v=>setFilter('documentType',v)}><option value="">All types</option>{type==='notes'?<><option value="CREDIT_NOTE">Credit note</option><option value="DEBIT_NOTE">Debit note</option></>:<option value="TAX_INVOICE">Tax invoice</option>}</SelectField>}
    </div>}
    <Panel title="Document registers" actions={<><select value={type} onChange={e=>setType(e.target.value)} className="rounded-lg border border-slate-200 px-3 py-2 text-sm"><option value="sales">Sales</option><option value="purchases">Purchases</option><option value="notes">Credit/debit notes</option><option value="payments">Payments</option><option value="day-book">Day book</option></select><ExportButton onClick={()=>exportReport('csv')} label="CSV"/>{isDocument&&<ExportButton onClick={()=>exportReport('xlsx')} label="Excel"/>}</>}>
      {!rows.length?<Empty/>:<Table headers={headers} rows={tableRows}/>}
      <Pagination page={data?.page || 0} totalPages={data?.totalPages || 0} totalElements={data?.totalElements || 0} setPage={setPage}/>
    </Panel>
  </div>;
}

function Gst({ data, exportExcel }) { const g=data?.gstr1, b=data?.gstr3b; return <div className="space-y-5"><div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">{g?.filingWarning}</div><Panel title="GSTR-1 style outward summary" actions={<ExportButton onClick={exportExcel} label="Excel"/>}><Table headers={['Category','Documents','Taxable','CGST','SGST','IGST','Cess']} rows={(g?.outwardSupplies||[]).map(r=>[r.category,r.documentCount,money(r.taxableValue),money(r.cgst),money(r.sgst),money(r.igst),money(r.cess)])}/></Panel><div className="grid sm:grid-cols-2 xl:grid-cols-5 gap-3">{[['Outward taxable',b?.outwardTaxable],['Output CGST',b?.outwardCgst],['Output SGST',b?.outwardSgst],['Output IGST',b?.outwardIgst],['Net payable',b?.netTaxPayable]].map(([label,value])=><Metric key={label} label={label} value={money(value)}/>)}</div><Panel title="HSN summary"><Table headers={['HSN','Description','UQC','Qty','Taxable','Total tax']} rows={(g?.hsnSummary||[]).map(r=>[r.hsn,r.description,r.uqc,r.quantity,money(r.taxableValue),money(Number(r.cgst)+Number(r.sgst)+Number(r.igst)+Number(r.cess))])}/></Panel></div>; }
function Inventory({ data }) { const rows=data?.stock||[], movements=data?.movements||[]; return <div className="space-y-5"><Panel title="Stock valuation · weighted average purchase cost"><div className="grid sm:grid-cols-3 gap-3 p-5"><Metric label="Products" value={rows.length}/><Metric label="Low stock" value={rows.filter(r=>r.lowStock).length}/><Metric label="Stock value" value={money(rows.reduce((sum,r)=>sum+Number(r.stockValue||0),0))}/></div>{!rows.length?<Empty/>:<Table headers={['Product','SKU','HSN','Quantity','Average cost','Stock value','State']} rows={rows.map(r=>[r.productName,r.sku,r.hsn||'—',r.quantity,money(r.weightedAverageCost),money(r.stockValue),r.lowStock?'Low stock':'Healthy'])}/>}</Panel><Panel title="Stock movement ledger"><Table headers={['Date','Product','Type','Quantity','Before','After','Reason']} rows={movements.map(r=>[r.date?.slice(0,10),r.productName,r.movementType,r.quantity,r.stockBefore,r.stockAfter,r.reason||'—'])}/></Panel></div>; }
function Business({ data }) { const p=data?.profitAndLoss; return <div className="space-y-5"><div className="grid sm:grid-cols-2 xl:grid-cols-4 gap-3"><Metric label="Gross sales" value={money(p?.sales)}/><Metric label="Credit notes" value={money(p?.creditNotes)}/><Metric label="Purchases" value={money(p?.purchases)}/><Metric label="Sales less purchases" value={money(p?.grossProfit)}/></div><div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">{p?.expenseLimitation}</div><div className="grid xl:grid-cols-2 gap-5"><Panel title="Sales by customer"><Table headers={['Customer','Invoices','Sales']} rows={(data?.customerSales||[]).map(r=>[r.customerName,r.invoiceCount,money(r.totalPurchase)])}/></Panel><Panel title="Sales by product"><Table headers={['Product','SKU','Quantity','Revenue']} rows={(data?.productSales||[]).map(r=>[r.productName,r.sku,r.quantitySold,money(r.revenue)])}/></Panel><Panel title="Receivables"><BalanceTable rows={data?.receivables}/></Panel><Panel title="Payables"><BalanceTable rows={data?.payables}/></Panel></div></div>; }

function ExportSetup({ settings, setSettings, save, saving, message, from, to }) {
  if (!settings) return <Empty/>;
  const fields=[['salesLedger','Sales ledger'],['purchaseLedger','Purchase ledger'],['cgstLedger','CGST ledger'],['sgstLedger','SGST ledger'],['igstLedger','IGST ledger'],['cessLedger','Cess ledger']];
  return <Panel title="Tally ledger mapping" actions={<button onClick={save} disabled={saving} className="flex items-center gap-2 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"><Save size={15}/>{saving?'Saving…':'Save mappings'}</button>}><div className="space-y-5 p-5"><p className="text-sm text-slate-600">These mappings apply only to the active shop. Import beta XML into a Tally test company before production use.</p><div className="grid gap-4 sm:grid-cols-2">{fields.map(([key,label])=><label key={key} className="text-sm font-semibold text-slate-700">{label}<input value={settings[key] || ''} maxLength={100} onChange={e=>setSettings(current=>({...current,[key]:e.target.value}))} className="mt-1 block w-full rounded-lg border border-slate-200 px-3 py-2 font-normal"/></label>)}</div>{message && <p role="status" className="text-sm font-semibold text-emerald-700">{message}</p>}<div className="flex flex-wrap gap-2 border-t border-slate-100 pt-4"><ExportButton label="Tally sales XML" onClick={()=>downloadReport('/reports/v2/tally/sales.xml',from,to,'tally-sales.xml')}/><ExportButton label="Tally purchases XML" onClick={()=>downloadReport('/reports/v2/tally/purchases.xml',from,to,'tally-purchases.xml')}/></div></div></Panel>;
}

function SelectField({ label, value, onChange, children }) { return <label className="text-xs font-semibold text-slate-500">{label}<select value={value} onChange={e=>onChange(e.target.value)} className="mt-1 block h-10 w-full rounded-lg border border-slate-200 px-3 text-sm text-slate-800">{children}</select></label>; }
function Pagination({ page, totalPages, totalElements, setPage }) { if (!totalElements) return null; return <div className="flex items-center justify-between gap-3 border-t border-slate-100 px-5 py-3 text-xs text-slate-500"><span>{totalElements} documents · Page {page+1} of {Math.max(totalPages,1)}</span><div className="flex gap-2"><button disabled={page===0} onClick={()=>setPage(page-1)} className="rounded border border-slate-200 px-3 py-1.5 font-semibold disabled:opacity-40">Previous</button><button disabled={page+1>=totalPages} onClick={()=>setPage(page+1)} className="rounded border border-slate-200 px-3 py-1.5 font-semibold disabled:opacity-40">Next</button></div></div>; }
function BalanceTable({ rows=[] }) { return !rows.length?<Empty/>:<Table headers={['Party','Invoiced','Paid','Outstanding']} rows={rows.map(r=>[r.partyName,money(r.invoiced),money(r.paid),money(r.outstanding)])}/>; }
function Panel({ title, actions, children }) { return <section className="overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-slate-200"><div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 px-5 py-4"><h2 className="font-bold text-slate-900">{title}</h2><div className="flex gap-2">{actions}</div></div>{children}</section>; }
function Metric({ label, value }) { return <div className="rounded-xl bg-white p-4 ring-1 ring-slate-200"><p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p><p className="mt-2 text-xl font-bold text-slate-950">{value}</p></div>; }
function ExportButton({ onClick, label }) { return <button onClick={onClick} className="flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"><Download size={15}/>{label}</button>; }
function Table({ headers, rows=[] }) { return <div className="overflow-x-auto"><table className="min-w-full text-sm"><thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500"><tr>{headers.map(h=><th key={h} className="whitespace-nowrap px-4 py-3">{h}</th>)}</tr></thead><tbody className="divide-y divide-slate-100">{rows.map((row,i)=><tr key={i} className="hover:bg-slate-50">{row.map((cell,j)=><td key={j} className="whitespace-nowrap px-4 py-3 text-slate-700">{cell ?? '—'}</td>)}</tr>)}</tbody></table></div>; }
