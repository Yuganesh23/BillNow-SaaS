import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Activity, ArrowRight, BadgeIndianRupee, BarChart3, BookOpen, Boxes, BriefcaseBusiness,
  Building2, Check, ChevronDown, CircleCheck, Database, Download, FileText, Headphones,
  Languages, Layers3, LifeBuoy, LockKeyhole, Menu, MessageCircle, Moon, PackageCheck,
  Phone, Play, QrCode, ReceiptText, ShieldCheck, ShoppingBag, Sparkles, Store, Sun,
  TrendingUp, Truck, Users, WalletCards, Warehouse, X,
} from 'lucide-react';
import { LanguageSelector } from '../../context/LocaleContext';
import {
  businessTypes, faqs, featureTabs, footerGroups, illustrativeScenarios, integrations,
  navigation, outcomes, plans, securityItems, trustSignals, workflowSteps,
} from '../../content/landingContent';

const icons = {
  activity: Activity, badge: BadgeIndianRupee, book: BookOpen, boxes: Boxes,
  briefcase: BriefcaseBusiness, building: Building2, chart: BarChart3, database: Database,
  download: Download, file: FileText, layers: Layers3, life: LifeBuoy, lock: LockKeyhole,
  message: MessageCircle, phone: Phone, receipt: ReceiptText, shield: ShieldCheck,
  store: Store, trend: TrendingUp, truck: Truck, users: Users, wallet: WalletCards,
  warehouse: Warehouse,
};

function Icon({ name, size = 20 }) {
  const Component = icons[name] || Sparkles;
  return <Component size={size} aria-hidden="true" />;
}

export function BrandMark() {
  return <span className="xen-brand" aria-label="XenBill home"><span className="xen-brand-mark" aria-hidden="true"><i /><i /><i /></span><span>XenBill</span></span>;
}

function MegaMenu({ group }) {
  return <div className={`xen-mega xen-mega-${group.label.toLowerCase()}`}>
    <p>{group.label === 'Features' ? 'A connected billing workspace' : group.label === 'Solutions' ? 'Built around your way of working' : 'Guides and support'}</p>
    <div>{group.items.map(([icon, title, description]) => <a key={title} href={title === 'Help centre' ? 'mailto:support@xenbill.app' : '#features'}><span><Icon name={icon} /></span><span><strong>{title}</strong><small>{description}</small></span></a>)}</div>
  </div>;
}

export function Header({ theme, onThemeChange }) {
  const [menuOpen, setMenuOpen] = useState(false);
  const [mobileGroup, setMobileGroup] = useState('');
  const [scrolled, setScrolled] = useState(false);
  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > Math.min(window.innerHeight * .7, 620));
    onScroll(); window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);
  return <header className={`xen-header ${scrolled ? 'is-scrolled' : ''}`}>
    <nav className="xen-nav xen-container" aria-label="Primary navigation">
      <a href="#top" className="xen-brand-link" onClick={() => setMenuOpen(false)}><BrandMark /></a>
      <div className="xen-desktop-nav">
        {navigation.map((group) => <div className="xen-nav-group" key={group.label}><button type="button" aria-haspopup="true">{group.label}<ChevronDown size={14} /></button><MegaMenu group={group} /></div>)}
        <a href="#pricing">Pricing</a>
      </div>
      <div className="xen-nav-actions">
        <button className="xen-icon-button" type="button" onClick={onThemeChange} aria-label={`Switch to ${theme === 'light' ? 'dark' : 'light'} mode`}>{theme === 'light' ? <Moon size={18} /> : <Sun size={18} />}</button>
        <Link to="/login" className="xen-signin">Sign in</Link>
        <Link to="/signup" className={`xen-button xen-button-primary xen-header-cta ${scrolled ? 'prominent' : ''}`} data-analytics="header_cta_click">Start free</Link>
      </div>
      <button className="xen-mobile-toggle" type="button" onClick={() => setMenuOpen(!menuOpen)} aria-expanded={menuOpen} aria-controls="mobile-navigation" aria-label={menuOpen ? 'Close navigation' : 'Open navigation'}>{menuOpen ? <X /> : <Menu />}</button>
    </nav>
    <div id="mobile-navigation" className={`xen-mobile-nav ${menuOpen ? 'open' : ''}`}>
      {navigation.map((group) => <div className="xen-mobile-group" key={group.label}><button type="button" onClick={() => setMobileGroup(mobileGroup === group.label ? '' : group.label)} aria-expanded={mobileGroup === group.label}>{group.label}<ChevronDown size={16} /></button>{mobileGroup === group.label && <div>{group.items.map(([icon, title]) => <a href="#features" key={title} onClick={() => setMenuOpen(false)}><Icon name={icon} size={17} />{title}</a>)}</div>}</div>)}
      <a href="#pricing" onClick={() => setMenuOpen(false)}>Pricing</a>
      <div className="xen-mobile-actions"><Link to="/login">Sign in</Link><Link to="/signup" className="xen-button xen-button-primary">Start free</Link></div>
    </div>
  </header>;
}

