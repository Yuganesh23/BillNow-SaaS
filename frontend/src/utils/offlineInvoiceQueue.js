import { createInvoice } from '../api/invoiceApi';

const DATABASE = 'billnow-offline';
const STORE = 'invoice-queue';
const VERSION = 1;

function openDatabase() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DATABASE, VERSION);
    request.onupgradeneeded = () => {
      const database = request.result;
      if (!database.objectStoreNames.contains(STORE)) database.createObjectStore(STORE, { keyPath: 'temporaryReference' });
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function transaction(mode, action) {
  const database = await openDatabase();
  return new Promise((resolve, reject) => {
    const tx = database.transaction(STORE, mode);
    const store = tx.objectStore(STORE);
    const request = action(store);
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
    tx.oncomplete = () => database.close();
  });
}

export async function queueInvoice({ payload, branchId, userKey }) {
  const temporaryReference = `OFF-${Date.now()}-${crypto.randomUUID().slice(0, 8)}`;
  const record = {
    temporaryReference,
    idempotencyKey: crypto.randomUUID(),
    payload,
    branchId: String(branchId || ''),
    userKey,
    status: 'QUEUED',
    createdAt: new Date().toISOString(),
    error: '',
  };
  await transaction('readwrite', (store) => store.put(record));
  window.dispatchEvent(new Event('offline-queue:changed'));
  return record;
}

export async function listQueuedInvoices(userKey) {
  const rows = await transaction('readonly', (store) => store.getAll());
  return rows.filter((row) => row.userKey === userKey);
}

export async function syncQueuedInvoices(userKey) {
  if (!navigator.onLine) return { synced: 0, conflicts: 0 };
  const records = await listQueuedInvoices(userKey);
  const previousBranch = localStorage.getItem('activeBranchId');
  let synced = 0;
  let conflicts = 0;
  try {
    for (const record of records.filter((item) => item.status !== 'SYNCED')) {
      try {
        localStorage.setItem('activeBranchId', record.branchId);
        const invoice = await createInvoice(record.payload, record.idempotencyKey);
        await transaction('readwrite', (store) => store.put({ ...record, status: 'SYNCED', invoiceId: invoice.id, error: '' }));
        synced += 1;
      } catch (error) {
        const status = error.response?.status;
        const conflict = status === 409 || status === 422 || status === 400;
        await transaction('readwrite', (store) => store.put({
          ...record,
          status: conflict ? 'CONFLICT' : 'QUEUED',
          error: error.response?.data?.message || 'Sync failed; BillNow will retry when online.',
        }));
        if (conflict) conflicts += 1;
      }
    }
  } finally {
    if (previousBranch) localStorage.setItem('activeBranchId', previousBranch);
    else localStorage.removeItem('activeBranchId');
  }
  window.dispatchEvent(new Event('offline-queue:changed'));
  return { synced, conflicts };
}

export function canQueueInvoice(error) {
  return !navigator.onLine || !error.response;
}
