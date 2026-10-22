# Deploy on Cloud Run, Firebase Hosting and Neon

This is the alternative to the Railway + Vercel setup in the README:

- **Neon** PostgreSQL (database)
- **Cloud Run** service `billnow-api` (Spring Boot API, built from the root `Dockerfile`)
- **Firebase Hosting** (React frontend), which forwards `/api/**` to Cloud Run, so the browser talks to one origin

Commands assume region `asia-south1` (Mumbai). If you use another region, change it everywhere, including `frontend/firebase.json`.

## 1. Neon database

1. Create a Neon project, preferably in **AWS Asia Pacific (Mumbai)** to sit close to `asia-south1`.
2. Copy the **pooled** connection string (host contains `-pooler`):
   `postgresql://ROLE:PASSWORD@ep-xxxx-pooler.ap-south-1.aws.neon.tech/neondb?sslmode=require`
3. Leave the database empty. On first start the API creates the whole schema (from `db/bootstrap/schema.sql`) and records it in `flyway_schema_history`. Later migrations then apply on each deploy.

Before every release that adds a migration, create a Neon branch of production as a restore point.

## 2. Secrets

Use one Google Cloud project for Cloud Run and Firebase.

```bash
PROJECT_ID=your-project-id
REGION=asia-south1
gcloud config set project $PROJECT_ID
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com secretmanager.googleapis.com

# Generate once and keep them stable: changing JWT_SECRET signs everyone out,
# changing WHATSAPP_TOKEN_ENCRYPTION_KEY makes stored WhatsApp tokens unreadable.
printf '%s' "postgresql://ROLE:PASSWORD@HOST-pooler/neondb?sslmode=require" | gcloud secrets create DATABASE_URL --data-file=-
openssl rand -base64 48 | tr -d '\n' | gcloud secrets create JWT_SECRET --data-file=-
openssl rand -base64 48 | tr -d '\n' | gcloud secrets create OTP_HMAC_SECRET --data-file=-
openssl rand -base64 32 | tr -d '\n' | gcloud secrets create WHATSAPP_TOKEN_ENCRYPTION_KEY --data-file=-
printf '%s' "RAZORPAY_KEY_SECRET_VALUE" | gcloud secrets create RAZORPAY_KEY_SECRET --data-file=-
printf '%s' "RAZORPAY_WEBHOOK_SECRET_VALUE" | gcloud secrets create RAZORPAY_WEBHOOK_SECRET --data-file=-
printf '%s' "SMTP_PASSWORD" | gcloud secrets create MAIL_PASSWORD --data-file=-

# Let the Cloud Run runtime service account read them.
SA=$(gcloud projects describe $PROJECT_ID --format='value(projectNumber)')-compute@developer.gserviceaccount.com
for s in DATABASE_URL JWT_SECRET OTP_HMAC_SECRET WHATSAPP_TOKEN_ENCRYPTION_KEY RAZORPAY_KEY_SECRET RAZORPAY_WEBHOOK_SECRET MAIL_PASSWORD; do
  gcloud secrets add-iam-policy-binding $s --member="serviceAccount:$SA" --role=roles/secretmanager.secretAccessor
done
```

## 3. Cloud Run API

Firebase gives the site `https://PROJECT_ID.web.app` and `https://PROJECT_ID.firebaseapp.com`. Use your custom domain instead if you add one.

