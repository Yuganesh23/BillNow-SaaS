import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { forgotPassword, resetPassword } from '../api/authApi';
import { Mail, KeyRound, ArrowLeft, CheckCircle2 } from 'lucide-react';

export default function ForgotPassword() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const urlEmail = searchParams.get('email') || '';
  const urlCode = searchParams.get('code') || searchParams.get('token') || '';

  const [step, setStep] = useState(urlCode ? 2 : 1);
  const [email, setEmail] = useState(urlEmail);
  const [code, setCode] = useState(urlCode);
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    if (urlEmail) setEmail(urlEmail);
    if (urlCode) {
      setCode(urlCode);
      setStep(2);
    }
  }, [urlEmail, urlCode]);

  // Step 1: Send reset link & OTP
  const handleSendResetEmail = async (e) => {
    e.preventDefault();
    if (!email) {
      setError('Please enter your registered email address.');
      return;
    }
    setLoading(true);
    setError('');
    setSuccess('');
    try {
      await forgotPassword(email);
      setSuccess(`Password reset link and verification code sent to ${email}. Check your inbox!`);
      setStep(2);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to send reset email. Please verify your address.');
    } finally {
      setLoading(false);
    }
  };

  // Step 2: Reset password using code/token
  const handleResetPassword = async (e) => {
    e.preventDefault();
    if (!code) {
      setError('Please enter the 6-digit verification code or mobile number.');
      return;
    }
    if (newPassword.length < 6) {
      setError('Password must be at least 6 characters long.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setLoading(true);
    setError('');
    setSuccess('');
    try {
      await resetPassword({
        email,
        token: code,
        mobileNumber: code,
        newPassword
      });
      setSuccess('Password reset successfully! Redirecting to login in 2 seconds...');
      setTimeout(() => {
        navigate('/login');
      }, 2000);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Invalid or expired code. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="flex justify-center">
          <div className="w-12 h-12 bg-indigo-600 rounded-xl flex items-center justify-center shadow-md">
            <span className="text-white font-bold text-2xl">B</span>
          </div>
        </div>
        <h2 className="mt-6 text-center text-3xl font-extrabold text-slate-900">
          {step === 1 ? 'Reset Your Password' : 'Set New Password'}
        </h2>
        <p className="mt-2 text-center text-sm text-slate-600">
          {step === 1 
            ? 'Enter your registered email to receive a password reset link and code' 
            : `Enter the code sent to ${email} and set your new password`}
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-6 shadow-sm border border-slate-200 rounded-xl sm:px-10">
          {error && (
            <div className="mb-4 bg-red-50 border-l-4 border-red-500 p-3 rounded text-sm text-red-700">
              {error}
            </div>
          )}
          {success && (
            <div className="mb-4 bg-green-50 border-l-4 border-green-500 p-3 rounded text-sm text-green-700 flex items-start gap-2">
              <CheckCircle2 className="w-5 h-5 flex-shrink-0 mt-0.5 text-green-600" />
              <span>{success}</span>
            </div>
          )}

          {step === 1 ? (
            <form className="space-y-5" onSubmit={handleSendResetEmail}>
              <div>
                <label htmlFor="email" className="block text-sm font-medium text-slate-700">
                  Email address
                </label>
                <div className="mt-1 relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                    <Mail className="h-4 w-4" />
                  </div>
                  <input
                    id="email"
                    name="email"
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="owner@yourshop.com"
                    className="block w-full pl-10 pr-3 py-2 border border-slate-300 rounded-lg shadow-sm placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 sm:text-sm"
                  />
                </div>
              </div>

              <div>
                <button
                  type="submit"
                  disabled={loading}
                  className={`w-full flex justify-center py-2.5 px-4 border border-transparent rounded-lg shadow-sm text-sm font-semibold text-white ${
                    loading ? 'bg-indigo-400' : 'bg-indigo-600 hover:bg-indigo-700'
                  } focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500`}
                >
                  {loading ? 'Sending link...' : 'Send Reset Link & Code'}
                </button>
              </div>

              <div className="text-center pt-2">
                <button
                  type="button"
                  onClick={() => setStep(2)}
                  className="text-xs text-indigo-600 hover:text-indigo-500 font-medium"
                >
                  Already have a verification code? Enter it here &rarr;
                </button>
              </div>
            </form>
          ) : (
            <form className="space-y-5" onSubmit={handleResetPassword}>
              <div>
                <label htmlFor="email" className="block text-sm font-medium text-slate-700">
                  Email address
                </label>
                <input
                  id="email"
                  name="email"
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="mt-1 block w-full px-3 py-2 border border-slate-300 rounded-lg shadow-sm bg-slate-50 text-slate-600 sm:text-sm"
                />
              </div>

              <div>
                <label htmlFor="code" className="block text-sm font-medium text-slate-700">
                  6-Digit Code (or Mobile Number)
                </label>
                <div className="mt-1 relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                    <KeyRound className="h-4 w-4" />
                  </div>
                  <input
                    id="code"
                    name="code"
                    type="text"
                    required
                    value={code}
                    onChange={(e) => setCode(e.target.value)}
                    placeholder="123456"
                    className="block w-full pl-10 pr-3 py-2 border border-slate-300 rounded-lg shadow-sm placeholder-slate-400 tracking-widest font-mono focus:outline-none focus:ring-2 focus:ring-indigo-500 sm:text-sm"
                  />
                </div>
              </div>

              <div>
                <label htmlFor="newPassword" className="block text-sm font-medium text-slate-700">
                  New Password
                </label>
                <input
                  id="newPassword"
                  name="newPassword"
                  type="password"
                  required
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="At least 6 characters"
                  className="mt-1 block w-full px-3 py-2 border border-slate-300 rounded-lg shadow-sm placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 sm:text-sm"
                />
              </div>

              <div>
                <label htmlFor="confirmPassword" className="block text-sm font-medium text-slate-700">
                  Confirm New Password
                </label>
                <input
                  id="confirmPassword"
                  name="confirmPassword"
                  type="password"
                  required
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Repeat new password"
                  className="mt-1 block w-full px-3 py-2 border border-slate-300 rounded-lg shadow-sm placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 sm:text-sm"
                />
              </div>

              <div>
                <button
                  type="submit"
                  disabled={loading}
                  className={`w-full flex justify-center py-2.5 px-4 border border-transparent rounded-lg shadow-sm text-sm font-semibold text-white ${
                    loading ? 'bg-indigo-400' : 'bg-indigo-600 hover:bg-indigo-700'
                  } focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500`}
                >
                  {loading ? 'Updating Password...' : 'Save New Password'}
                </button>
              </div>

              <div className="text-center pt-2">
                <button
                  type="button"
                  onClick={() => setStep(1)}
                  className="text-xs text-slate-500 hover:text-slate-700 font-medium inline-flex items-center gap-1"
                >
                  <ArrowLeft className="w-3 h-3" /> Request a new code
                </button>
              </div>
            </form>
          )}

          <div className="mt-6 border-t border-slate-200 pt-4 text-center">
            <Link to="/login" className="text-sm font-medium text-indigo-600 hover:text-indigo-500">
              Back to Sign In
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
