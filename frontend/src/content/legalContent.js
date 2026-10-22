export const legalReviewNotice = 'These notices are a product draft and must be reviewed by qualified Indian counsel before public launch. The owner must add the legal entity name, registered address, grievance officer and final refund policy.';

export const privacySections = [
  ['Who operates XenBill', [
    'XenBill is the service name used by the operator of this billing application. The operator’s legal entity name, registered address and grievance officer details must be inserted here before launch.',
    'Privacy questions and data requests can be sent to legal@xenbill.app. Do not send passwords, OTPs, payment card details or database credentials by email.',
  ]],
  ['Information we process', [
    'Account and business information, including names, email addresses, phone numbers, shop details, GSTIN, addresses, tax settings and staff roles.',
    'Billing records entered by a shop, including customers, suppliers, products, invoices, purchases, payments, inventory movements and document notes.',
    'Service and security data, including authentication sessions, request identifiers, audit records, device and browser information, IP address, error reports and rate-limit events.',
    'Payment subscription status received from Razorpay. XenBill should not collect or store complete card or UPI credentials.',
  ]],
  ['Why we use information', [
    'To provide billing, inventory, reporting, customer-portal and account features; authenticate users; prevent abuse; calculate subscription limits; deliver requested messages; troubleshoot incidents; and meet applicable accounting, tax and legal obligations.',
    'Shop owners are responsible for having a lawful basis to enter and use their customers’ and suppliers’ information in XenBill.',
  ]],
  ['Hosting and service providers', [
    'The current deployment design uses Neon for PostgreSQL data, Railway for the API, Vercel for the web application, and Razorpay for subscription billing. Email, WhatsApp through Meta, object storage, error reporting and GST compliance providers are used only when configured by the operator or shop.',
    'These providers may process data in locations outside India. The operator must review each provider’s data-processing terms, retention controls and transfer arrangements before launch.',
  ]],
  ['Cookies and local storage', [
    'XenBill uses Secure, HttpOnly authentication cookies for sign-in and refresh sessions. The application also uses browser storage for preferences, active branch selection, offline invoice drafts and short-lived customer-portal access in the current tab.',
    'Do not change authentication cookies to cross-site SameSite=None without adding and reviewing explicit CSRF protection.',
  ]],
  ['Retention and deletion', [
    'Business data is retained while an account is active and as needed for security, dispute handling, backups and statutory recordkeeping. Indian tax and accounting records may need to be retained even after an account closes; the exact schedule requires advice from a CA and lawyer.',
    'A shop owner can export shop data from Settings. Until the in-product deletion workflow is completed, deletion or correction requests must be sent to legal@xenbill.app and handled through a verified support process.',
  ]],
  ['Security', [
    'XenBill uses tenant-scoped access, role checks, short-lived access tokens, revocable refresh tokens, encrypted provider secrets, private object storage options, request IDs and rate limiting. No online service can guarantee absolute security.',
    'If you believe an account or invoice link has been exposed, contact support immediately and revoke the affected access.',
  ]],
  ['Your choices and rights', [
    'Subject to applicable law and required record retention, users may ask for access, correction, export or deletion of personal data and may withdraw optional communication consent. Identity and authority over the relevant shop must be verified before a request is fulfilled.',
    'The operator must define the final grievance and appeal process after legal review of India’s Digital Personal Data Protection requirements and applicable rules.',
  ]],
  ['Children and changes', [
    'XenBill is a business service and is not intended for children. The service should not knowingly create accounts for people who cannot enter a binding business agreement.',
    'Material changes to this notice should be dated and communicated through the service or account email before they take effect.',
  ]],
];

export const termsSections = [
  ['Service and eligibility', [
    'XenBill provides software for invoices, purchases, inventory, payments, reports and related business workflows. It does not provide legal, tax, accounting or investment advice.',
    'A person creating or administering a shop confirms that they are authorised to act for that business and can enter a binding agreement under applicable law.',
  ]],
  ['Accounts and permitted use', [
    'You must provide accurate account information, protect sign-in credentials, assign staff the minimum required role and promptly report suspected misuse.',
    'You may not probe or bypass security, access another shop’s data, upload unlawful material, misuse communications, interfere with the service, resell access without permission or use XenBill to facilitate fraud.',
  ]],
  ['Billing and subscription', [
    'Current plans, limits, trial duration and prices are shown on the pricing page and billing screen. Subscription payments are processed by Razorpay. Taxes may apply.',
    'The operator must publish the final renewal, cancellation, refund, failed-payment and plan-downgrade policy before accepting public payments. Marketing text or a support message does not replace that policy.',
  ]],
  ['GST and business records', [
    'You are responsible for configuring tax registration, state, HSN/SAC, rates, place of supply and document details correctly, and for having a qualified professional review filings and business-specific obligations.',
    'E-invoice and e-way bill features require an authorised provider, applicable credentials and production certification. A generated report or export is not a representation that a filing has been accepted by a government portal.',
  ]],
  ['Your data and content', [
    'You retain responsibility for business data you submit. You grant the operator the limited permission needed to host, process, back up and transmit that data to provide the service and integrations you enable.',
    'Before closing an account, export the records you need. Some records may be retained where required for security, disputes, backups or statutory obligations, as described in the Privacy Notice.',
  ]],
  ['Third-party services', [
    'Neon, Railway, Vercel, Razorpay and optional email, Meta WhatsApp, storage, monitoring and GST providers operate under their own terms. Outages or policy changes at those providers may affect XenBill.',
    'You are responsible for obtaining customer consent for optional messages and for complying with provider messaging rules and applicable anti-spam requirements.',
  ]],
  ['Availability, suspension and support', [
    'The service may be changed, interrupted for maintenance or limited to protect security and reliability. The operator may suspend access for material misuse, non-payment or a credible threat to other users or systems.',
    'Support is available through support@xenbill.app. Any uptime commitment or service-level agreement must be stated in a separate written plan or agreement.',
  ]],
  ['Warranties and liability', [
    'The final warranty disclaimer, liability cap, indemnity, governing law, dispute process and termination provisions require review and approval by qualified counsel and the legal operator. They are intentionally not invented in this draft.',
    'Nothing in these terms is intended to exclude rights or liabilities that cannot lawfully be excluded.',
  ]],
  ['Changes and contact', [
    'Material changes should be dated and communicated before taking effect. Continued use after the effective date may constitute acceptance where permitted by law.',
    'Questions about these terms can be sent to legal@xenbill.app. The operator’s formal notice address must be added before launch.',
  ]],
];
