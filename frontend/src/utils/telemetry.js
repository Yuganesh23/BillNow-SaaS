const endpoint = import.meta.env?.VITE_ERROR_REPORTING_URL;
const sensitive = /(authorization|cookie|password|token|secret|gstin|email|phone|whatsapp)["'=:\s]+[^\s,;}]*/gi;

export function scrubErrorText(value) {
  return String(value || '').replace(sensitive, '$1=[redacted]').slice(0, 4000);
}

export function reportFrontendError(error, context = {}) {
  if (!endpoint) return;
  const payload = {
    message: scrubErrorText(error?.message || error),
    stack: scrubErrorText(error?.stack),
    route: window.location.pathname,
    context: scrubErrorText(JSON.stringify(context)),
    occurredAt: new Date().toISOString(),
  };
  const body = JSON.stringify(payload);
  if (navigator.sendBeacon) navigator.sendBeacon(endpoint, new Blob([body], { type: 'application/json' }));
  else fetch(endpoint, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body, keepalive: true }).catch(() => {});
}
