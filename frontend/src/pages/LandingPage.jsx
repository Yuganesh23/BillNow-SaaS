import { useCallback, useEffect, useState } from 'react';
import {
  DemoModal, FAQ, FeatureTabs, FinalCTA, Footer, Header, Hero, HowItWorks,
  Industries, Integrations, MobileSection, Outcomes, Pricing, Scenarios, Security,
  TrustBar, WhatsAppButton,
} from '../components/landing/LandingComponents';
import { faqs } from '../content/landingContent';
import './LandingPage.css';

const structuredData = [
  {
    '@context': 'https://schema.org', '@type': 'SoftwareApplication', name: 'XenBill',
    applicationCategory: 'BusinessApplication', operatingSystem: 'Web', url: 'https://www.xenbill.app/',
    description: 'GST billing, inventory, purchases, payments and reporting software for Indian businesses.',
    offers: { '@type': 'Offer', priceCurrency: 'INR', price: '4999', category: 'annual subscription' },
  },
  {
    '@context': 'https://schema.org', '@type': 'Organization', name: 'XenBill',
    url: 'https://www.xenbill.app/', email: 'hello@xenbill.app',
  },
  {
    '@context': 'https://schema.org', '@type': 'FAQPage',
    mainEntity: faqs.map(([question, answer]) => ({
      '@type': 'Question', name: question,
      acceptedAnswer: { '@type': 'Answer', text: answer },
    })),
  },
];

export default function LandingPage() {
  const [demoOpen, setDemoOpen] = useState(false);
  const [theme, setTheme] = useState(() => localStorage.getItem('xenbillLandingTheme') || 'light');
  const closeDemo = useCallback(() => setDemoOpen(false), []);

  useEffect(() => { localStorage.setItem('xenbillLandingTheme', theme); }, [theme]);

  useEffect(() => {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-visible');
          observer.unobserve(entry.target);
        }
      });
    }, { threshold: .12, rootMargin: '0px 0px -24px' });
    const nodes = document.querySelectorAll('[data-reveal]');
    nodes.forEach((node) => observer.observe(node));
    return () => observer.disconnect();
  }, []);

  return <div className="xen-landing" data-theme={theme} id="top">
    <a href="#main-content" className="xen-skip-link">Skip to content</a>
    <Header theme={theme} onThemeChange={() => setTheme((current) => current === 'light' ? 'dark' : 'light')} />
    <main id="main-content">
      <Hero onDemoOpen={() => setDemoOpen(true)} />
      <TrustBar /><Outcomes /><FeatureTabs /><HowItWorks /><Industries /><Integrations />
      <MobileSection /><Scenarios /><Pricing /><Security /><FAQ /><FinalCTA />
    </main>
    <Footer /><WhatsAppButton /><DemoModal open={demoOpen} onClose={closeDemo} />
    {structuredData.map((item) => <script key={item['@type']} type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(item) }} />)}
  </div>;
}
