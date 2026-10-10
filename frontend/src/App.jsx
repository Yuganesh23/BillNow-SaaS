import React from 'react';
import { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { BranchProvider } from './context/BranchContext';
import { ProtectedRoute } from './components/common/ProtectedRoute';
import { AppLayout } from './components/layout/AppLayout';
const LandingPage = lazy(() => import('./pages/LandingPage'));
const Login = lazy(() => import('./pages/Login'));
const ForgotPassword = lazy(() => import('./pages/ForgotPassword'));
const Dashboard = lazy(() => import('./pages/Dashboard'));
const Products = lazy(() => import('./pages/Products'));
const Customers = lazy(() => import('./pages/Customers'));
const CustomerDetail = lazy(() => import('./pages/CustomerDetail'));
const Invoices = lazy(() => import('./pages/Invoices'));
const CreateInvoice = lazy(() => import('./pages/CreateInvoice'));
const InvoiceDetails = lazy(() => import('./pages/InvoiceDetails'));
const Payments = lazy(() => import('./pages/Payments'));
const Reports = lazy(() => import('./pages/Reports'));
const Settings = lazy(() => import('./pages/Settings'));
const Subscription = lazy(() => import('./pages/Subscription'));
const Register = lazy(() => import('./pages/Register'));
const Billers = lazy(() => import('./pages/Billers'));
const Suppliers = lazy(() => import('./pages/Suppliers'));
const SupplierForm = lazy(() => import('./pages/SupplierForm'));
const SupplierProfile = lazy(() => import('./pages/SupplierProfile'));
const Purchases = lazy(() => import('./pages/Purchases'));
const PurchaseForm = lazy(() => import('./pages/PurchaseForm'));
const PurchaseDetails = lazy(() => import('./pages/PurchaseDetails'));

function RootRedirect() {
  const { user } = useAuth();
  if (!user) {
    return <LandingPage />;
  }
  if (user?.role === 'BILLER') {
    return <Navigate to="/invoices" replace />;
  }
  return <Navigate to="/dashboard" replace />;
}

function App() {
  return (
    <AuthProvider>
      <BranchProvider>
      <BrowserRouter>
        <Suspense fallback={<div className="min-h-screen grid place-items-center text-slate-600">Loading BillNow…</div>}>
        <Routes>
          <Route path="/" element={<RootRedirect />} />
          <Route path="/login" element={<Login />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/register" element={<Register />} />
          
          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/billers" element={<Billers />} />
              
              <Route path="/products" element={<Products />} />
              
              <Route path="/customers" element={<Customers />} />
              <Route path="/suppliers" element={<Suppliers />} />
              <Route path="/suppliers/new" element={<SupplierForm />} />
              <Route path="/suppliers/:id" element={<SupplierProfile />} />
              <Route path="/suppliers/:id/edit" element={<SupplierForm />} />
              <Route path="/purchases" element={<Purchases />} />
              <Route path="/purchases/new" element={<PurchaseForm />} />
              <Route path="/purchases/:id" element={<PurchaseDetails />} />

              <Route path="/customers/:id" element={<CustomerDetail />} />
              
              <Route path="/invoices" element={<Invoices />} />
              <Route path="/invoices/create" element={<CreateInvoice />} />
              <Route path="/invoices/:id" element={<InvoiceDetails />} />
              
              <Route path="/payments" element={<Payments />} />
              
              <Route path="/reports" element={<Reports />} />
              
              <Route path="/settings" element={<Settings />} />
              <Route path="/subscription" element={<Subscription />} />
            </Route>
          </Route>
        </Routes>
        </Suspense>
      </BrowserRouter>
      </BranchProvider>
    </AuthProvider>
  );
}

export default App;
