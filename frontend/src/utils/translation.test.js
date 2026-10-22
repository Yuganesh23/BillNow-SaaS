import test from 'node:test';
import assert from 'node:assert/strict';
import { resolveMessage } from './translation.js';

const catalog = { en: { invoice: 'Invoice', gst: 'GST' }, ta: { invoice: 'விலைப்பட்டியல்' } };

test('uses the selected locale and falls back to standard English GST terms', () => {
  assert.equal(resolveMessage(catalog, 'ta', 'invoice'), 'விலைப்பட்டியல்');
  assert.equal(resolveMessage(catalog, 'ta', 'gst'), 'GST');
  assert.equal(resolveMessage(catalog, 'hi', 'unknown'), 'unknown');
});
