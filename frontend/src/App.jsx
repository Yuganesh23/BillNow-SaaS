import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { BranchProvider } from './context/BranchContext';
import { ProtectedRoute } from './components/common/ProtectedRoute';
import { AppLayout } from './components/layout/AppLayout';
import Suppliers from './pages/Suppliers';
import SupplierForm from './pages/SupplierForm';
import SupplierProfile from './pages/SupplierProfile';
import Purchases from './pages/Purchases';
import PurchaseForm from './pages/PurchaseForm';
import PurchaseDetails from './pages/PurchaseDetails';

// Pages
import LandingPage from './pages/LandingPage';
import Login from './pages/Login';
import ForgotPassword from './pages/ForgotPassword';
import Dashboard from './pages/Dashboard';
import Products from './pages/Products';
import Customers from './pages/Customers';
import CustomerDetail from './pages/CustomerDetail';
import Invoices from './pages/Invoices';
import CreateInvoice from './pages/CreateInvoice';
import InvoiceDetails from './pages/InvoiceDetails';
import Payments from './pages/Payments';
import Reports from './pages/Reports';
import Settings from './pages/Settings';
import Subscription from './pages/Subscription';
import Register from './pages/Register';
import Billers from './pages/Billers';

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
      </BrowserRouter>
      </BranchProvider>
    </AuthProvider>
  );
}

export default App;