function InvoicePanel() {
  return <div className="xen-ui-card xen-invoice-card"><div className="xen-ui-card-head"><span><ReceiptText size={17} />GST invoice</span><b>INV-1042</b></div><div className="xen-invoice-parties"><span><small>FROM</small><strong>Ananya Home Stores</strong><em>GSTIN 33ABCDE1234F1Z5</em></span><span><small>BILL TO</small><strong>Kaveri Retail</strong><em>Chennai, Tamil Nadu</em></span></div><div className="xen-invoice-lines"><div><b>Item</b><b>Tax</b><b>Amount</b></div><div><span>Copper bottle × 10</span><span>18%</span><strong>₹21,000</strong></div><div><span>Delivery</span><span>18%</span><strong>₹1,500</strong></div></div><div className="xen-tax-lines"><span>CGST <b>₹1,716</b></span><span>SGST <b>₹1,716</b></span><strong>Total <b>₹24,500</b></strong></div></div>;
}

function ChartPanel() {
  return <div className="xen-ui-card xen-chart-card"><div className="xen-ui-card-head"><span><BarChart3 size={17} />Sales overview</span><small>Oct 2026</small></div><strong className="xen-big-number">₹4,82,640</strong><small>Illustrative dashboard data</small><div className="xen-bars" aria-hidden="true">{[38, 52, 44, 71, 60, 82, 68, 91, 78, 100, 86, 96].map((height, index) => <i key={index} style={{ height: `${height}%` }} />)}</div><div className="xen-chart-key"><span><i className="paid" />Paid ₹3.74L</span><span><i className="due" />Due ₹1.08L</span></div></div>;
}

function BrowserMockup({ compact = false, view = 'hero' }) {
  return <div className={`xen-browser ${compact ? 'compact' : ''}`} aria-label="Illustrative XenBill product interface">
    <div className="xen-browser-chrome"><span><i /><i /><i /></span><em>xenbill.app/dashboard</em><ShieldCheck size={14} /></div>
    <div className="xen-app-frame"><aside><BrandMark />{[BarChart3, ReceiptText, Boxes, Users, Truck].map((Item, index) => <span className={index === (view === 'inventory' ? 2 : 0) ? 'active' : ''} key={index}><Item size={17} /></span>)}</aside><main><div className="xen-app-top"><span><small>Sample business</small><strong>Good morning, Ananya</strong></span><span className="xen-branch-pill"><Store size={13} /> Chennai</span></div>{view === 'inventory' ? <InventoryPanel /> : view === 'payments' ? <PaymentsPanel /> : view === 'gst' ? <CompliancePanel /> : view === 'invoicing' ? <InvoicePanel /> : <div className="xen-dashboard-grid"><ChartPanel /><InvoicePanel /></div>}</main></div>
  </div>;
}

function InventoryPanel() {
  return <div className="xen-ui-card xen-wide-panel"><div className="xen-ui-card-head"><span><Boxes size={17} />Inventory health</span><b>126 products</b></div>{[['Copper bottle', '8 left', 24], ['Thermal roll', '46 left', 72], ['Paper carry bag', '18 left', 48]].map(([name, stock, width]) => <div className="xen-stock-line" key={name}><PackageCheck size={18} /><span><strong>{name}</strong><i><em style={{ width: `${width}%` }} /></i></span><b>{stock}</b></div>)}</div>;
}

