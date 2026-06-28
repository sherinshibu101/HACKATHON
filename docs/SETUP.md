# Community Hero AI Setup

## Backend environment

Create `backend/.env` from `backend/.env.example` and fill in your local values.

For Google Cloud Vision image validation on Cloud Run, prefer service-account authentication:

```properties
GOOGLE_CLOUD_VISION_AUTH_MODE=auto
```

Enable the Cloud Vision API in the same Google Cloud project and grant the Cloud Run runtime service account a Vision-capable role, for example `Cloud Vision AI User`. API-key auth can be used as a local fallback, but Cloud Vision may reject API keys for image annotation.

Authority workflow is enabled by default for the admin portal. To explicitly keep it enabled:

```properties
AUTHORITY_WORKFLOW_ENABLED=true
```

This enables admin status controls in the authority portal. Set `AUTHORITY_WORKFLOW_ENABLED=false` only if you intentionally want to hide status updates.

## Run locally

Backend:

```powershell
cd backend
mvn spring-boot:run
```

Frontend:

```powershell
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

## Firebase Google authentication

1. Go to Firebase Console.
2. Create or open a project.
3. Add a Web App.
4. Enable `Authentication -> Sign-in method -> Google`.
5. Add `localhost` in Firebase Authentication authorized domains if it is not already present.
6. Copy `frontend/.env.example` to `frontend/.env`.
7. Fill:

```properties
VITE_FIREBASE_API_KEY=your_firebase_api_key
VITE_FIREBASE_AUTH_DOMAIN=your_project.firebaseapp.com
VITE_FIREBASE_PROJECT_ID=your_project_id
VITE_FIREBASE_APP_ID=your_firebase_web_app_id
VITE_ADMIN_EMAILS=admin1@gmail.com,admin2@gmail.com
```

Configure the matching backend settings. Keep `LEDGER_HMAC_SECRET` on the
server only and use Secret Manager in Cloud Run:

```properties
FIREBASE_PROJECT_ID=your_project_id
ADMIN_EMAILS=admin1@gmail.com,admin2@gmail.com
LEDGER_HMAC_SECRET=generate_at_least_32_random_characters
```

Normal citizen pages require a verified Google account. Admin pages and APIs
require the Gmail to be listed in both `VITE_ADMIN_EMAILS` and backend
`ADMIN_EMAILS`. The backend verifies every Firebase ID token; frontend route
guards alone are not treated as authorization.

### Production secrets on Cloud Run

Store credentials in Google Secret Manager and bind them to Cloud Run as
secret-backed environment variables. The production service uses:

| Environment variable | Secret Manager secret |
| --- | --- |
| `SPRING_DATASOURCE_PASSWORD` | `cloud-sql-password` |
| `GEMINI_API_KEY` | `gemini-api-key` |
| `GOOGLE_CLOUD_VISION_API_KEY` | `google-cloud-vision-api-key` |
| `RESEND_API_KEY` | `resend-api-key` |
| `LEDGER_HMAC_SECRET` | `ledger-hmac-secret` |

Do not place these values in `--set-env-vars` during a redeployment. Existing
Cloud Run secret bindings are preserved when they are not explicitly replaced.
Database URLs and usernames, project IDs, bucket names, model names, CORS
origins, authority email addresses, feature flags, and provider base URLs are
ordinary configuration and do not need Secret Manager.

Frontend `VITE_*` variables are compiled into browser JavaScript and therefore
cannot be hidden with Secret Manager. Firebase web configuration, including its
web API key, is designed to be public; protect Firebase data and operations with
Firebase rules, authorized domains, and backend token verification.

Routes:

- Citizen app: `http://localhost:5173/`
- Admin authority portal: `http://localhost:5173/admin/authorities`

## Testing the full timeline

1. Report an issue.
2. Wait for AI analysis.
3. Submit three community verifications.
4. Configure Resend and send the authority email, or use the local authority workflow to escalate.
5. Open `http://localhost:5173/admin/authorities` or click the `Admin` role switch in the navbar.
6. Use the authority portal to move `ESCALATED -> IN_PROGRESS`.
7. Add a resolution note and optional evidence URL to move `IN_PROGRESS -> RESOLVED`.
8. Open `/issues/{id}/certificate` or click `View resolution certificate` to print/save the public proof.

The app now has two demo roles in the frontend:

- `Citizen`: reporting, map, issues, public dashboard, issue timeline, public audit history, and resolution certificates.
- `Admin`: authority inbox, official status updates, resolution notes, evidence links, and certificate review at `/admin/authorities`.

Admin access is selected from the configured frontend admin email allowlist after Google sign-in. Citizens do not see authority workflow controls.

