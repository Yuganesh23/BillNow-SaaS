import test from 'node:test';
import assert from 'node:assert/strict';
import { legalReviewNotice, privacySections, termsSections } from './legalContent.js';

test('legal pages disclose launch decisions instead of inventing them', () => {
  assert.ok(privacySections.length >= 8);
  assert.ok(termsSections.length >= 8);
  assert.match(legalReviewNotice, /legal entity name/i);
  assert.match(legalReviewNotice, /refund policy/i);
  assert.match(privacySections.flat(2).join(' '), /deletion workflow/i);
  assert.match(termsSections.flat(2).join(' '), /liability cap/i);
});
