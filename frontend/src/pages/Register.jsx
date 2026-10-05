import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import axiosClient from '../api/axiosClient';
import { User, Store, Mail, Phone, MapPin, Lock, UserPlus, CheckCircle2 } from 'lucide-react';

export default function Register() {
  const [formData, setFormData] = useState({
    ownerName: '',
    shopName: '',
    email: '',
    mobileNumber: '',
    password: '',
    address: '',
    logoBase64: ''
  });
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();

  const handleLogoUpload = (e) => {
    const file = e.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setFormData({ ...formData, logoBase64: reader.result });
      };
      reader.readAsDataURL(file);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setIsSubmitting(true);
    
    try {
      await axiosClient.post('/auth/register', formData);
      navigate('/login', { state: { message: 'Registration successful. Please log in.' } });
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-white flex">
      {/* Left Marketing Panel - Hidden on mobile */}
      <div className="hidden lg:flex lg:w-1/2 bg-slate-900 flex-col justify-between p-12 lg:p-16 xl:p-20 text-white">
        <div>
          <div className="flex items-center gap-3 mb-16">
            <div className="w-12 h-12 bg-green-500 rounded-xl flex items-center justify-center transform rotate-3 shadow-lg shadow-green-500/20">
              <span className="text-white font-bold text-2xl transform -rotate-3">B</span>
            </div>
            <span className="text-3xl font-bold tracking-tight">BillNow</span>
          </div>
          
          <h1 className="text-4xl xl:text-5xl font-bold leading-tight mb-6">
            Transform how you manage your retail business.
          </h1>
          <p className="text-lg text-slate-300 mb-12 max-w-lg leading-relaxed">
            Join thousands of modern store owners who use BillNow to streamline their point of sale, track multi-branch inventory, and automate supplier ledgers effortlessly.
          </p>
          
          <div className="space-y-8">
            <div className="flex items-start gap-4">
              <div className="flex-shrink-0 w-10 h-10 rounded-full bg-green-500/20 flex items-center justify-center mt-1">
                <CheckCircle2 className="w-5 h-5 text-green-400" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-white">Lightning Fast POS</h3>
                <p className="text-slate-400 mt-1">Generate professional GST invoices in under 10 seconds.</p>
              </div>
            </div>
            
            <div className="flex items-start gap-4">
              <div className="flex-shrink-0 w-10 h-10 rounded-full bg-green-500/20 flex items-center justify-center mt-1">
                <CheckCircle2 className="w-5 h-5 text-green-400" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-white">Multi-Branch Sync</h3>
                <p className="text-slate-400 mt-1">Monitor all your shops and staff from one unified dashboard.</p>
              </div>
            </div>
            
            <div className="flex items-start gap-4">
              <div className="flex-shrink-0 w-10 h-10 rounded-full bg-green-500/20 flex items-center justify-center mt-1">
                <CheckCircle2 className="w-5 h-5 text-green-400" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-white">Smart Supplier Ledgers</h3>
                <p className="text-slate-400 mt-1">Never lose track of an advance payment with our auto-allocation engine.</p>
              </div>
            </div>
          </div>
        </div>
        
        <div className="mt-16 pt-10 border-t border-slate-800">
          <div className="flex items-center gap-4">
            <div className="flex -space-x-3">
              <img className="w-12 h-12 rounded-full border-2 border-slate-900 object-cover" src="https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=100&q=80" alt="Avatar" />
              <img className="w-12 h-12 rounded-full border-2 border-slate-900 object-cover" src="https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=100&q=80" alt="Avatar" />
              <img className="w-12 h-12 rounded-full border-2 border-slate-900 object-cover" src="https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=100&q=80" alt="Avatar" />
              <div className="w-12 h-12 rounded-full border-2 border-slate-900 bg-slate-800 flex items-center justify-center text-xs font-medium text-white">
                1k+
              </div>
            </div>
            <p className="text-sm text-slate-400 max-w-[200px]">
              Trusted by <span className="text-white font-semibold">1,000+</span> retail owners across India.
            </p>
          </div>
        </div>
      </div>

      {/* Right Registration Form */}
      <div className="w-full lg:w-1/2 flex flex-col justify-center py-12 px-4 sm:px-6 lg:px-20 xl:px-24 bg-white overflow-y-auto">
        <div className="mx-auto w-full max-w-sm lg:max-w-md">
          {/* Mobile Header (Hidden on Desktop) */}
          <div className="lg:hidden flex items-center gap-3 mb-8 justify-center">
            <div className="w-10 h-10 bg-green-500 rounded-lg flex items-center justify-center transform rotate-3">
              <span className="text-white font-bold text-xl transform -rotate-3">B</span>
            </div>
            <span className="text-2xl font-bold tracking-tight text-slate-900">BillNow</span>
          </div>

          <div>
            <h2 className="text-3xl font-extrabold text-slate-900 tracking-tight">
              Start your 7-day free trial
            </h2>
            <p className="mt-2 text-sm text-slate-600">
              No credit card required. Setup takes less than a minute.
            </p>
          </div>

          <div className="mt-8">
            <form className="space-y-5" onSubmit={handleSubmit}>
              {error && (
                <div className="bg-red-50 border-l-4 border-red-500 p-4 rounded-r-md">
                  <p className="text-sm text-red-700 font-medium">{error}</p>
                </div>
              )}
              
              <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                <div>
                  <label className="block text-sm font-medium text-slate-700">Owner Name</label>
                  <div className="mt-1 relative rounded-md shadow-sm">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                      <User className="h-5 w-5 text-slate-400" />
                    </div>
                    <input
                      type="text"
                      required
                      className="focus:ring-green-500 focus:border-green-500 block w-full bg-white text-slate-900 pl-10 sm:text-sm border-slate-300 rounded-md py-2 border transition-colors"
                      placeholder="John Doe"
                      value={formData.ownerName}
                      onChange={(e) => setFormData({...formData, ownerName: e.target.value})}
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-700">Shop Name</label>
                  <div className="mt-1 relative rounded-md shadow-sm">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                      <Store className="h-5 w-5 text-slate-400" />
                    </div>
                    <input
                      type="text"
                      required
                      className="focus:ring-green-500 focus:border-green-500 block w-full bg-white text-slate-900 pl-10 sm:text-sm border-slate-300 rounded-md py-2 border transition-colors"
                      placeholder="My Supermarket"
                      value={formData.shopName}
                      onChange={(e) => setFormData({...formData, shopName: e.target.value})}
                    />
                  </div>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700">Email address</label>
                <div className="mt-1 relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Mail className="h-5 w-5 text-slate-400" />
                  </div>
                  <input
                    type="email"
                    required
                    className="focus:ring-green-500 focus:border-green-500 block w-full bg-white text-slate-900 pl-10 sm:text-sm border-slate-300 rounded-md py-2 border transition-colors"
                    placeholder="john@example.com"
                    value={formData.email}
                    onChange={(e) => setFormData({...formData, email: e.target.value})}
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700">Mobile Number</label>
                <div className="mt-1 relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Phone className="h-5 w-5 text-slate-400" />
                  </div>
                  <input
                    type="tel"
                    required
                    className="focus:ring-green-500 focus:border-green-500 block w-full bg-white text-slate-900 pl-10 sm:text-sm border-slate-300 rounded-md py-2 border transition-colors"
                    placeholder="9876543210"
                    value={formData.mobileNumber}
                    onChange={(e) => setFormData({...formData, mobileNumber: e.target.value})}
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700">Address</label>
                <div className="mt-1 relative rounded-md shadow-sm">
                  <div className="absolute top-2 left-0 pl-3 flex items-start pointer-events-none">
                    <MapPin className="h-5 w-5 text-slate-400" />
                  </div>
                  <textarea
                    required
                    rows="2"
                    className="focus:ring-green-500 focus:border-green-500 block w-full bg-white text-slate-900 pl-10 sm:text-sm border-slate-300 rounded-md py-2 border transition-colors"
                    placeholder="123 Main St, City"
                    value={formData.address}
                    onChange={(e) => setFormData({...formData, address: e.target.value})}
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700">Shop Logo (Optional)</label>
                <div className="mt-1 relative rounded-md shadow-sm">
                  <input
                    type="file"
                    accept="image/*"
                    onChange={handleLogoUpload}
                    className="focus:ring-green-500 focus:border-green-500 block w-full bg-white text-slate-600 sm:text-sm border-slate-300 rounded-md py-2 px-3 border transition-colors file:mr-4 file:py-1 file:px-3 file:rounded-full file:border-0 file:text-xs file:font-semibold file:bg-green-50 file:text-green-700 hover:file:bg-green-100"
                  />
                  {formData.logoBase64 && (
                    <div className="mt-3 flex justify-center border border-slate-200 rounded-md p-2 bg-slate-50">
                      <img src={formData.logoBase64} alt="Logo Preview" className="h-16 w-auto object-contain rounded" />
                    </div>
                  )}
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700">Password</label>
                <div className="mt-1 relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Lock className="h-5 w-5 text-slate-400" />
                  </div>
                  <input
                    type="password"
                    required
                    minLength="6"
                    className="focus:ring-green-500 focus:border-green-500 block w-full bg-white text-slate-900 pl-10 sm:text-sm border-slate-300 rounded-md py-2 border transition-colors"
                    placeholder="••••••••"
                    value={formData.password}
                    onChange={(e) => setFormData({...formData, password: e.target.value})}
                  />
                </div>
                <p className="mt-1 text-xs text-slate-500">Must be at least 6 characters.</p>
              </div>

              <div className="pt-2">
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="w-full flex justify-center py-2.5 px-4 border border-transparent rounded-lg shadow-sm text-sm font-semibold text-white bg-green-600 hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 disabled:opacity-50 transition-all hover:shadow-md"
                >
                  {isSubmitting ? 'Creating account...' : 'Create Account'}
                  {!isSubmitting && <UserPlus className="ml-2 h-5 w-5" />}
                </button>
              </div>
              
              <div className="text-center text-sm pt-2">
                <span className="text-slate-600">Already have an account? </span>
                <Link to="/login" className="font-semibold text-green-600 hover:text-green-500 transition-colors">
                  Sign in
                </Link>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
