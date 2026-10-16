import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { AlertTriangle, ArrowLeft, Mail, ShieldCheck } from 'lucide-react';
import { BrandMark } from '../components/landing/LandingComponents';
import { legalReviewNotice, privacySections, termsSections } from '../content/legalContent';
import './LegalPage.css';

export default function LegalPage({ kind }) {
  const privacy = kind === 'privacy';
  const title = privacy ? 'Privacy Notice' : 'Terms of Service';
  const sections = privacy ? privacySections : termsSections;

  useEffect(() => {
    document.title = `${title} | XenBill`;
    const canonical = document.querySelector('link[rel="canonical"]');
    if (canonical) canonical.href = `https://www.xenbill.app/${kind}`;
    return () => {
      document.title = 'XenBill – GST Billing & Invoicing Software for Indian Businesses';
      if (canonical) canonical.href = 'https://www.xenbill.app/';
    };
  }, [kind, title]);

  return <div className="legal-page">
    <header className="legal-header"><div className="legal-shell"><Link to="/" aria-label="XenBill home"><BrandMark /></Link><Link to="/" className="legal-back"><ArrowLeft size={17} /> Back to XenBill</Link></div></header>
    <main className="legal-shell legal-main">
      <div className="legal-kicker"><ShieldCheck size={18} /> Trust and transparency</div>
      <h1>{title}</h1>
      <p className="legal-updated">Last updated 16 October 2026 · Draft for owner and legal review</p>
      <aside className="legal-review"><AlertTriangle size={22} /><div><strong>Not yet approved for launch</strong><p>{legalReviewNotice}</p></div></aside>
      <p className="legal-intro">This notice describes the current XenBill product and deployment design in plain language. Where a business or legal decision has not been made, it says so explicitly.</p>
      <nav className="legal-index" aria-label={`${title} sections`}>{sections.map(([heading], index) => <a key={heading} href={`#section-${index + 1}`}>{index + 1}. {heading}</a>)}</nav>
      <div className="legal-sections">{sections.map(([heading, paragraphs], index) => <section id={`section-${index + 1}`} key={heading}><span>{String(index + 1).padStart(2, '0')}</span><div><h2>{heading}</h2>{paragraphs.map(paragraph => <p key={paragraph}>{paragraph}</p>)}</div></section>)}</div>
      <aside className="legal-contact"><Mail size={22} /><div><strong>Questions or data requests</strong><p>Email <a href="mailto:legal@xenbill.app">legal@xenbill.app</a>. Identity and shop authority will be verified before account data is disclosed or changed.</p></div></aside>
    </main>
    <footer className="legal-footer"><div className="legal-shell"><span>© {new Date().getFullYear()} XenBill</span><nav><Link to="/privacy">Privacy</Link><Link to="/terms">Terms</Link><a href="mailto:support@xenbill.app">Support</a></nav></div></footer>
  </div>;
}
