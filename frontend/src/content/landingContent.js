export const navigation = [
  {
    label: 'Features',
    items: [
      ['receipt', 'GST invoicing', 'Tax-ready invoices with HSN, discounts and PDF sharing.'],
      ['file', 'Quotes & estimates', 'Create quotations and convert approved work into invoices.'],
      ['badge', 'E-invoicing', 'Provider-ready IRN workflow for eligible paid plans.'],
      ['truck', 'E-way bills', 'Generate, update and cancel transport records.'],
      ['wallet', 'Payments', 'Record cash, UPI and card payments against every bill.'],
      ['boxes', 'Inventory', 'Track stock, movements and low-stock items in real time.'],
      ['users', 'Customers & suppliers', 'Keep parties, purchases and ledgers together.'],
      ['chart', 'Reports', 'See sales, tax, payments and stock insights.'],
      ['building', 'Multi-branch', 'Control branches, roles and billing limits centrally.'],
      ['phone', 'Mobile-ready PWA', 'Install from your browser and queue invoices offline.'],
    ],
  },
  {
    label: 'Solutions',
    items: [
      ['store', 'Retail counters', 'Fast product search, barcode scanning and compact printing.'],
      ['warehouse', 'Wholesale teams', 'Purchases, supplier ledgers and multi-branch visibility.'],
      ['briefcase', 'Service businesses', 'Quotes, customer history and professional invoices.'],
      ['users', 'Growing teams', 'Owner and biller roles with plan-based access limits.'],
    ],
  },
  {
    label: 'Resources',
    items: [
      ['book', 'GST workflow guide', 'Understand how XenBill handles tax and compliance records.'],
      ['download', 'Import templates', 'Bring products and customers in with validated CSV files.'],
      ['life', 'Help centre', 'Get setup and troubleshooting guidance from the team.'],
    ],
  },
];

export const trustSignals = [
  { value: 3, suffix: '', label: 'Languages in the app' },
  { value: 2, suffix: '', label: 'Thermal print formats' },
  { value: 100, suffix: '%', label: 'Backend-calculated tax totals' },
];

export const businessTypes = ['Retail', 'Wholesale', 'Services', 'Pharma', 'Restaurants', 'Manufacturing', 'Electronics', 'Freelancers'];

export const outcomes = [
  ['message', 'Stop chasing payment context', 'Keep invoice status, payment entries and WhatsApp delivery together, so the next follow-up starts with the full picture.'],
  ['shield', 'Make GST review calmer', 'Use backend-calculated CGST, SGST and IGST totals, then export the records your accountant needs.'],
  ['trend', 'Know stock and cash today', 'See sales, payment mix, low-stock items and supplier activity without rebuilding a spreadsheet.'],
];

export const featureTabs = [
  {
    id: 'invoicing', label: 'Invoicing', icon: 'receipt', eyebrow: 'GST billing',
    headline: 'Create a clear, tax-ready bill while the customer waits.',
    body: 'Search or scan products, add a customer, apply discounts and let XenBill calculate the tax split on the server.',
    bullets: ['HSN, GST, cess and inclusive-tax support', 'Professional PDF and 58/80mm printing', 'Idempotent invoice creation and offline queue'],
    link: 'Explore invoicing',
  },
  {
    id: 'gst', label: 'GST & e-invoicing', icon: 'badge', eyebrow: 'Compliance workflow',
    headline: 'Keep the invoice, IRN and e-way record connected.',
    body: 'Enable the provider workflow when your production GSP/IRP integration is configured and certified for your business.',
    bullets: ['CGST, SGST and IGST breakdowns', 'IRN generation and cancellation records', 'E-way bill generation, vehicle update and cancellation'],
    link: 'See compliance workflow',
  },
  {
    id: 'payments', label: 'Payments', icon: 'wallet', eyebrow: 'Collections',
    headline: 'Turn every payment into a traceable ledger entry.',
    body: 'Record cash, UPI, card or other payments and keep balances tied to the underlying invoice.',
    bullets: ['Payment status on each invoice', 'UPI QR on configured invoices', 'Razorpay-powered annual subscription checkout'],
    link: 'Explore payments',
  },
  {
    id: 'inventory', label: 'Inventory', icon: 'boxes', eyebrow: 'Live stock',
    headline: 'See what is selling before the shelf goes empty.',
    body: 'Inventory movements follow sales and purchases, with searchable products and low-stock attention points.',
    bullets: ['SKU, barcode and QR-assisted lookup', 'Purchase-linked stock updates', 'Low-stock and movement visibility'],
    link: 'Explore inventory',
  },
  {
    id: 'reports', label: 'Reports', icon: 'chart', eyebrow: 'Business signals',
    headline: 'Answer the daily questions without exporting everything first.',
    body: 'Review revenue, taxes, payment mix, customer value and inventory movement from one reporting workspace.',
    bullets: ['Sales, tax and payment summaries', 'Customer, product and stock analysis', 'CSV, Excel and Tally XML exports'],
    link: 'Explore reports',
  },
];

