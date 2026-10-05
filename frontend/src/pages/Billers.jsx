import React, { useState, useEffect } from 'react';
import { getBillers, createBiller, deactivateBiller, updateBiller } from '../api/billerApi';
import { EmptyState, LoadingState } from '../components/common/States';
import { UserCog, Plus, Search, UserMinus, Edit2 } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function Billers() {
  const [billers, setBillers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  
  // Modal state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingBiller, setEditingBiller] = useState(null);
  const [formData, setFormData] = useState({ name: '', email: '', mobileNumber: '', password: '' });

  const fetchBillers = async () => {
    try {
      setLoading(true);
      const data = await getBillers();
      setBillers(data);
    } catch (err) {
      setError('Failed to fetch billers');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBillers();
  }, []);

  const handleOpenModal = (biller = null) => {
    if (biller) {
      setEditingBiller(biller);
      setFormData({ name: biller.name, email: biller.email, mobileNumber: biller.mobileNumber || '', password: '' });
    } else {
      setEditingBiller(null);
      setFormData({ name: '', email: '', mobileNumber: '', password: '' });
    }
    setIsModalOpen(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingBiller) {
        await updateBiller(editingBiller.id, formData);
      } else {
        await createBiller(formData);
      }
      setIsModalOpen(false);
      fetchBillers();
    } catch (err) {
      alert(err.response?.data?.message || err.response?.data?.error || 'Failed to save biller');
    }
  };

  const handleDeactivate = async (id) => {
    if (window.confirm('Are you sure you want to deactivate this biller?')) {
      try {
        await deactivateBiller(id);
        fetchBillers();
      } catch (err) {
        alert('Failed to deactivate biller');
      }
    }
  };

  if (error) return <div className="p-8 text-red-500">{error}</div>;

  const filteredBillers = billers.filter(b => 
    b.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
    b.email.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (loading && billers.length === 0) return <LoadingState message="Loading billers..." />;

  return (
    <div className="p-4 sm:p-6 lg:p-8">
      <div className="sm:flex sm:items-center sm:justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Billers</h1>
          <p className="mt-2 text-sm text-slate-700">Manage your staff and billing clerks.</p>
        </div>
        <div className="mt-4 sm:mt-0">
          <button
            onClick={() => handleOpenModal()}
            className="inline-flex items-center justify-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700"
          >
            <Plus className="w-5 h-5 mr-2 -ml-1" />
            Add Biller
          </button>
        </div>
      </div>

      <div className="mb-6 max-w-md relative">
        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
          <Search className="h-5 w-5 text-slate-400" />
        </div>
        <input
          type="text"
          className="block w-full pl-10 pr-3 py-2 border border-slate-300 rounded-md leading-5 bg-white text-slate-900 placeholder-slate-500 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
          placeholder="Search by name or email..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
      </div>

      {billers.length === 0 ? (
        <EmptyState 
          icon={UserCog}
          title="No billers yet"
          description="Add a biller to help you create invoices."
          action={
            <button
              onClick={() => handleOpenModal()}
              className="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700"
            >
              Add Biller
            </button>
          }
        />
      ) : (
        <div className="bg-white shadow-sm ring-1 ring-slate-200 rounded-lg overflow-hidden">
          <table className="min-w-full divide-y divide-slate-200">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Staff Details</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Contact</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Performance</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Status</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-slate-500 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-slate-200">
              {filteredBillers.map((biller) => (
                <tr key={biller.id} className="hover:bg-slate-50">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-slate-900 flex items-center">
                      {biller.name}
                      {biller.role === 'SHOP_OWNER' && <span className="ml-2 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-amber-100 text-amber-800">Owner</span>}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-500">
                    <div>{biller.email}</div>
                    {biller.mobileNumber && <div className="text-xs text-slate-400">{biller.mobileNumber}</div>}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-900">
                    <div className="flex items-center space-x-3">
                      <div><span className="font-medium text-slate-500 text-xs mr-1">Sales:</span> ₹{biller.totalSales ? parseFloat(biller.totalSales).toLocaleString() : '0'}</div>
                      <div>
                        <span className="font-medium text-slate-500 text-xs mr-1">Invoices:</span> 
                        <Link to={`/invoices?billerId=${biller.id}`} className="inline-flex items-center justify-center px-2 py-1 text-xs font-bold leading-none text-indigo-100 bg-indigo-600 rounded-full hover:bg-indigo-700 transition-colors">
                          {biller.totalInvoices || 0}
                        </Link>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-500">
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${biller.active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                      {biller.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    {biller.active && biller.role !== 'SHOP_OWNER' && (
                      <>
                      <button onClick={() => handleOpenModal(biller)} className="text-indigo-600 hover:text-indigo-900 font-medium text-[13px]">
                          Edit
                        </button>
                        <span className="mx-2 text-slate-300">&middot;</span>
                        <button onClick={() => handleDeactivate(biller.id)} className="text-red-600 hover:text-red-900 font-medium text-[13px]">
                          Disable
                        </button>
                      </>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {isModalOpen && (
        <div className="fixed inset-0 z-50 overflow-y-auto">
          <div className="flex items-center justify-center min-h-screen px-4 pt-4 pb-20 text-center sm:p-0">
            <div className="fixed inset-0 transition-opacity" aria-hidden="true" onClick={() => setIsModalOpen(false)}>
              <div className="absolute inset-0 bg-gray-500 opacity-75"></div>
            </div>
            <div className="relative z-10 inline-block align-bottom bg-white rounded-lg px-4 pt-5 pb-4 text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full sm:p-6">
              <div>
                <h3 className="text-lg leading-6 font-medium text-slate-900 mb-4">
                  {editingBiller ? 'Edit Biller' : 'Add Biller'}
                </h3>
                <form onSubmit={handleSubmit} className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-700">Name</label>
                    <input type="text" required value={formData.name} onChange={e => setFormData({...formData, name: e.target.value})} className="mt-1 block w-full bg-white text-slate-900 border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700">Email</label>
                    <input type="email" required value={formData.email} onChange={e => setFormData({...formData, email: e.target.value})} className="mt-1 block w-full bg-white text-slate-900 border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700">Mobile Number (Optional)</label>
                    <input type="text" value={formData.mobileNumber} onChange={e => setFormData({...formData, mobileNumber: e.target.value})} className="mt-1 block w-full bg-white text-slate-900 border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700">Password {editingBiller && <span className="text-slate-400 font-normal text-xs">(Leave blank to keep unchanged)</span>}</label>
                    <input type="password" required={!editingBiller} minLength="6" value={formData.password} onChange={e => setFormData({...formData, password: e.target.value})} className="mt-1 block w-full bg-white text-slate-900 border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
                  </div>
                  
                  <div className="mt-5 sm:mt-6 sm:grid sm:grid-cols-2 sm:gap-3 sm:grid-flow-row-dense">
                    <button type="submit" className="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-indigo-600 text-base font-medium text-white hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 sm:col-start-2 sm:text-sm">
                        Save Biller
                      </button>
                    <button type="button" onClick={() => setIsModalOpen(false)} className="mt-3 w-full inline-flex justify-center rounded-md border border-slate-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-slate-700 hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 sm:mt-0 sm:col-start-1 sm:text-sm">
                      Cancel
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