function PaymentsPanel() {
  return <div className="xen-ui-card xen-wide-panel"><div className="xen-ui-card-head"><span><WalletCards size={17} />Collections</span><b>Today</b></div><div className="xen-payment-total"><span><small>COLLECTED</small><strong>₹48,320</strong></span><CircleCheck /></div>{[['INV-1042', 'Kaveri Retail', '₹24,500', 'Paid'], ['INV-1041', 'Mohan & Co.', '₹12,800', 'Part paid'], ['INV-1040', 'A1 Services', '₹11,020', 'Paid']].map((row) => <div className="xen-payment-line" key={row[0]}><b>{row[0]}</b><span>{row[1]}</span><strong>{row[2]}</strong><em>{row[3]}</em></div>)}</div>;
}

function CompliancePanel() {
  return <div className="xen-ui-card xen-wide-panel"><div className="xen-ui-card-head"><span><ShieldCheck size={17} />Compliance record</span><b>INV-1042</b></div><div className="xen-compliance-flow"><span className="done"><CircleCheck />Invoice saved<small>Tax calculated</small></span><i /><span className="done"><CircleCheck />IRN generated<small>Ack 182610512345</small></span><i /><span><Truck />E-way ready<small>Awaiting transport</small></span></div></div>;
}

export function Hero({ onDemoOpen }) {
  return <section className="xen-hero" id="story"><div className="xen-container xen-hero-grid"><div className="xen-hero-copy" data-reveal><span className="xen-kicker"><Sparkles size={14} /> Billing, stock and payments in one place</span><h1>GST billing that <em>runs itself.</em></h1><p>Create invoices, quotations and tax records in seconds, then share a PDF on WhatsApp and keep stock, payments and reports in sync.</p><div className="xen-hero-actions"><Link to="/signup" className="xen-button xen-button-accent xen-button-large" data-analytics="hero_cta_click">Start 7-day free trial <ArrowRight size={18} /></Link><button type="button" className="xen-button xen-button-quiet xen-button-large" onClick={onDemoOpen} data-analytics="demo_open"><Play size={17} fill="currentColor" /> Watch 2-min demo</button></div><div className="xen-hero-proof"><span><Check />No credit card</span><span><Check />25 trial invoices</span><span><Check />Setup in minutes</span></div><p className="xen-honesty-note"><ShieldCheck size={15} /> GST-ready calculations. Production e-invoicing requires your authorised provider setup.</p></div><div className="xen-hero-visual" data-reveal><div className="xen-orbit orbit-a" /><div className="xen-orbit orbit-b" /><BrowserMockup /><div className="xen-float-chip xen-paid-chip"><CircleCheck /><span><strong>Invoice #1042 paid</strong>₹24,500</span></div><div className="xen-float-chip xen-gst-chip"><ShieldCheck /><span><strong>Tax summary</strong>Ready to review</span></div></div></div></section>;
}

function Counter({ value, suffix }) {
  const [display, setDisplay] = useState(0); const ref = useRef(null);
  useEffect(() => { const node = ref.current; if (!node) return undefined; const observer = new IntersectionObserver(([entry]) => { if (!entry.isIntersecting) return; const started = performance.now(); const tick = (now) => { const progress = Math.min((now - started) / 850, 1); setDisplay(Math.round(value * (1 - ((1 - progress) ** 3)))); if (progress < 1) requestAnimationFrame(tick); }; requestAnimationFrame(tick); observer.disconnect(); }, { threshold: .5 }); observer.observe(node); return () => observer.disconnect(); }, [value]);
  return <strong ref={ref}>{display}{suffix}</strong>;
}

export function TrustBar() {
  return <section className="xen-trust"><div className="xen-container"><p>Designed for the businesses that keep India moving</p><div className="xen-industry-marquee">{businessTypes.map((item) => <span key={item}><Store size={15} />{item}</span>)}</div><div className="xen-trust-counters">{trustSignals.map((item) => <div key={item.label}><Counter value={item.value} suffix={item.suffix} /><span>{item.label}</span></div>)}</div></div></section>;
}

export function Outcomes() {
  return <section className="xen-section xen-outcomes"><div className="xen-container"><div className="xen-section-heading" data-reveal><span className="xen-kicker">Less chasing. More control.</span><h2>Your billing software should close loops, not create new ones.</h2><p>XenBill connects the small actions at the counter to the numbers you need at the end of the day.</p></div><div className="xen-outcome-grid">{outcomes.map(([icon, title, copy], index) => <article key={title} data-reveal><span className="xen-card-number">0{index + 1}</span><span className="xen-feature-icon"><Icon name={icon} /></span><h3>{title}</h3><p>{copy}</p></article>)}</div></div></section>;
}

