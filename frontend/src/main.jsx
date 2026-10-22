import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import { ErrorBoundary } from './components/common/ErrorBoundary.jsx'
import { reportFrontendError } from './utils/telemetry.js'

window.addEventListener('error', (event) => reportFrontendError(event.error || event.message, { source: 'window' }))
window.addEventListener('unhandledrejection', (event) => reportFrontendError(event.reason, { source: 'promise' }))

if ('serviceWorker' in navigator && import.meta.env.PROD) {
  window.addEventListener('load', () => navigator.serviceWorker.register('/sw.js'))
}

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <ErrorBoundary><App /></ErrorBoundary>
  </StrictMode>,
)
