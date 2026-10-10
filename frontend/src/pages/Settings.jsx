import React, { useState, useEffect } from 'react';
import axiosClient from '../api/axiosClient';
import { useAuth } from '../context/AuthContext';
import { useBranch } from '../context/BranchContext';
import { Building2, Plus, Mail, Phone, User, Edit2, Trash2 } from 'lucide-react';

export default function Settings() {
  const { user, logout } = useAuth();
  const { fetchBranches } = useBranch();
  
  const [userProfile, setUserProfile] = useState({ name: '', email: '', mobileNumber: '' });
  const [shops, setShops] = useState([]);
  
  const [loading, setLoading] = useState(true);
  const [savingUser, setSavingUser] = useState(false);
  const [showAddShop, setShowAddShop] = useState(false);
  const [newShop, setNewShop] = useState({ name: '', invoiceName: '', email: '', mobileNumber: '', address: '', gstin: '', legalName: '', state: '' });
  const [editingShop, setEditingShop] = useState(null);
  const [editShopData, setEditShopData] = useState({ id: null, name: '', invoiceName: '', email: '', mobileNumber: '', address: '', logoBase64: '', gstin: '', legalName: '', state: '' });

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const profileRes = await axiosClient.get('/profile');
      setUserProfile({ 
        name: profileRes.data.name || '',
        email: profileRes.data.email || '',
        mobileNumber: profileRes.data.mobileNumber || ''
      });
      
      if (user?.role === 'SHOP_OWNER') {
        const shopsRes = await axiosClient.get('/shops');
        setShops(shopsRes.data);
      }
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleSaveUser = async () => {
    setSavingUser(true);
    try {
      const emailChanged = userProfile.email !== user?.email;
      await axiosClient.put('/profile', { 
        userName: userProfile.name,
        userEmail: userProfile.email,
        userMobile: userProfile.mobileNumber
      });
      alert('Profile updated successfully!');
      
      if (emailChanged) {
        alert(`You changed your login email. Sign in again using ${userProfile.email}.`);
        await logout();
      }
    } catch (e) {
      alert('Failed to update profile.');
    } finally {
      setSavingUser(false);
    }
  };

  const handleEditShopClick = (shop) => {
    setEditShopData({
      id: shop.id,
      name: shop.name || '',
      invoiceName: shop.invoiceName || '',
      email: shop.email || '',
      mobileNumber: shop.mobileNumber || '',
      address: shop.address || '',
      logoBase64: shop.logoBase64 || '',
      gstin: shop.gstin || '',
      legalName: shop.legalName || '',
      state: shop.state || ''
    });
    setEditingShop(shop.id);
  };

  const handleUpdateShop = async (e) => {
    e.preventDefault();
    try {
      await axiosClient.put(`/shops/${editShopData.id}`, editShopData);
      alert('Branch updated successfully!');
      setEditingShop(null);
      fetchData();
      fetchBranches();
    } catch (e) {
      console.error(e);
      alert(e.response?.data?.message || 'Failed to update branch.');
    }
  };

  const handleLogoUpload = (e) => {
    const file = e.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setEditShopData(prev => ({ ...prev, logoBase64: reader.result }));
      };
      reader.readAsDataURL(file);
    }
  };


  const handleDeleteShop = async (shopId, shopName) => {
    if (!window.confirm(`Are you sure you want to delete branch "${shopName}"?\n\nWARNING: You can only delete branches that have no invoices or data attached. This action cannot be undone.`)) return;
    try {
      await axiosClient.delete(`/shops/${shopId}`);
      alert('Branch deleted successfully!');
      fetchData();
      fetchBranches(); // refresh global dropdown
    } catch (e) {
      console.error(e);
      alert(e.response?.data?.message || 'Cannot delete branch. It contains existing data (invoices, products, etc).');
    }
  };

  const handleAddShop = async (e) => {
    e.preventDefault();
    try {
      await axiosClient.post('/shops', newShop);
      alert('New branch added successfully!');
      setShowAddShop(false);
      setNewShop({ name: '', email: '', mobileNumber: '', address: '', gstin: '', legalName: '', state: '' });
      fetchData();
      fetchBranches(); // refresh global dropdown
    } catch (e) {
      console.error(e);
      alert(e.response?.data?.message || 'Failed to add branch. Please check console.');
    }
  };

  if (loading) return <div className="p-4 sm:p-6 lg:p-8">Loading...</div>;

  return (
    <div className="p-4 sm:p-6 lg:p-8">
      <div className="mb-8 flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Settings</h1>
          <p className="mt-2 text-sm text-slate-700">Manage your profile and business branches.</p>
        </div>
        {user?.role === 'SHOP_OWNER' && (
          <button onClick={() => setShowAddShop(true)} className="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500">
            <Plus className="w-5 h-5 mr-2 -ml-1" /> Add New Branch
          </button>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Personal Profile Section */}
        <div className="bg-white shadow-sm ring-1 ring-slate-200 rounded-lg p-6">
          <h2 className="text-lg font-medium text-slate-900 mb-6 flex items-center">
            <User className="w-5 h-5 mr-2 text-indigo-500" />
            Personal Profile
          </h2>
          <div className="space-y-5">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Full Name</label>
              <div className="relative rounded-md shadow-sm">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <User className="h-4 w-4 text-slate-400" />
                </div>
                <input type="text" value={userProfile.name} onChange={e => setUserProfile({...userProfile, name: e.target.value})} className="pl-10 block w-full bg-white text-slate-900 border border-slate-300 rounded-md py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Login Email</label>
              <div className="relative rounded-md shadow-sm">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Mail className="h-4 w-4 text-slate-400" />
                </div>
                <input type="email" value={userProfile.email} onChange={e => setUserProfile({...userProfile, email: e.target.value})} className="pl-10 block w-full bg-white text-slate-900 border border-slate-300 rounded-md py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Mobile Number</label>
              <div className="relative rounded-md shadow-sm">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Phone className="h-4 w-4 text-slate-400" />
                </div>
                <input type="text" placeholder="e.g. +91 9876543210" value={userProfile.mobileNumber} onChange={e => setUserProfile({...userProfile, mobileNumber: e.target.value})} className="pl-10 block w-full bg-white text-slate-900 border border-slate-300 rounded-md py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
              </div>
            </div>

            <div className="pt-4">
              <button onClick={handleSaveUser} disabled={savingUser} className="inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-slate-900 text-sm font-medium text-white hover:bg-slate-800 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-slate-900 disabled:opacity-50">
                {savingUser ? 'Saving...' : 'Save Profile Changes'}
              </button>
            </div>
          </div>
        </div>

        {user?.role === 'SHOP_OWNER' && (
          <div className="space-y-6">
            {/* Add Branch Form is now a Modal at the bottom */}

            {/* List of Branches */}
            <div className="space-y-4">
              <h3 className="text-sm font-medium text-slate-500 uppercase tracking-wider">Your Branches</h3>
              {shops.map(shop => (
                <div key={shop.id} className="bg-white shadow-sm ring-1 ring-slate-200 rounded-lg p-5 hover:shadow-md transition-shadow">
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center">
                      {shop.logoBase64 ? (
                        <img src={shop.logoBase64} alt={shop.name} className="w-10 h-10 rounded-md object-contain border border-slate-200 mr-3 bg-white" />
                      ) : (
                        <div className="w-10 h-10 rounded-full bg-indigo-100 flex items-center justify-center text-indigo-600 mr-3">
                          <Building2 className="w-5 h-5" />
                        </div>
                      )}
                      <h2 className="text-lg font-medium text-slate-900">{shop.name}</h2>
                    </div>
                    
                      <div className="flex items-center space-x-2">
                        <button onClick={() => handleEditShopClick(shop)} className="text-indigo-600 hover:text-indigo-900 p-1 bg-indigo-50 rounded" title="Edit Branch">
                          <Edit2 className="w-4 h-4" />
                        </button>
                        <button onClick={() => handleDeleteShop(shop.id, shop.name)} className="text-red-600 hover:text-red-900 p-1 bg-red-50 rounded" title="Delete Branch">
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                  </div>
                  <div className="text-sm text-slate-600 space-y-2 ml-13 pl-13">
                    {shop.email && (
                      <p className="flex items-center"><Mail className="w-4 h-4 mr-2 text-slate-400" /> {shop.email}</p>
                    )}
                    {shop.mobileNumber && (
                      <p className="flex items-center"><Phone className="w-4 h-4 mr-2 text-slate-400" /> {shop.mobileNumber}</p>
                    )}
                    {shop.address && (
                      <p className="flex items-start text-slate-500"><span className="font-medium mr-2 text-slate-600">Address:</span> {shop.address}</p>
                    )}
                  </div>
                </div>
              ))}
              {shops.length === 0 && (
                <div className="text-center p-6 bg-white rounded-lg border border-dashed border-slate-300">
                  <p className="text-slate-500">You haven't created any additional branches yet.</p>
                </div>
              )}
            </div>
          </div>
        )}

      {/* Add Branch Modal */}
      {showAddShop && (
        <div className="fixed inset-0 z-50 overflow-y-auto">
          <div className="flex items-center justify-center min-h-screen px-4 pt-4 pb-20 text-center sm:p-0">
            <div className="fixed inset-0 transition-opacity" aria-hidden="true" onClick={() => setShowAddShop(false)}>
              <div className="absolute inset-0 bg-gray-500 opacity-75"></div>
            </div>
            <div className="relative z-10 inline-block align-bottom bg-white rounded-lg px-4 pt-5 pb-4 text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full sm:p-6">
              <div className="flex justify-between items-center mb-4">
                <h3 className="text-lg leading-6 font-medium text-slate-900 flex items-center">
                  <Building2 className="w-5 h-5 mr-2 text-indigo-600" /> Add New Branch
                </h3>
              </div>
              <form onSubmit={handleAddShop} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700">Branch Name (Internal)</label>
                  <input type="text" required value={newShop.name} onChange={e => setNewShop({...newShop, name: e.target.value})} className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700">PDF Display Name (Optional)</label>
                  <input type="text" value={newShop.invoiceName} onChange={e => setNewShop({...newShop, invoiceName: e.target.value})} placeholder="e.g. Raymond" className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700">Branch Email (Optional)</label>
                  <input type="email" value={newShop.email} onChange={e => setNewShop({...newShop, email: e.target.value})} className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700">Branch Mobile (Optional)</label>
                  <input type="text" value={newShop.mobileNumber} onChange={e => setNewShop({...newShop, mobileNumber: e.target.value})} className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700">Legal Name (For GST)</label>
                  <input type="text" value={newShop.legalName} onChange={e => setNewShop({...newShop, legalName: e.target.value})} className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-700">GSTIN</label>
                    <input type="text" value={newShop.gstin} onChange={e => setNewShop({...newShop, gstin: e.target.value})} className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700">State</label>
                    <input type="text" placeholder="e.g. Tamil Nadu" value={newShop.state} onChange={e => setNewShop({...newShop, state: e.target.value})} className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700">Branch Address</label>
                  <textarea rows="3" required value={newShop.address} onChange={e => setNewShop({...newShop, address: e.target.value})} className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
                </div>
                <div className="mt-5 sm:mt-6 sm:grid sm:grid-cols-2 sm:gap-3 sm:grid-flow-row-dense">
                  <button type="submit" className="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-indigo-600 text-base font-medium text-white hover:bg-indigo-700 focus:outline-none sm:col-start-2 sm:text-sm">
                    Create Branch
                  </button>
                  <button type="button" onClick={() => setShowAddShop(false)} className="mt-3 w-full inline-flex justify-center rounded-md border border-slate-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-slate-700 hover:bg-slate-50 focus:outline-none sm:mt-0 sm:col-start-1 sm:text-sm">
                    Cancel
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

{/* Edit Branch Modal */}
      {editingShop && (
        <div className="fixed inset-0 z-50 overflow-y-auto">
          <div className="flex items-center justify-center min-h-screen px-4 pt-4 pb-20 text-center sm:p-0">
            <div className="fixed inset-0 transition-opacity" aria-hidden="true" onClick={() => setEditingShop(null)}>
              <div className="absolute inset-0 bg-gray-500 opacity-75"></div>
            </div>
            <div className="relative z-10 inline-block align-bottom bg-white rounded-lg px-4 pt-5 pb-4 text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full sm:p-6">
              <div className="flex justify-between items-center mb-4">
                <h3 className="text-lg leading-6 font-medium text-slate-900 flex items-center">
                  <Building2 className="w-5 h-5 mr-2 text-indigo-600" /> Edit Branch
                </h3>
              </div>
              <form onSubmit={handleUpdateShop} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700">Branch Name (Internal)</label>
                  <input type="text" required value={editShopData.name} onChange={e => setEditShopData({...editShopData, name: e.target.value})} className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700">PDF Display Name</label>
                  <input type="text" placeholder="Leave blank to use Branch Name" value={editShopData.invoiceName} onChange={e => setEditShopData({...editShopData, invoiceName: e.target.value})} className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700">Branch Email</label>
                  <input type="email" required value={editShopData.email} onChange={e => setEditShopData({...editShopData, email: e.target.value})} className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700">Mobile Number</label>
                  <input type="text" value={editShopData.mobileNumber} onChange={e => setEditShopData({...editShopData, mobileNumber: e.target.value})} className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700">Legal Name (For GST)</label>
                  <input type="text" value={editShopData.legalName} onChange={e => setEditShopData({...editShopData, legalName: e.target.value})} className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-700">GSTIN</label>
                    <input type="text" value={editShopData.gstin} onChange={e => setEditShopData({...editShopData, gstin: e.target.value})} className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700">State</label>
                    <input type="text" placeholder="e.g. Tamil Nadu" value={editShopData.state} onChange={e => setEditShopData({...editShopData, state: e.target.value})} className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700">Address</label>
                  <textarea rows="2" required value={editShopData.address} onChange={e => setEditShopData({...editShopData, address: e.target.value})} className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">Branch Logo</label>
                  <div className="flex items-center space-x-4">
                    {editShopData.logoBase64 && (
                      <img src={editShopData.logoBase64} alt="Preview" className="h-12 w-12 object-contain rounded border border-slate-200" />
                    )}
                    <input type="file" accept="image/*" onChange={handleLogoUpload} className="block w-full text-sm text-slate-500 file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-semibold file:bg-indigo-50 file:text-indigo-700 hover:file:bg-indigo-100" />
                  </div>
                </div>
                <div className="mt-5 sm:mt-6 sm:grid sm:grid-cols-2 sm:gap-3 sm:grid-flow-row-dense">
                  <button type="submit" className="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-indigo-600 text-base font-medium text-white hover:bg-indigo-700 focus:outline-none sm:col-start-2 sm:text-sm">
                    Save Changes
                  </button>
                  <button type="button" onClick={() => setEditingShop(null)} className="mt-3 w-full inline-flex justify-center rounded-md border border-slate-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-slate-700 hover:bg-slate-50 focus:outline-none sm:mt-0 sm:col-start-1 sm:text-sm">
                    Cancel
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
      </div>
    </div>
  );
}
