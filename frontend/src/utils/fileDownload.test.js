import test from 'node:test';
import assert from 'node:assert/strict';
import { filenameFromDisposition } from './fileDownload.js';

test('extracts plain and UTF-8 attachment filenames safely', () => {
  assert.equal(filenameFromDisposition('attachment; filename="billnow-shop-export-2026-10-15.zip"', 'fallback.zip'), 'billnow-shop-export-2026-10-15.zip');
  assert.equal(filenameFromDisposition("attachment; filename*=UTF-8''shop%20export.json", 'fallback.json'), 'shop export.json');
  assert.equal(filenameFromDisposition('', 'fallback.zip'), 'fallback.zip');
  assert.equal(filenameFromDisposition("attachment; filename*=UTF-8''%E0%A4%A", 'fallback.json'), 'fallback.json');
});
