import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getSupplierById, createSupplier, updateSupplier } from '../api/supplierApi';
import { LoadingState } from '../components/common/States';
import { ArrowLeft, Save } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function SupplierForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = Boolean(id);
  
  const [loading, setLoading] = useState(isEdit);
  const [saving, setSaving] = useState(false);
  const [phoneError, setPhoneError] = useState("");
  const [error, setError] = useState(null);
  
  const [formData, setFormData] = useState({
    businessName: '',
    contactPerson: '',
    phone: '',
    whatsapp: '',
    email: '',
    gstin: '',
    address: '',
    notes: ''
  });

  useEffect(() => {
    if (isEdit) {
      getSupplierById(id)
        .then(res => setFormData(res.data))
        .catch(console.error)
        .finally(() => setLoading(false));
    }
  }, [id, isEdit]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      if (isEdit) {
        await updateSupplier(id, formData);
      } else {
        await createSupplier(formData);
      }
      navigate('/suppliers');
    } catch (err) {
      console.error(err);
      alert('Failed to save supplier');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <LoadingState />;

  return (
    <div className="p-4 sm:p-6 lg:p-8 max-w-3xl mx-auto">
      <div className="mb-6 flex items-center">
        <Link to="/suppliers" className="mr-4 text-slate-400 hover:text-slate-600">
          <ArrowLeft className="w-6 h-6" />
        </Link>
        <h1 className="text-2xl font-bold text-slate-900">
          {isEdit ? 'Edit Supplier' : 'Add Supplier'}
        </h1>
      </div>

            <form onSubmit={handleSubmit} className="bg-white shadow-sm ring-1 ring-slate-200 rounded-lg p-6 space-y-8">
        
        {/* SECTION 1: Basic Information */}
        <div>
          <h4 className="text-sm font-semibold text-slate-900 mb-4 border-b pb-2">Basic Information</h4>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-slate-700 mb-1">Business Name *</label>
              <input required type="text" name="businessName" value={formData.businessName} onChange={handleChange} className="block w-full border border-slate-300 rounded-md py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
            </div>
            
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Contact Person</label>
              <input type="text" name="contactPerson" value={formData.contactPerson} onChange={handleChange} className="block w-full border border-slate-300 rounded-md py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Email</label>
              <input type="email" name="email" value={formData.email} onChange={handleChange} className="block w-full border border-slate-300 rounded-md py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Phone *</label>
              <input type="text" name="phone" value={formData.phone} onChange={handleChange} className={`block w-full border rounded-md py-2 px-3 focus:outline-none sm:text-sm ${phoneError ? "border-red-300 focus:ring-red-500 focus:border-red-500" : "border-slate-300 focus:ring-indigo-500 focus:border-indigo-500"}`} />
              {phoneError && <p className="mt-1 text-sm text-red-600">{phoneError}</p>}
            </div>

            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="block text-sm font-medium text-slate-700">WhatsApp</label>
                <label className="flex items-center text-xs text-slate-500 cursor-pointer">
                  <input type="checkbox" className="mr-1.5 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500" onChange={(e) => { if(e.target.checked) handleChange({target: {name: 'whatsapp', value: formData.phone}})} } />
                  Same as phone
                </label>
              </div>
              <input type="text" name="whatsapp" value={formData.whatsapp} onChange={handleChange} className="block w-full border border-slate-300 rounded-md py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
            </div>
          </div>
        </div>

        {/* SECTION 2: Tax & Address */}
        <div>
          <h4 className="text-sm font-semibold text-slate-900 mb-4 border-b pb-2 mt-2">Tax & Address</h4>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-slate-700 mb-1">GSTIN <span className="text-slate-400 font-normal ml-1">(optional)</span></label>
              <input type="text" name="gstin" value={formData.gstin} onChange={handleChange} className="block w-full border border-slate-300 rounded-md py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm uppercase" />
            </div>

            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-slate-700 mb-1">Address</label>
              <textarea name="address" rows={2} value={formData.address} onChange={handleChange} className="block w-full border border-slate-300 rounded-md py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
            </div>
          </div>
        </div>

        {/* SECTION 3: Additional */}
        <div>
          <h4 className="text-sm font-semibold text-slate-900 mb-4 border-b pb-2 mt-2">Additional</h4>
          <div className="grid grid-cols-1 gap-6">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Notes</label>
              <textarea name="notes" rows={2} value={formData.notes} onChange={handleChange} className="block w-full border border-slate-300 rounded-md py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
            </div>
          </div>
        </div>

        <div className="pt-4 border-t border-slate-200 flex justify-end mt-4">
          <Link to="/suppliers" className="bg-white py-2 px-4 border border-slate-300 rounded-md shadow-sm text-sm font-medium text-slate-700 hover:bg-slate-50 focus:outline-none mr-3">
            Cancel
          </Link>
          <button type="submit" disabled={saving || !formData.businessName?.trim()} className="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed">
            {saving ? 'Saving...' : 'Save Supplier'}
          </button>
        </div>
      </form>
    </div>
  );
}