## Persistent media storage

All uploaded photos and videos are stored in a private Google Cloud Storage bucket. Local filesystem storage is not supported.

```properties
MEDIA_GCS_BUCKET=your_globally_unique_bucket_name
MEDIA_GCS_PREFIX=evidence
```

The Cloud Run service account requires `roles/storage.objectUser` on the bucket. Media remains private in Cloud Storage and is served to the application through `/uploads/{storageKey}`.

## Resend email delivery

Create a Resend API key and configure the backend without putting the key in the frontend:

```properties
EMAIL_SENDING_ENABLED=true
RESEND_API_KEY=re_your_private_key
RESEND_FROM=Community Hero AI <onboarding@resend.dev>
AUTHORITY_EMAIL_POTHOLE=herocommunity96@gmail.com
AUTHORITY_EMAIL_WATER_LEAKAGE=herocommunity96@gmail.com
AUTHORITY_EMAIL_STREETLIGHT_DAMAGE=herocommunity96@gmail.com
AUTHORITY_EMAIL_WASTE_MANAGEMENT=herocommunity96@gmail.com
AUTHORITY_EMAIL_DRAINAGE_ISSUE=herocommunity96@gmail.com
```

The Resend test sender is suitable when sending to the email associated with the Resend account. Use a verified domain sender for production. Keep `EMAIL_SENDING_ENABLED=false` until the API key and sender have been configured.

## Testing gamification

1. Report an issue and enter a contributor display name.
2. Verify issues from the Issue Details page.
3. Open `http://localhost:5173/leaderboard`.
4. Confirm points and badges appear.

Scoring:

- `20` points per report.
- `10` points per verification.
- `25` bonus points when a report gets three verifications.

## Testing Zero Trust civil ledger

1. Report an issue or submit a community verification.
2. Open `http://localhost:5173/admin/authorities`.
3. Click `Run integrity check`.
4. A valid chain should show `Ledger verified`.

Only backend-authorized admins can run this check. Citizens can still see the
public status history and a certificate verification receipt, but cannot call
the full integrity endpoint. The receipt hash is safe to publish because the
HMAC signing secret is never returned to the browser.

Advanced tamper demo:

1. Manually edit a row in `civic_audit_ledger` in MySQL, such as changing `payload`.
2. Run the admin portal integrity check again.
3. The admin portal should show `Warning: audit log compromised!`.

## Testing map clustering and heatmaps

1. Open `http://localhost:5173/map`.
2. Allow browser location permission when prompted.
3. The map should center on your current location and show a teal current-location dot.
4. Use the layer buttons to switch between `Clusters + Heat`, `Clusters`, and `Heatmap`.
5. Zoom out to see nearby issue markers collapse into numbered clusters; zoom in to split them apart.

Desktop computers may return a coarse network-based location when GPS is unavailable. On the report page, readings less accurate than 1 km are not accepted automatically; search for a locality or landmark and confirm the draggable map pin instead.

## Testing Google Cloud Vision image validation

1. Enable the Cloud Vision API in Google Cloud.
2. Create an API key with access to Cloud Vision.
3. Add it to `backend/.env`:

```properties
GOOGLE_CLOUD_VISION_API_KEY=your_google_cloud_vision_api_key
```

4. Restart the backend.
5. Start the frontend with `npm run dev`.
6. Open `http://localhost:5173/report`.
7. Select or capture a photo.
8. The report page should show a Google Cloud Vision validation badge before submission.
9. Submit the report and check the issue details page; stored image evidence keeps the validation badge too.

Images are flagged as `VALID`, `SUSPECT`, `UNAVAILABLE`, or `FAILED`. Suspicious images are flagged for review instead of being deleted automatically.

## Reset and seed demo data

Use this only when you want to replace old civic issue data with a unified hackathon demo dataset.

1. Add these values to `backend/.env`:

```properties
DEMO_DATA_SEED_ON_START=true
DEMO_PRIMARY_EMAIL=your_logged_in_gmail@example.com
```

2. Start the backend once:

```powershell
cd backend
mvn spring-boot:run
```

3. After the backend starts successfully, stop it.
4. Change the flag back:

```properties
DEMO_DATA_SEED_ON_START=false
```

5. Start the backend again for the actual demo.

The seeder clears civic issue/demo tables, keeps Firebase/auth configuration untouched, creates demo media assets under `UPLOAD_DIR`, and inserts scenarios for citizen reporting, Google Vision validation, duplicate detection, community verification, AI analysis, authority workflow, email escalation, dashboard, leaderboard, map, and ledger integrity.
