# BillNow Frontend

This is the React frontend prototype V1 for the BillNow Spring Boot backend.
It is built with React, Vite, Tailwind CSS, React Router, and Axios.

## Project Structure

```text
frontend/
├── src/
│   ├── api/            # API client and service endpoints
│   ├── components/     # Reusable UI components and layouts
│   ├── context/        # React Context (Auth)
│   ├── mock/           # Isolated mock adapters for prototype mode
│   ├── pages/          # Main route components
│   ├── utils/          # Utility functions (e.g. calculations)
│   ├── App.jsx         # App routing and providers
│   └── main.jsx        # Entry point
├── .env                # Environment variables
├── .env.example        # Example environment variables
├── package.json        
└── vite.config.js
```

## Features Implemented
- **Authentication**: JWT-based login integration.
- **Dashboard**: High-level KPIs and recent invoices.
- **Products**: CRUD for product inventory.
- **Customers**: CRUD for customer directory.
- **Invoices**: Create, list, cancel, and view detailed invoices.
- **Payments**: Placeholder for future Razorpay integration.
- **Reports**: Placeholder for business insights.
- **Settings**: Business profile management.

## Environment Variables

Copy `.env.example` to `.env`:

```env
VITE_API_BASE_URL=
VITE_USE_MOCK_API=false
```

- Set `VITE_USE_MOCK_API=true` during UI development if the backend is unavailable. This will use an isolated mock adapter without cluttering components with fake data.

## Installation & Setup

1. Install dependencies:
   ```bash
   npm install
   ```

2. Run development server:
   ```bash
   npm run dev
   ```

3. Build for production:
   ```bash
   npm run build
   npm run preview
   ```

## Backend Dependency

This frontend is designed to consume the BillNow Spring Boot backend.
During local development, Vite proxies `/api` to the backend on `http://localhost:8083`. On Vercel, leave `VITE_API_BASE_URL` blank and set the server-side `BACKEND_ORIGIN` variable to the Railway public URL.

## Known Limitations & V2 Recommendations
- PDF generation currently relies on UI/Print or a future backend endpoint `/api/invoices/{id}/pdf`.
- Payment integration (Razorpay) is mapped in the backend but needs UI integration in the next iteration.
- Validation could be further enhanced using a library like `react-hook-form` and `zod`.
