import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowRight, BarChart3, Boxes, Building2, Check, ChevronDown, CircleCheck,
  FileDown, Menu, PackageCheck, QrCode, ReceiptText,
  ShieldCheck, Sparkles, Store, Truck, Users, WalletCards, X,
} from 'lucide-react';
import './LandingPage.css';

const featureCards = [
  { icon: ReceiptText, label: 'GST billing', title: 'Invoices that understand Indian tax.', copy: 'Add HSN codes, discounts and line items while BillNow handles CGST, SGST or IGST based on the sale.', accent: 'mint' },
  { icon: Boxes, label: 'Inventory', title: 'Know what is moving—and what is not.', copy: 'Search by SKU, scan a barcode or QR code, set low-stock thresholds and follow every inventory movement.', accent: 'amber' },
  { icon: Truck, label: 'Purchases', title: 'Suppliers and stock, in one ledger.', copy: 'Record purchases, capture payments and keep a clear supplier ledger without jumping between spreadsheets.', accent: 'blue' },
  { icon: BarChart3, label: 'Reports', title: 'Turn every bill into a business signal.', copy: 'See revenue, payment mix, top products, valuable customers and slow-moving stock from one reporting view.', accent: 'violet' },
];

const plans = [
  { name: 'Base', price: '499', description: 'For a single shop that needs reliable billing.', features: ['Unlimited invoices', 'Standard PDF generation', '1 admin user'] },
  { name: 'Pro', price: '999', description: 'For a growing retail team that wants more control.', features: ['Everything in Base', 'WhatsApp integration', 'Up to 3 staff accounts', 'Low-stock alerts'], featured: true },
  { name: 'Enterprise', price: '2,499', description: 'For wholesalers and multi-branch businesses.', features: ['Everything in Pro', 'Multi-branch support', 'Unlimited staff', 'GST Excel exports'] },
];

const faqs = [
  { question: 'Can I try BillNow before choosing a plan?', answer: 'Yes. New accounts start with a 7-day free trial, and registration does not require a credit card.' },
  { question: 'Does BillNow support GST invoices?', answer: 'Yes. Products can include HSN codes and GST rates, and invoices calculate CGST and SGST for intra-state sales or IGST for inter-state sales.' },
  { question: 'Can my staff create invoices?', answer: 'Yes. BillNow supports dedicated biller accounts. The Pro plan includes up to three staff accounts, while Enterprise supports unlimited staff.' },
  { question: 'How do customers receive their invoices?', answer: 'You can generate a professional PDF for download. On supported plans, invoices can also be sent through your configured WhatsApp integration.' },
];

const demoTabs = {
  billing: { eyebrow: 'Live invoice', title: 'INV-2048', icon: ReceiptText },
  inventory: { eyebrow: 'Inventory health', title: '126 products', icon: Boxes },
  insights: { eyebrow: 'Sales overview', title: 'This month', icon: BarChart3 },
};

function BrandMark() {
  return (
    <span className="landing-brand" aria-label="BillNow home">
      <span className="landing-brand-mark" aria-hidden="true"><span /><span /></span>
      <span>BillNow</span>
    </span>
  );
}