```bash
SITE=https://$PROJECT_ID.web.app

gcloud run deploy billnow-api \
  --source . \
  --region $REGION \
  --allow-unauthenticated \
  --memory 1Gi --cpu 1 \
  --min-instances 0 --max-instances 4 \
  --set-secrets "DATABASE_URL=DATABASE_URL:latest,JWT_SECRET=JWT_SECRET:latest,OTP_HMAC_SECRET=OTP_HMAC_SECRET:latest,WHATSAPP_TOKEN_ENCRYPTION_KEY=WHATSAPP_TOKEN_ENCRYPTION_KEY:latest,RAZORPAY_KEY_SECRET=RAZORPAY_KEY_SECRET:latest,RAZORPAY_WEBHOOK_SECRET=RAZORPAY_WEBHOOK_SECRET:latest,MAIL_PASSWORD=MAIL_PASSWORD:latest" \
  --set-env-vars "^@^SPRING_PROFILES_ACTIVE=prod@AUTH_COOKIE_MODE=session@AUTH_COOKIE_SECURE=true@AUTH_COOKIE_SAME_SITE=Lax@FRONTEND_URL=$SITE@CORS_ALLOWED_ORIGINS=$SITE,https://$PROJECT_ID.firebaseapp.com@RAZORPAY_KEY_ID=rzp_live_or_test_id@RAZORPAY_MONTHLY_PLAN_ID=plan_xxx@RAZORPAY_YEARLY_PLAN_ID=plan_xxx@RAZORPAY_BASE_YEARLY_PLAN_ID=plan_xxx@RAZORPAY_PRO_YEARLY_PLAN_ID=plan_xxx@RAZORPAY_ENTERPRISE_YEARLY_PLAN_ID=plan_xxx@MAIL_HOST=smtp.example.com@MAIL_PORT=587@MAIL_USERNAME=apikey@MAIL_FROM=billing@example.com@MAIL_SMTP_AUTH=true@MAIL_STARTTLS=true"
```

Notes:

- `AUTH_COOKIE_MODE=session` is **required** behind Firebase Hosting. Firebase forwards only the cookie named `__session` to Cloud Run; with the default mode users sign in and are immediately signed out.
- `--allow-unauthenticated` is required for Firebase Hosting rewrites. The API enforces its own authentication.
- The prod profile fails at startup if `JWT_SECRET`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET` or `WHATSAPP_TOKEN_ENCRYPTION_KEY` is missing, and every `RAZORPAY_*_PLAN_ID` must be set, even to a placeholder, until billing is configured.
- Add the subscription GST variables once known: `BILLING_SELLER_LEGAL_NAME`, `BILLING_SELLER_GSTIN`, `BILLING_SELLER_ADDRESS`, `BILLING_SAAS_SAC_CODE` (see `docs/SUBSCRIPTION_BILLING.md`).
- Cloud Run sets `PORT`; the app reads it. Startup takes about 15–25 s, so with `--min-instances 0` the first request after idle is slow. Use `--min-instances 1` for production.
- Keep `DB_POOL_SIZE` (default 5) × `--max-instances` within your Neon plan's connection limit.

Check it: `curl https://billnow-api-xxxxx.a.run.app/actuator/health` → `{"status":"UP"}`. The first start on an empty database logs `Empty database: creating schema from db/bootstrap/schema.sql`.

## 4. Firebase Hosting frontend

`frontend/firebase.json` serves `dist/`, sends `/api/**` to the Cloud Run service `billnow-api` in `asia-south1`, falls back to `index.html` for client routes, and sets the same security headers as `vercel.json`.

```bash
npm install -g firebase-tools
firebase login
firebase projects:addfirebase $PROJECT_ID   # once, if the GCP project is not yet a Firebase project

cd frontend
firebase use --add $PROJECT_ID
npm ci
npm run build          # leave VITE_API_BASE_URL unset so the app calls /api on the same origin
firebase deploy --only hosting
```

## 5. Razorpay

Point the webhook at the Cloud Run URL directly, not through Firebase:

`https://billnow-api-xxxxx.a.run.app/api/webhooks/razorpay`

Enable the events listed in `docs/SUBSCRIPTION_BILLING.md` and use the same secret as `RAZORPAY_WEBHOOK_SECRET`.

## 6. Verify

1. Open `https://PROJECT_ID.web.app`, register a shop and sign in.
2. In browser developer tools, `/api/auth/login` is called on the `web.app` origin and sets one `__session` cookie (HttpOnly, Secure).
3. Reload the dashboard and open another page; you stay signed in. After 15 minutes the app silently refreshes the session.
4. Create a customer and confirm it appears in the Neon table view.
5. In Neon, `flyway_schema_history` shows version `11` (Bootstrap schema) plus any later migrations.

## Updating

- API: rerun the `gcloud run deploy` command (secrets and variables are kept). Migrations run on start.
- Frontend: `npm run build && firebase deploy --only hosting` in `frontend`.
- Roll back the API with `gcloud run services update-traffic billnow-api --to-revisions=REVISION=100`; migrations are additive, so the previous revision keeps working.
