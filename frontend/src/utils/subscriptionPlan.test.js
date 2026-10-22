import test from 'node:test';
import assert from 'node:assert/strict';
import { apiErrorMessage, planAction } from './subscriptionPlan.js';

test('plans go through checkout when there is no active subscription', () => {
  assert.equal(planAction(null, 'PRO').kind, 'checkout');
  assert.equal(planAction({ active: false, plan: 'PRO' }, 'PRO').kind, 'checkout');
});

test('active subscriptions upgrade, downgrade or show their current and pending plans', () => {
  const subscription = { active: true, plan: 'PRO', pendingPlan: null, cancelAtPeriodEnd: false };
  assert.deepEqual(planAction(subscription, 'PRO'), { kind: 'current', label: 'Current plan', disabled: true });
  assert.equal(planAction(subscription, 'ENTERPRISE').kind, 'upgrade');
  assert.equal(planAction(subscription, 'BASE').kind, 'downgrade');
  assert.equal(planAction({ ...subscription, pendingPlan: 'BASE' }, 'BASE').kind, 'pending');
});

test('a subscription ending at period end cannot change plan', () => {
  const action = planAction({ active: true, plan: 'BASE', cancelAtPeriodEnd: true }, 'PRO');
  assert.equal(action.kind, 'locked');
  assert.equal(action.disabled, true);
});

test('reads error messages from JSON and blob responses', async () => {
  assert.equal(await apiErrorMessage({ response: { data: { message: 'Nope' } } }, 'x'), 'Nope');
  const blob = new Blob([JSON.stringify({ message: 'Not configured' })], { type: 'application/json' });
  assert.equal(await apiErrorMessage({ response: { data: blob } }, 'x'), 'Not configured');
  assert.equal(await apiErrorMessage({}, 'fallback'), 'fallback');
});