export function FeatureTabs() {
  const [active, setActive] = useState(0); const item = featureTabs[active];
  const handleKey = (event) => { if (!['ArrowRight', 'ArrowLeft'].includes(event.key)) return; event.preventDefault(); setActive((current) => (current + (event.key === 'ArrowRight' ? 1 : featureTabs.length - 1)) % featureTabs.length); };
  return <section className="xen-section xen-features" id="features"><div className="xen-container"><div className="xen-section-heading split" data-reveal><div><span className="xen-kicker">One connected system</span><h2>Every bill updates the business behind it.</h2></div><p>Move from invoice to inventory to insight without stitching together separate tools.</p></div><div className="xen-tab-list" role="tablist" aria-label="Product capabilities" onKeyDown={handleKey}>{featureTabs.map((tab, index) => <button id={`tab-${tab.id}`} aria-controls={`panel-${tab.id}`} role="tab" aria-selected={active === index} tabIndex={active === index ? 0 : -1} className={active === index ? 'active' : ''} onClick={() => setActive(index)} key={tab.id}><Icon name={tab.icon} size={17} />{tab.label}</button>)}</div><div className="xen-feature-stage" id={`panel-${item.id}`} role="tabpanel" aria-labelledby={`tab-${item.id}`} key={item.id}><div className="xen-feature-copy"><span>{item.eyebrow}</span><h3>{item.headline}</h3><p>{item.body}</p><ul>{item.bullets.map((bullet) => <li key={bullet}><CircleCheck size={17} />{bullet}</li>)}</ul><a href="#signup">{item.link} <ArrowRight size={16} /></a></div><div className="xen-feature-visual"><BrowserMockup compact view={item.id} /></div></div></div></section>;
}

export function HowItWorks() {
  return <section className="xen-section xen-how" id="demo"><div className="xen-container"><div className="xen-section-heading centered" data-reveal><span className="xen-kicker">From setup to paid</span><h2>Your first clean billing workflow in three steps.</h2></div><ol className="xen-step-list">{workflowSteps.map(([number, title, copy], index) => <li key={number} data-reveal><span className="xen-step-number">{number}</span><div><h3>{title}</h3><p>{copy}</p></div>{index < workflowSteps.length - 1 && <i aria-hidden="true" />}</li>)}</ol></div></section>;
}

export function Industries() {
  const industryIcons = [Store, Warehouse, BriefcaseBusiness, ShieldCheck, ShoppingBag, Building2, Phone, Users];
  return <section className="xen-section xen-industries" id="industries"><div className="xen-container"><div className="xen-section-heading split" data-reveal><div><span className="xen-kicker">Made for practical work</span><h2>Flexible enough for the counter, office or warehouse.</h2></div><p>Start with the workflow you use today, then add branches, staff and reporting as the business grows.</p></div><div className="xen-industry-grid">{businessTypes.map((name, index) => { const IndustryIcon = industryIcons[index]; return <a href="#signup" key={name} data-reveal><IndustryIcon /><span><strong>{name}</strong><small>See the workflow <ArrowRight size={13} /></small></span></a>; })}</div></div></section>;
}

export function Integrations() {
  return <section className="xen-section xen-integrations"><div className="xen-container"><div className="xen-section-heading centered" data-reveal><span className="xen-kicker">Connected where it matters</span><h2>Connect the tools that complete the workflow.</h2><p>Every connection shown here is already represented in the product or deployment stack.</p></div><div className="xen-logo-wall">{integrations.map(([name, copy]) => <div key={name} data-reveal><span>{name.slice(0, 2).toUpperCase()}</span><strong>{name}</strong><small>{copy}</small></div>)}</div></div></section>;
}

