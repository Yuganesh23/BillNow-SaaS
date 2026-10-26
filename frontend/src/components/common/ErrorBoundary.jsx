import React from 'react';

export class ErrorBoundary extends React.Component {
  state = { failed: false, error: null };

  static getDerivedStateFromError(error) {
    return { failed: true, error };
  }

  componentDidCatch(error, info) {
    console.error('Application render failed', error, info);
  }

  render() {
    if (this.state.failed) {
      return (
        <main className="min-h-screen grid place-items-center bg-slate-50 px-6">
          <section className="max-w-md rounded-2xl bg-white p-8 text-center shadow-lg">
            <h1 className="text-2xl font-bold text-slate-900">Something went wrong</h1>
            <p className="mt-3 text-slate-600">Your data is safe. Reload the page to continue.</p>
            <button
              onClick={() => window.location.reload()}
              className="mt-6 rounded-lg bg-indigo-600 px-5 py-2.5 font-semibold text-white hover:bg-indigo-700"
            >
              Reload App
            </button>
          </section>
        </main>
      );
    }
    return this.props.children;
  }
}
