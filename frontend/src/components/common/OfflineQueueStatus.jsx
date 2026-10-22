import { useCallback, useEffect, useState } from 'react';
import { CloudOff, RefreshCw } from 'lucide-react';
import { listQueuedInvoices, syncQueuedInvoices } from '../../utils/offlineInvoiceQueue';
import { useAuth } from '../../context/AuthContext';

export function OfflineQueueStatus() {
  const { user } = useAuth();
  const userKey = user?.email || String(user?.id || '');
  const [items, setItems] = useState([]);
  const [syncing, setSyncing] = useState(false);

  const refresh = useCallback(() => {
    if (!userKey || !('indexedDB' in window)) return;
    listQueuedInvoices(userKey).then(setItems).catch(() => setItems([]));
  }, [userKey]);

  const sync = useCallback(async () => {
    if (!userKey || syncing) return;
    setSyncing(true);
    try { await syncQueuedInvoices(userKey); } finally { setSyncing(false); refresh(); }
  }, [refresh, syncing, userKey]);

  useEffect(() => {
    refresh();
    const handleOnline = () => sync();
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline-queue:changed', refresh);
    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline-queue:changed', refresh);
    };
  }, [refresh, sync]);

  const pending = items.filter((item) => item.status !== 'SYNCED');
  if (!pending.length) return null;
  const conflicts = pending.filter((item) => item.status === 'CONFLICT').length;
  return (
    <button type="button" onClick={sync} disabled={syncing || !navigator.onLine}
      className="mr-3 inline-flex items-center gap-2 rounded-full border border-amber-300 bg-amber-50 px-3 py-1.5 text-xs font-semibold text-amber-900 disabled:opacity-60"
      title={conflicts ? `${conflicts} queued invoices need review` : 'Sync queued invoices'}>
      {syncing ? <RefreshCw size={14} className="animate-spin" /> : <CloudOff size={14} />}
      {pending.length} queued{conflicts ? ` · ${conflicts} conflicts` : ''}
    </button>
  );
}
