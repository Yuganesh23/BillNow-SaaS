import assert from 'node:assert/strict';
import test from 'node:test';
import { compactReportParams } from './reportParams.js';

test('report filters omit empty optional values but preserve zero and pagination', () => {
  assert.deepEqual(compactReportParams({
    partyId: '', taxRate: 0, documentType: undefined, page: 0, size: 25,
  }), { taxRate: 0, page: 0, size: 25 });
});