export function MobileSection() {
  return <section className="xen-section xen-mobile-section"><div className="xen-container xen-mobile-grid"><div className="xen-phone-wrap" data-reveal><div className="xen-phone"><div className="xen-phone-notch" /><div className="xen-phone-screen"><div className="xen-phone-head"><BrandMark /><span>•••</span></div><span className="xen-phone-welcome">Good morning, Ananya</span><strong className="xen-phone-total">₹48,320</strong><small>Collected today</small><button type="button"><ReceiptText />Create invoice</button><div className="xen-phone-stat"><span><Boxes />Low stock</span><b>4 items</b></div><div className="xen-phone-stat"><span><CircleCheck />Queued invoices</span><b>All synced</b></div></div></div></div><div className="xen-mobile-copy" data-reveal><span className="xen-kicker">Installable web app</span><h2>Bill from the counter or keep moving.</h2><p>XenBill works responsively in the browser and can be installed as a Progressive Web App. If a connection drops during invoice creation, the draft can queue safely on the device.</p><ul><li><Check />Works on mobile, tablet and desktop</li><li><Check />English, Hindi and Tamil primary flows</li><li><Check />Offline invoice queue with safe retry</li></ul><div className="xen-install-row"><div className="xen-qr-placeholder" aria-label="QR placeholder for the production app URL"><QrCode /></div><span><strong>Open xenbill.app</strong><small>Use “Add to Home Screen” in your browser. Native app-store releases are not currently published.</small></span></div></div></div></section>;
}

export function Scenarios() {
  return <section className="xen-section xen-scenarios"><div className="xen-container"><div className="xen-section-heading centered" data-reveal><span className="xen-kicker">Illustrative workflows</span><h2>See how the pieces fit for different teams.</h2><p>These are product scenarios, not customer endorsements. Replace them with approved customer stories before publishing social proof.</p></div><div className="xen-scenario-grid">{illustrativeScenarios.map(([initials, business, city, copy]) => <article key={business} data-reveal><div className="xen-avatar">{initials}</div><div><h3>{business}</h3><span>{city}</span></div><p>“{copy}”</p><small>Illustrative scenario</small></article>)}</div></div></section>;
}

export function Pricing() {
  const [billing, setBilling] = useState('annual');
  return <section className="xen-section xen-pricing" id="pricing"><div className="xen-container"><div className="xen-section-heading centered" data-reveal><span className="xen-kicker">Clear annual plans</span><h2>Choose the operating room your business needs.</h2><p>Every new shop starts with a 7-day trial and a 25-invoice allowance. No permanent free tier is advertised.</p><div className="xen-price-toggle" data-analytics="pricing_toggle"><button type="button" className={billing === 'monthly' ? 'active' : ''} onClick={() => setBilling('monthly')}>Monthly equivalent</button><button type="button" className={billing === 'annual' ? 'active' : ''} onClick={() => setBilling('annual')}>Annual billing <span>Best value</span></button></div></div><div className="xen-price-grid">{plans.map((plan) => { const annual = Number(plan.price.replace(',', '')); const monthly = Math.round(annual / 12); return <article className={plan.featured ? 'featured' : ''} key={plan.name} data-reveal>{plan.featured && <span className="xen-popular">Most popular</span>}<div className="xen-plan-head"><span>{plan.name}</span><small>{plan.note}</small></div><div className="xen-plan-price"><sup>₹</sup><strong>{billing === 'annual' ? plan.price : monthly.toLocaleString('en-IN')}</strong><span>/{billing === 'annual' ? 'year' : 'month'}{billing === 'monthly' ? '*' : ''}</span></div>{billing === 'monthly' && <p>*Equivalent shown for comparison; checkout is annual.</p>}<Link to="/signup" className={`xen-button ${plan.featured ? 'xen-button-accent' : 'xen-button-outline'}`} data-analytics={`pricing_${plan.name.toLowerCase()}_click`}>Start trial <ArrowRight size={16} /></Link><ul>{plan.features.map((feature) => <li key={feature}><Check />{feature}</li>)}</ul></article>; })}</div><a className="xen-compare-link" href="#features">Compare product capabilities <ArrowRight size={16} /></a></div></section>;
}

export function Security() {
  return <section className="xen-section xen-security" id="security"><div className="xen-container xen-security-grid"><div className="xen-security-copy" data-reveal><span className="xen-kicker">Security by design</span><h2>Your shop should never see another shop’s records.</h2><p>Tenant isolation and correct money calculations are product requirements, not marketing add-ons. Infrastructure claims still need independent verification before launch.</p></div><div className="xen-security-list">{securityItems.map(([icon, title, copy]) => <article key={title} data-reveal><span><Icon name={icon} /></span><div><h3>{title}</h3><p>{copy}</p></div></article>)}</div></div></section>;
}