export const workflowSteps = [
  ['01', 'Add your business', 'Set the shop profile, GST details, invoice sequence, UPI ID and team access.'],
  ['02', 'Create the first invoice', 'Choose a customer, scan or search products, confirm tax and save the bill.'],
  ['03', 'Share and collect', 'Download the PDF, print it or send it through your configured WhatsApp Cloud account.'],
];

export const integrations = [
  ['UPI', 'Payment QR and payment-method tracking'],
  ['Razorpay', 'Secure subscription checkout'],
  ['WhatsApp', 'Invoice PDF delivery through Meta Cloud API'],
  ['Tally', 'Sales and purchase XML export'],
  ['CSV / Excel', 'Validated imports and business exports'],
  ['Neon Postgres', 'Managed production database'],
];

export const plans = [
  { name: 'Base', price: '4,999', note: 'Single shop', features: ['250 invoices per month', '2 active users', '1 branch', 'GST invoice PDFs', 'CSV and Excel imports'] },
  { name: 'Pro', price: '9,999', note: 'Growing retail team', featured: true, features: ['2,000 invoices per month', '10 active users', 'Up to 5 branches', 'E-invoice access', 'WhatsApp invoice delivery'] },
  { name: 'Enterprise', price: '24,999', note: 'Multi-branch operations', features: ['100,000 invoices per month', '100 active users', 'Up to 100 branches', 'E-invoice access', 'Advanced export workflows'] },
];

export const securityItems = [
  ['lock', 'Secure sessions', 'HttpOnly cookie authentication with configurable production security.'],
  ['layers', 'Tenant separation', 'Shop-scoped access rules protect each business workspace.'],
  ['database', 'Managed PostgreSQL', 'Production data runs on Neon with encrypted connections.'],
  ['activity', 'Operational visibility', 'Health checks, structured errors and request tracing aid support.'],
];

export const faqs = [
  ['Does XenBill calculate GST?', 'Yes. The backend calculates CGST and SGST for intra-state sales or IGST for inter-state sales, including supported discounts, cess and tax-inclusive prices. Your accountant should still review your business configuration and filings.'],
  ['Can I migrate from Tally or Excel?', 'You can import validated product and customer CSV files, preview errors before committing, and export sales or purchase data to Tally XML. Test Tally exports in a backup company first.'],
  ['Is my business data separated from other shops?', 'Yes. XenBill uses shop-scoped access across tenant data and role-based owner or biller access. Production security also depends on correctly configured hosting secrets and infrastructure.'],
  ['Does it work when the internet drops?', 'The installable web app can queue a new invoice draft in IndexedDB when a network request fails, then retry it safely when you reconnect. Full offline access to every module is not yet available.'],
  ['Can I cancel my subscription?', 'Subscription billing is handled through Razorpay. Cancellation and refund terms should be confirmed in your published billing policy before launch.'],
  ['Which languages are available?', 'The primary application shell, authentication, dashboard and invoice-entry actions currently support English, Hindi and Tamil. Some advanced screens remain English-first.'],
  ['Are e-invoices and e-way bills ready?', 'The workflows and provider adapter are built. They must be connected to an authorised GSP/IRP provider and complete sandbox certification before production use.'],
  ['Is there a free plan?', 'New shops receive a 7-day trial with a 25-invoice limit. Paid annual plans begin with Base; there is no permanent free plan in the current product.'],
];

export const footerGroups = [
  ['Product', [['Features', '#features'], ['Pricing', '#pricing'], ['Security', '#security'], ['Sign in', '/login']]],
  ['Solutions', [['Retail', '#industries'], ['Wholesale', '#industries'], ['Services', '#industries'], ['Multi-branch', '#features']]],
  ['Resources', [['GST guide', '#faq'], ['Import help', 'mailto:support@xenbill.app?subject=Import help'], ['Product demo', '#demo'], ['FAQ', '#faq']]],
  ['Company', [['About XenBill', '#story'], ['Contact', 'mailto:hello@xenbill.app'], ['Status', 'mailto:support@xenbill.app?subject=Service status']]],
  ['Support', [['Help centre', 'mailto:support@xenbill.app'], ['Email support', 'mailto:support@xenbill.app'], ['WhatsApp', 'mailto:support@xenbill.app?subject=WhatsApp support']]],
];

export const illustrativeScenarios = [
  ['RK', 'Retail counter', 'Chennai', 'Invoice, payment and stock movement stay connected after every sale.'],
  ['WS', 'Wholesale team', 'Coimbatore', 'Purchases, supplier balances and branch activity sit in one operating view.'],
  ['SV', 'Service studio', 'Bengaluru', 'A quote can move into a customer-ready GST invoice without re-entry.'],
];

export const requiredAssets = [
  'Self-hosted Plus Jakarta Sans and Inter WOFF2 font files (or approved system-font substitutions).',
  'Final XenBill logo pack: SVG wordmark, mono mark and dark-mode variants.',
  'Verified customer logo permissions and eight production SVG logos.',
  'Approved customer testimonials, names, portraits, cities and measurable results.',
  'Audited usage metrics for invoices, collections, customer count and locations.',
  'A hosted two-minute product video URL and poster image.',
  'Play Store and App Store badges only after native apps are published.',
  'A real support WhatsApp number to replace the placeholder footer/chat link.',
  'Published privacy, terms, refund, security and uptime pages.',
];
