import React, { useState } from 'react';
import { AlertCircle, CheckCircle2, Download, Upload } from 'lucide-react';
import { commitImport, downloadImportTemplate, previewImport } from '../api/importApi';

const TYPES = [
  ['PRODUCTS', 'Products'],
  ['CUSTOMERS', 'Customers'],
  ['SUPPLIERS', 'Suppliers'],
  ['OPENING_BALANCES', 'Opening balances'],
];

export default function Imports() {
  const [type, setType] = useState('PRODUCTS');
  const [file, setFile] = useState(null);
  const [preview, setPreview] = useState(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');

  const inspect = async () => {
    if (!file) return;
    setBusy(true);
    setMessage('');
    try {
      setPreview(await previewImport(type, file));
    } catch (error) {
      setMessage(error.response?.data?.message || 'The file could not be previewed.');
    } finally {
      setBusy(false);
    }
  };

  const commit = async () => {
    setBusy(true);
    setMessage('');
    try {
      const result = await commitImport(type, preview.rows.map((row) => row.values));
      setMessage(`${result.importedRows} rows imported successfully.`);
      setPreview(null);
      setFile(null);
    } catch (error) {
      setMessage(error.response?.data?.message || 'Nothing was imported. Fix the errors and try again.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="p-4 sm:p-6 lg:p-8 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Import business data</h1>
        <p className="mt-1 text-sm text-slate-500">Preview every row first. BillNow commits the whole file or none of it.</p>
      </div>

      <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm space-y-5">
        <div className="grid gap-4 sm:grid-cols-2">
          <label className="text-sm font-medium text-slate-700">
            Data type
            <select value={type} onChange={(event) => { setType(event.target.value); setPreview(null); }}
              className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2">
              {TYPES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select>
          </label>
          <div>
            <span className="text-sm font-medium text-slate-700">Start with a template</span>
            <div className="mt-1 flex gap-2">
              <button onClick={() => downloadImportTemplate(type, 'xlsx')} className="inline-flex items-center gap-2 rounded-lg border px-3 py-2 text-sm"><Download size={16} /> Excel</button>
              <button onClick={() => downloadImportTemplate(type, 'csv')} className="inline-flex items-center gap-2 rounded-lg border px-3 py-2 text-sm"><Download size={16} /> CSV</button>
            </div>
          </div>
        </div>

        <label className="block rounded-lg border-2 border-dashed border-slate-300 p-6 text-center cursor-pointer hover:border-indigo-400">
          <Upload className="mx-auto mb-2 text-slate-400" />
          <span className="text-sm text-slate-700">{file ? file.name : 'Choose a .csv or .xlsx file'}</span>
          <input type="file" accept=".csv,.xlsx" className="hidden" onChange={(event) => { setFile(event.target.files?.[0] || null); setPreview(null); }} />
        </label>
        <button disabled={!file || busy} onClick={inspect} className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50">
          {busy ? 'Checking…' : 'Preview import'}
        </button>
      </section>

      {message && <div className="rounded-lg border border-indigo-200 bg-indigo-50 p-4 text-sm text-indigo-900">{message}</div>}

      {preview && (
        <section className="rounded-xl border border-slate-200 bg-white shadow-sm overflow-hidden">
          <div className="flex flex-wrap items-center justify-between gap-3 border-b p-4">
            <div className="flex gap-4 text-sm">
              <span className="inline-flex items-center gap-1 text-emerald-700"><CheckCircle2 size={16} /> {preview.validRows} valid</span>
              <span className="inline-flex items-center gap-1 text-red-700"><AlertCircle size={16} /> {preview.invalidRows} invalid</span>
            </div>
            <button disabled={busy || preview.invalidRows > 0} onClick={commit} className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50">
              Import {preview.validRows} rows
            </button>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-sm">
              <thead className="bg-slate-50"><tr><th className="px-3 py-2 text-left">Row</th>{preview.headers.map((header) => <th key={header} className="px-3 py-2 text-left whitespace-nowrap">{header}</th>)}<th className="px-3 py-2 text-left">Validation</th></tr></thead>
              <tbody className="divide-y">{preview.rows.map((row) => <tr key={row.rowNumber} className={row.errors.length ? 'bg-red-50' : ''}>
                <td className="px-3 py-2">{row.rowNumber}</td>
                {preview.headers.map((header) => <td key={header} className="px-3 py-2 whitespace-nowrap">{row.values[header] || '—'}</td>)}
                <td className="px-3 py-2 text-red-700 min-w-64">{row.errors.join('; ') || 'Ready'}</td>
              </tr>)}</tbody>
            </table>
          </div>
        </section>
      )}
    </div>
  );
}