export function FAQ() {
  const [open, setOpen] = useState(0);
  return <section className="xen-section xen-faq" id="faq"><div className="xen-container xen-faq-grid"><div className="xen-faq-copy" data-reveal><span className="xen-kicker">Straight answers</span><h2>Know what is ready before you depend on it.</h2><p>We have separated production capabilities from items that still require provider setup or policy decisions.</p><a href="mailto:support@xenbill.app">Ask a different question <ArrowRight size={16} /></a></div><div className="xen-faq-list">{faqs.map(([question, answer], index) => <article className={open === index ? 'open' : ''} key={question}><button type="button" aria-expanded={open === index} onClick={() => setOpen(open === index ? -1 : index)}><span>{question}</span><ChevronDown /></button><div><p>{answer}</p></div></article>)}</div></div></section>;
}

export function FinalCTA() {
  return <section className="xen-final" id="signup"><div className="xen-container"><div className="xen-final-card" data-reveal><div className="xen-final-grid" aria-hidden="true" /><span className="xen-kicker">A cleaner close to every sale</span><h2>Send your first GST invoice in minutes.</h2><p>Start with a 7-day trial, import your products and keep the entire billing trail in one workspace.</p><div><Link to="/signup" className="xen-button xen-button-accent xen-button-large" data-analytics="final_cta_click">Start free trial <ArrowRight /></Link><span>No credit card · 25 trial invoices</span></div></div></div></section>;
}

export function Footer() {
  return <footer className="xen-footer"><div className="xen-container"><div className="xen-footer-top"><div className="xen-footer-brand"><BrandMark /><p>GST billing, inventory, purchases and reporting for Indian businesses.</p><a href="mailto:hello@xenbill.app">hello@xenbill.app</a></div>{footerGroups.map(([group, links]) => <div className="xen-footer-group" key={group}><strong>{group}</strong>{links.map(([label, href]) => href.startsWith('/') ? <Link to={href} key={label}>{label}</Link> : <a href={href} key={label}>{label}</a>)}</div>)}</div><div className="xen-footer-bottom"><LanguageSelector className="xen-language" /><div className="xen-socials" aria-label="Social links"><a href="mailto:hello@xenbill.app" aria-label="Email XenBill"><MessageCircle /></a><a href="mailto:support@xenbill.app" aria-label="XenBill support"><Headphones /></a><span><Languages />India</span></div><div><a href="mailto:legal@xenbill.app?subject=Privacy policy">Privacy</a><a href="mailto:legal@xenbill.app?subject=Terms of service">Terms</a><span>© {new Date().getFullYear()} XenBill</span></div></div></div></footer>;
}

export function DemoModal({ open, onClose }) {
  const closeRef = useRef(null);
  useEffect(() => { if (!open) return undefined; const prior = document.activeElement; const onKey = (event) => { if (event.key === 'Escape') onClose(); }; document.body.style.overflow = 'hidden'; document.addEventListener('keydown', onKey); requestAnimationFrame(() => closeRef.current?.focus()); return () => { document.body.style.overflow = ''; document.removeEventListener('keydown', onKey); prior?.focus?.(); }; }, [open, onClose]);
  if (!open) return null;
  return <div className="xen-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}><div className="xen-demo-modal" role="dialog" aria-modal="true" aria-labelledby="demo-title"><button ref={closeRef} type="button" className="xen-modal-close" onClick={onClose} aria-label="Close demo"><X /></button><div className="xen-demo-copy"><span className="xen-kicker">Two-minute product tour</span><h2 id="demo-title">From product scan to paid invoice.</h2><p>This interactive preview uses illustrative data. Add a verified product-video URL before launch if you want the button to play recorded footage.</p></div><div className="xen-demo-story"><BrowserMockup compact /><ol><li><span>1</span><p><strong>Set the business</strong>Add GST and invoice details once.</p></li><li><span>2</span><p><strong>Create the bill</strong>Search, scan and let the backend calculate.</p></li><li><span>3</span><p><strong>Share and track</strong>PDF, print or WhatsApp the invoice.</p></li></ol></div><Link to="/signup" className="xen-button xen-button-accent">Try it with your business <ArrowRight /></Link></div></div>;
}

export function WhatsAppButton() {
  return <a className="xen-whatsapp" href="mailto:support@xenbill.app?subject=WhatsApp support number" aria-label="Ask XenBill support on WhatsApp"><MessageCircle /><span>Chat with us</span></a>;
}
