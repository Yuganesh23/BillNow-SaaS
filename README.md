# BillNow SaaS

BillNow is a Spring Boot 4 / React 19 billing application. Its production setup targets:

- Neon PostgreSQL for the database
- Railway for the Spring Boot API
- Vercel for the Vite frontend and same-origin API proxy

The proxy is important: browser requests stay on the Vercel origin while Vercel forwards `/api/*` to Railway. This keeps the HttpOnly authentication cookie first-party instead of relying on third-party cookies between `vercel.app` and `railway.app`.

## 1. Create the Neon database

Create a Neon project and copy the pooled connection details. Railway needs these variables:

```env
DATABASE_URL=jdbc:postgresql://YOUR-POOLER-HOST/neondb?sslmode=require
DATABASE_USERNAME=YOUR_NEON_ROLE
DATABASE_PASSWORD=YOUR_NEON_PASSWORD
```

Use the host ending in `-pooler` and keep `sslmode=require`. The app limits its Hikari connection pool to five connections by default. The schema is created/updated by Hibernate on startup (`JPA_DDL_AUTO=update`).

## 2. Deploy the backend to Railway

Create a Railway project from this GitHub repository. Railway detects the root `Dockerfile`; `railway.json` configures `/actuator/health` as the deployment health check.

Add the following service variables, using `.env.example` as the complete template:

```env
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://YOUR-POOLER-HOST/neondb?sslmode=require
DATABASE_USERNAME=YOUR_NEON_ROLE
DATABASE_PASSWORD=YOUR_NEON_PASSWORD
JWT_SECRET=GENERATE_AT_LEAST_32_RANDOM_BYTES
AUTH_COOKIE_SECURE=true
AUTH_COOKIE_SAME_SITE=Lax
CORS_ALLOWED_ORIGINS=https://YOUR-PROJECT.vercel.app
```

Generate a Railway public domain after the service is healthy. Do not add or override `PORT`; Railway supplies it and the backend reads it automatically.

Razorpay and email values are optional for the initial boot, but must be replaced before those integrations are used. Never commit real values.

## 3. Deploy the frontend to Vercel

Import the same repository in Vercel and set the project Root Directory to `frontend`. Vercel reads `frontend/vercel.json`, builds the Vite app, preserves client-side routes, and runs the `/api/*` proxy function.

Add this server-side Vercel environment variable for Production and Preview:

```env
BACKEND_ORIGIN=https://YOUR-BACKEND.up.railway.app
```

Leave `VITE_API_BASE_URL` unset. Redeploy after changing an environment variable. Once Vercel assigns the final production URL, put that exact origin in Railway's `CORS_ALLOWED_ORIGINS` and redeploy the backend.

## 4. Verify production

1. Open `https://YOUR-BACKEND.up.railway.app/actuator/health` and confirm `{"status":"UP"}`.
2. Open the Vercel URL and register or sign in.
3. In browser developer tools, confirm `/api/auth/login` is requested from the Vercel domain and returns a `jwt` HttpOnly, Secure cookie.
4. Create a small test record and confirm it appears in the Neon table view.

## Local development

Run PostgreSQL locally, then start the backend:

```bash
./mvnw spring-boot:run
```

The Vite development server proxies `/api` to the backend on port `8083`:

```bash
cd frontend
npm ci
npm run dev
```

## Checks

```bash
./mvnw verify
cd frontend
npm ci
npm run lint
npm run build
```

GitHub Actions runs these checks on pushes to `main` and pull requests.
