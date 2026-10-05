import React, { useState } from 'react';
import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useBranch } from '../../context/BranchContext';
import { Building2 } from 'lucide-react';
import { 
  LayoutDashboard, 
  Package, 
  Users, 
  FileText, 
  CreditCard, 
  BarChart3, 
  Settings, 
  Star, 
  LogOut,
  Menu,
  X,
  UserCog,
  Truck,
  ShoppingBag
} from 'lucide-react';

const Sidebar = ({ mobileOpen, setMobileOpen }) => {
  const { logout, user } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

    const allNavItems = [
    { name: 'Dashboard', path: '/dashboard', icon: LayoutDashboard },
    { name: 'Invoices', path: '/invoices', icon: FileText },
    { name: 'Products', path: '/products', icon: Package },
    { name: 'Purchases', path: '/purchases', icon: ShoppingBag },
    { name: 'Suppliers', path: '/suppliers', icon: Truck },
    { name: 'Customers', path: '/customers', icon: Users },
    { name: 'Billers', path: '/billers', icon: UserCog },
    { name: 'Reports', path: '/reports', icon: BarChart3 },
    { name: 'Settings', path: '/settings', icon: Settings },
    { name: 'Subscription', path: '/subscription', icon: Star },
  ];

  const navItems = user?.role === 'SHOP_OWNER' 
    ? allNavItems 
    : allNavItems.filter(item => ['Invoices', 'Customers'].includes(item.name));

  return (
    <>
      {/* Mobile overlay */}
      {mobileOpen && (
        <div 
          className="fixed inset-0 z-40 bg-gray-600 bg-opacity-50 lg:hidden"
          onClick={() => setMobileOpen(false)}
        />
      )}

      {/* Sidebar */}
      <div className={`fixed inset-y-0 left-0 z-50 w-64 bg-white border-r border-slate-200 transform transition-transform duration-200 ease-in-out lg:translate-x-0 ${mobileOpen ? 'translate-x-0' : '-translate-x-full'} flex flex-col`}>
        <div className="flex items-center justify-between h-16 px-6 border-b border-slate-200">
          <span className="text-xl font-bold text-indigo-600">BillNow</span>
          <button onClick={() => setMobileOpen(false)} className="lg:hidden text-slate-500 hover:text-slate-700">
            <X size={24} />
          </button>
        </div>

        <nav className="flex-1 px-4 py-6 space-y-1 overflow-y-auto">
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.name}
                to={item.path}
                className={({ isActive }) =>
                  `flex items-center px-2 py-2 text-[14px] rounded-md ${
                    isActive
                      ? 'bg-indigo-50 text-indigo-600 font-semibold'
                      : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900 font-medium'
                  }`
                }
              >
                <Icon className="w-5 h-5 mr-3" />
                {item.name}
              </NavLink>
            );
          })}
        </nav>

        <div className="p-4 border-t border-slate-200">
          <button
            onClick={handleLogout}
            className="flex items-center w-full px-2 py-2 text-sm font-medium text-slate-600 rounded-md hover:bg-slate-50 hover:text-slate-900"
          >
            <LogOut className="w-5 h-5 mr-3" />
            Logout
          </button>
        </div>
      </div>
    </>
  );
};


const BranchSelector = () => {
  const { branches, activeBranchId, changeBranch } = useBranch();
  
  if (!branches || branches.length === 0) return null;
  
  return (
    <div className="flex items-center mr-6 border-r border-slate-200 pr-6">
      <Building2 className="w-4 h-4 text-slate-500 mr-2" />
      <select 
        value={activeBranchId || ''} 
        onChange={(e) => changeBranch(e.target.value)}
        className="text-sm font-medium text-slate-700 bg-transparent border-none focus:ring-0 cursor-pointer"
      >
        <option value="">All Branches</option>
        {branches.map(b => (
          <option key={b.id} value={b.id}>{b.name}</option>
        ))}
      </select>
    </div>
  );
};

export const AppLayout = () => {
  const [mobileOpen, setMobileOpen] = useState(false);
  const { user } = useAuth();

  return (
    <div className="min-h-screen bg-slate-50">
      <Sidebar mobileOpen={mobileOpen} setMobileOpen={setMobileOpen} />
      
      <div className="lg:pl-64 flex flex-col flex-1">
        <header className="sticky top-0 z-10 flex items-center justify-between h-16 px-4 bg-white border-b border-slate-200 sm:px-6 lg:px-8">
          <button
            className="text-slate-500 hover:text-slate-700 lg:hidden"
            onClick={() => setMobileOpen(true)}
          >
            <Menu size={24} />
          </button>
          
          <div className="flex-1 px-4 flex justify-end">
            {user?.role === 'SHOP_OWNER' && (
              <BranchSelector />
            )}
          </div>
          
          <div className="flex items-center">
            <span className="text-sm font-medium text-slate-700">{user?.name || 'User'}</span>
            <div className="w-8 h-8 ml-3 bg-indigo-100 rounded-full flex items-center justify-center text-indigo-600 font-bold">
              {(user?.name || 'U').charAt(0)}
            </div>
          </div>
        </header>

        <main className="flex-1 pb-8 max-w-[1440px] mx-auto w-full">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

