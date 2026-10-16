import test from 'node:test';
import assert from 'node:assert/strict';
import { scrubErrorText } from './telemetry.js';

test('scrubs credentials and customer identifiers from telemetry', () => {
  const result = scrubErrorText('password: hunter2 email=test@example.com Authorization=Bearer-abc');
  assert.equal(result.includes('hunter2'), false);
  assert.equal(result.includes('test@example.com'), false);
  assert.equal(result.includes('Bearer-abc'), false);
});
