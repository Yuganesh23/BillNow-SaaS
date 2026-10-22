import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { resolveMessage } from '../utils/translation';

const messages = {
  en: {
    dashboard: 'Dashboard', invoices: 'Invoices', products: 'Products', purchases: 'Purchases', suppliers: 'Suppliers', customers: 'Customers', workflows: 'Workflows', billers: 'Billers', reports: 'Reports', imports: 'Imports', settings: 'Settings', subscription: 'Subscription', logout: 'Logout',
    createInvoice: 'Create Invoice', todayRevenue: "Today's Revenue", totalInvoices: 'Total Invoices', totalCustomers: 'Total Customers', totalProducts: 'Total Products', lowStock: 'Low Stock Items', recentInvoices: 'Recent Invoices', noInvoices: 'No invoices yet.', setupTitle: 'Finish setting up BillNow', stepsComplete: 'steps complete',
    signInTitle: 'Sign in to your account', signInIntro: 'Welcome back! Please enter your details.', email: 'Email address', password: 'Password', forgotPassword: 'Forgot password?', signIn: 'Sign in', signingIn: 'Signing in...', noAccount: "Don't have an account?", signUp: 'Sign up for free', language: 'Language',
    selectCustomer: 'Select Customer', customerDetails: 'Customer Details', invoiceItems: 'Invoice Items', addItem: 'Add Item', scanBarcode: 'Scan Barcode', subtotal: 'Subtotal', discount: 'Discount', grandTotal: 'Grand Total', saveInvoice: 'Save & Create Invoice', saving: 'Saving...'
  },
  ta: {
    dashboard: 'முகப்பு', invoices: 'விலைப்பட்டியல்', products: 'பொருட்கள்', purchases: 'கொள்முதல்', suppliers: 'விநியோகஸ்தர்கள்', customers: 'வாடிக்கையாளர்கள்', workflows: 'பணிச்சூழல்கள்', billers: 'பில்லர்கள்', reports: 'அறிக்கைகள்', imports: 'தரவு இறக்குமதி', settings: 'அமைப்புகள்', subscription: 'சந்தா', logout: 'வெளியேறு',
    createInvoice: 'விலைப்பட்டியல் உருவாக்கு', todayRevenue: 'இன்றைய வருவாய்', totalInvoices: 'மொத்த விலைப்பட்டியல்', totalCustomers: 'மொத்த வாடிக்கையாளர்கள்', totalProducts: 'மொத்த பொருட்கள்', lowStock: 'குறைந்த இருப்பு', recentInvoices: 'சமீபத்திய விலைப்பட்டியல்', noInvoices: 'இன்னும் விலைப்பட்டியல் இல்லை.', setupTitle: 'BillNow அமைப்பை முடிக்கவும்', stepsComplete: 'படிகள் முடிந்தன',
    signInTitle: 'உங்கள் கணக்கில் உள்நுழையவும்', signInIntro: 'மீண்டும் வரவேற்கிறோம்! உங்கள் விவரங்களை உள்ளிடவும்.', email: 'மின்னஞ்சல் முகவரி', password: 'கடவுச்சொல்', forgotPassword: 'கடவுச்சொல் மறந்ததா?', signIn: 'உள்நுழை', signingIn: 'உள்நுழைகிறது...', noAccount: 'கணக்கு இல்லையா?', signUp: 'இலவசமாக பதிவு செய்க', language: 'மொழி',
    selectCustomer: 'வாடிக்கையாளரைத் தேர்ந்தெடு', customerDetails: 'வாடிக்கையாளர் விவரங்கள்', invoiceItems: 'விலைப்பட்டியல் பொருட்கள்', addItem: 'பொருள் சேர்', scanBarcode: 'பார்கோடு ஸ்கேன்', subtotal: 'கூட்டுத்தொகை', discount: 'தள்ளுபடி', grandTotal: 'மொத்தத் தொகை', saveInvoice: 'சேமித்து உருவாக்கு', saving: 'சேமிக்கிறது...'
  },
  hi: {
    dashboard: 'डैशबोर्ड', invoices: 'चालान', products: 'उत्पाद', purchases: 'खरीद', suppliers: 'आपूर्तिकर्ता', customers: 'ग्राहक', workflows: 'कार्यप्रवाह', billers: 'बिलर', reports: 'रिपोर्ट', imports: 'डेटा आयात', settings: 'सेटिंग्स', subscription: 'सदस्यता', logout: 'लॉग आउट',
    createInvoice: 'चालान बनाएँ', todayRevenue: 'आज का राजस्व', totalInvoices: 'कुल चालान', totalCustomers: 'कुल ग्राहक', totalProducts: 'कुल उत्पाद', lowStock: 'कम स्टॉक', recentInvoices: 'हाल के चालान', noInvoices: 'अभी कोई चालान नहीं है।', setupTitle: 'BillNow सेटअप पूरा करें', stepsComplete: 'चरण पूरे',
    signInTitle: 'अपने खाते में साइन इन करें', signInIntro: 'वापसी पर स्वागत है! अपना विवरण दर्ज करें।', email: 'ईमेल पता', password: 'पासवर्ड', forgotPassword: 'पासवर्ड भूल गए?', signIn: 'साइन इन', signingIn: 'साइन इन हो रहा है...', noAccount: 'खाता नहीं है?', signUp: 'मुफ़्त साइन अप करें', language: 'भाषा',
    selectCustomer: 'ग्राहक चुनें', customerDetails: 'ग्राहक विवरण', invoiceItems: 'चालान आइटम', addItem: 'आइटम जोड़ें', scanBarcode: 'बारकोड स्कैन करें', subtotal: 'उप-योग', discount: 'छूट', grandTotal: 'कुल योग', saveInvoice: 'सहेजें और चालान बनाएँ', saving: 'सहेजा जा रहा है...'
  }
};

const LocaleContext = createContext(null);

export function LocaleProvider({ children }) {
  const [locale, setLocale] = useState(() => localStorage.getItem('billnowLocale') || 'en');
  useEffect(() => {
    localStorage.setItem('billnowLocale', locale);
    document.documentElement.lang = locale;
  }, [locale]);
  const value = useMemo(() => ({ locale, setLocale, t: (key) => resolveMessage(messages, locale, key) }), [locale]);
  return <LocaleContext.Provider value={value}>{children}</LocaleContext.Provider>;
}

export function useLocale() {
  const context = useContext(LocaleContext);
  if (!context) throw new Error('useLocale must be used inside LocaleProvider');
  return context;
}

export function LanguageSelector({ className = '' }) {
  const { locale, setLocale, t } = useLocale();
  return <select aria-label={t('language')} value={locale} onChange={(event) => setLocale(event.target.value)} className={className}>
    <option value="en">English</option><option value="ta">தமிழ்</option><option value="hi">हिन्दी</option>
  </select>;
}