function ProductPreview({ activeDemo, setActiveDemo }) {
  const ActiveIcon = demoTabs[activeDemo].icon;
  return (
    <div className="product-preview-wrap" aria-label="Illustrative BillNow product preview">
      <div className="preview-glow" />
      <div className="product-preview">
        <div className="preview-rail">
          <BrandMark />
          <div className="preview-rail-links" aria-hidden="true">
            {[Store, ReceiptText, Boxes, Users, BarChart3].map((Icon, index) => <span className={index === 1 ? 'active' : ''} key={index}><Icon size={17} /></span>)}
          </div>
          <div className="preview-avatar">AK</div>
        </div>
        <div className="preview-main">
          <div className="preview-topbar">
            <div><span className="preview-kicker">Sample store</span><strong>Good morning, Arjun</strong></div>
            <span className="preview-branch"><Store size={14} /> Indiranagar</span>
          </div>
          <div className="preview-tabs" role="tablist" aria-label="Product preview views">
            {Object.entries(demoTabs).map(([key, item]) => (
              <button key={key} type="button" role="tab" aria-selected={activeDemo === key} className={activeDemo === key ? 'active' : ''} onClick={() => setActiveDemo(key)}>{item.eyebrow}</button>
            ))}
          </div>
          <div className="preview-content">
            <div className="preview-content-head">
              <span className="preview-content-icon"><ActiveIcon size={19} /></span>
              <div><span>{demoTabs[activeDemo].eyebrow}</span><strong>{demoTabs[activeDemo].title}</strong></div>
              <span className="preview-status"><CircleCheck size={13} /> Synced</span>
            </div>
            {activeDemo === 'billing' && (
              <div className="invoice-demo">
                <div className="invoice-customer"><div><span>Bill to</span><strong>Meera Stores</strong></div><div><span>Payment</span><strong>UPI · Paid</strong></div></div>
                <div className="invoice-table">
                  <div className="invoice-row invoice-header"><span>Item</span><span>Qty</span><span>GST</span><span>Amount</span></div>
                  <div className="invoice-row"><span>Arabica coffee</span><span>2</span><span>5%</span><span>₹840</span></div>
                  <div className="invoice-row"><span>Paper filters</span><span>3</span><span>12%</span><span>₹360</span></div>
                </div>
                <div className="invoice-total"><span>Total</span><strong>₹1,254</strong></div>
                <div className="preview-actions"><span><FileDown size={14} /> PDF</span><span><ArrowRight size={14} /> Send on WhatsApp</span></div>
              </div>
            )}
            {activeDemo === 'inventory' && (
              <div className="inventory-demo">
                <div className="inventory-summary"><strong>4</strong><span>items need attention</span></div>
                {[
                  ['Arabica coffee', '8 left', 38], ['Paper filters', '12 left', 58], ['Ceramic mug', '4 left', 22],
                ].map(([name, stock, width]) => (
                  <div className="stock-row" key={name}><PackageCheck size={17} /><div><strong>{name}</strong><span><i style={{ width: `${width}%` }} /></span></div><small>{stock}</small></div>
                ))}
                <div className="scan-pill"><QrCode size={15} /> Scan to add product</div>
              </div>
            )}
            {activeDemo === 'insights' && (
              <div className="insights-demo">
                <div className="insight-total"><span>Total sales</span><strong>₹1,84,620</strong><small>Illustrative data</small></div>
                <div className="mini-chart" aria-hidden="true">{[34, 52, 44, 68, 57, 78, 88, 72, 94, 82, 100, 91].map((height, index) => <i key={index} style={{ height: `${height}%` }} />)}</div>
                <div className="insight-split"><span><i className="upi" /> UPI <strong>52%</strong></span><span><i className="cash" /> Cash <strong>31%</strong></span><span><i className="card" /> Card <strong>17%</strong></span></div>
              </div>
            )}
          </div>
        </div>
      </div>
      <div className="floating-proof proof-one"><QrCode size={17} /><span><strong>Barcode ready</strong>Scan and bill</span></div>
      <div className="floating-proof proof-two"><CircleCheck size={17} /><span><strong>Payment captured</strong>Ledger updated</span></div>
    </div>
  );
}

export default function LandingPage() {
  const [menuOpen, setMenuOpen] = useState(false);
  const [activeDemo, setActiveDemo] = useState('billing');
  const [openFaq, setOpenFaq] = useState(0);
  const closeMenu = () => setMenuOpen(false);

  return (
    <div className="landing-page" id="top">
      <header className="landing-header">
        <nav className="landing-nav" aria-label="Primary navigation">
          <a href="#top" className="brand-link" onClick={closeMenu}><BrandMark /></a>
          <div className={`landing-nav-links ${menuOpen ? 'open' : ''}`}>
            <a href="#product" onClick={closeMenu}>Product</a><a href="#workflow" onClick={closeMenu}>How it works</a><a href="#pricing" onClick={closeMenu}>Pricing</a><a href="#faq" onClick={closeMenu}>FAQ</a>
            <div className="mobile-nav-actions"><Link to="/login">Log in</Link><Link to="/register" className="button button-primary">Start free</Link></div>
          </div>
          <div className="landing-nav-actions"><Link to="/login" className="login-link">Log in</Link><Link to="/register" className="button button-primary button-small">Start free</Link></div>
          <button type="button" className="menu-button" onClick={() => setMenuOpen((open) => !open)} aria-expanded={menuOpen} aria-label={menuOpen ? 'Close navigation' : 'Open navigation'}>{menuOpen ? <X /> : <Menu />}</button>
        </nav>
      </header>

      <main>
        <section className="hero-section">
          <div className="hero-grid landing-container">
            <div className="hero-copy">
              <div className="eyebrow-pill"><Sparkles size={14} /> Built for ambitious Indian businesses</div>
              <h1>Run the counter.<br /><em>Know the business.</em></h1>
              <p>BillNow brings GST billing, inventory, purchases, payments and business insights into one remarkably simple workspace.</p>
              <div className="hero-actions"><Link to="/register" className="button button-primary button-large">Start 7-day free trial <ArrowRight size={18} /></Link><a href="#product" className="button button-ghost button-large">Explore the product</a></div>
              <div className="hero-assurances"><span><Check size={15} /> No credit card</span><span><Check size={15} /> Setup in minutes</span><span><Check size={15} /> Works in your browser</span></div>
            </div>
            <ProductPreview activeDemo={activeDemo} setActiveDemo={setActiveDemo} />
          </div>
        </section>

        <section className="capability-strip" aria-label="Core capabilities"><div className="landing-container capability-strip-inner"><span>GST-ready invoices</span><i /><span>PDF &amp; WhatsApp</span><i /><span>Live stock tracking</span><i /><span>Multi-branch control</span></div></section>

        <section className="story-section" id="product">
          <div className="landing-container">
            <div className="section-heading split-heading"><div><span className="section-kicker">One connected workspace</span><h2>From stock in<br />to payment collected.</h2></div><p>BillNow keeps the whole sale connected. Every invoice updates the records behind it, so you spend less time reconciling and more time running the shop.</p></div>
            <div className="feature-grid">
              {featureCards.map(({ icon: Icon, label, title, copy, accent }) => (
                <article className={`feature-card feature-${accent}`} key={title}>
                  <div className="feature-card-top"><span className="feature-icon"><Icon size={21} /></span><span>{label}</span></div><h3>{title}</h3><p>{copy}</p>
                  <div className="feature-card-art" aria-hidden="true">
                    {accent === 'mint' && <><span className="art-line wide" /><span className="art-line" /><strong>₹ 1,254.00</strong></>}
                    {accent === 'amber' && <><span className="art-chip">LOW STOCK</span><div className="art-bars"><i /><i /><i /><i /></div></>}
                    {accent === 'blue' && <><span className="art-ledger"><i />Purchase recorded</span><span className="art-ledger"><i />Stock updated</span></>}
                    {accent === 'violet' && <div className="art-sparkline"><i /><i /><i /><i /><i /><i /></div>}
                  </div>
                </article>
              ))}
            </div>
            <div className="secondary-features">
              <div><FileDown /><strong>Professional PDF invoices</strong><span>Download clean, customer-ready bills.</span></div>
              <div><WalletCards /><strong>Payment tracking</strong><span>Capture cash, UPI and card payments.</span></div>
              <div><Users /><strong>Customer history</strong><span>Keep every purchase and payment in context.</span></div>
              <div><Building2 /><strong>Branches &amp; staff</strong><span>Grow from one counter to a complete team.</span></div>
            </div>
          </div>
        </section>

        <section className="workflow-section" id="workflow"><div className="landing-container workflow-grid">
          <div className="workflow-copy"><span className="section-kicker light">A calmer daily workflow</span><h2>One bill. Everything else falls into place.</h2><p>Built around the way a real counter works—from finding an item to sending the final invoice.</p><Link to="/register" className="text-link">Create your free account <ArrowRight size={17} /></Link></div>
          <ol className="workflow-steps">
            <li><span>01</span><div><strong>Pick or scan</strong><p>Find products by name or SKU, or open the QR scanner right from the invoice.</p></div><QrCode /></li>
            <li><span>02</span><div><strong>Bill with confidence</strong><p>Apply discounts, GST and a payment method while totals update automatically.</p></div><ReceiptText /></li>
            <li><span>03</span><div><strong>Share and stay in sync</strong><p>Download the PDF or send it on WhatsApp while inventory and reports stay current.</p></div><CircleCheck /></li>
          </ol>
        </div></section>

        <section className="pricing-section" id="pricing"><div className="landing-container">
          <div className="section-heading centered-heading"><span className="section-kicker">Straightforward pricing</span><h2>Start lean. Scale when you are ready.</h2><p>Every new account begins with a 7-day free trial. Choose a monthly plan when BillNow earns its place at your counter.</p></div>
          <div className="pricing-grid">{plans.map((plan) => (
            <article className={`price-card ${plan.featured ? 'featured' : ''}`} key={plan.name}>
              {plan.featured && <span className="popular-label">Most popular</span>}<div className="price-card-heading"><span>{plan.name}</span><p>{plan.description}</p></div><div className="price"><sup>₹</sup><strong>{plan.price}</strong><span>/ month</span></div><Link to="/register" className={`button ${plan.featured ? 'button-primary' : 'button-outline'}`}>Start free <ArrowRight size={16} /></Link><ul>{plan.features.map((feature) => <li key={feature}><span><Check size={14} /></span>{feature}</li>)}</ul>
            </article>
          ))}</div><p className="pricing-note"><ShieldCheck size={15} /> No credit card required to start your trial.</p>
        </div></section>

        <section className="faq-section" id="faq"><div className="landing-container faq-grid">
          <div className="faq-heading"><span className="section-kicker">Questions, answered</span><h2>Everything you need to get started.</h2><p>Still deciding? Create a free account and explore the full workflow with your own store.</p></div>
          <div className="faq-list">{faqs.map((faq, index) => (
            <article className={`faq-item ${openFaq === index ? 'open' : ''}`} key={faq.question}><button type="button" onClick={() => setOpenFaq(openFaq === index ? -1 : index)} aria-expanded={openFaq === index}><span>{faq.question}</span><ChevronDown size={20} /></button><div className="faq-answer"><p>{faq.answer}</p></div></article>
          ))}</div>
        </div></section>

        <section className="final-cta-section"><div className="landing-container final-cta"><div className="cta-orbit orbit-one" /><div className="cta-orbit orbit-two" /><span className="section-kicker light">Your next invoice can be better</span><h2>Less admin.<br />More business.</h2><p>Bring billing, stock and business visibility together with BillNow.</p><div className="final-actions"><Link to="/register" className="button button-light button-large">Start your free trial <ArrowRight size={18} /></Link><Link to="/login" className="cta-login">Already use BillNow? Log in</Link></div></div></section>
      </main>

      <footer className="landing-footer"><div className="landing-container footer-inner"><div><BrandMark /><p>Modern billing for Indian retail and wholesale.</p></div><div className="footer-links"><a href="#product">Product</a><a href="#pricing">Pricing</a><Link to="/login">Log in</Link></div><span>© {new Date().getFullYear()} BillNow</span></div></footer>
    </div>
  );
}
