# Community Hero AI Backend

Spring loads database, Gemini, CORS, and Cloud Storage settings from `backend/.env`.

## Issue media

- `POST /api/issues/{issueId}/media` accepts multipart field `files`.
- `GET /api/issues/{issueId}/media` lists attachments.
- `DELETE /api/issues/{issueId}/media/{mediaId}` removes an attachment and its Cloud Storage object.

Each issue supports up to five JPG/PNG/WebP images at 5 MB each and one MP4/WebM video at 50 MB. File bytes are stored in the private bucket configured by `MEDIA_GCS_BUCKET`; generated metadata and playback addresses are stored in MySQL. Citizens never enter media URLs manually.

Google Cloud Storage is mandatory. The backend serves private objects through `/uploads/{storageKey}` so bucket objects do not need public access.

## Location detection

- `GET /api/location/reverse?latitude={lat}&longitude={lng}` resolves coordinates into country, state, district, city, locality, postal code, road, and formatted address.
- Browser GPS and map pins remain the authoritative coordinates.
- Ward is optional and falls back to `UNASSIGNED` because public reverse geocoders cannot guarantee municipal ward boundaries.

## Authority email escalation

- `GET /api/issues/{issueId}/authority-email/preview` returns the configured recipient and exact complaint.
- `POST /api/issues/{issueId}/authority-email/send` requires an unchanged, explicitly confirmed preview.
- Recipients are selected by issue category from `AUTHORITY_EMAIL_*` variables; the client cannot choose arbitrary addresses.
- Non-urgent emergency requests require a second explicit citizen confirmation. Confirmed overrides bypass the AI urgency gate and are recorded in status history.
- Email delivery uses the Resend HTTPS API. `RESEND_API_KEY` remains backend-only, while `RESEND_FROM` controls the sender shown in the admin inbox.
- Emergency messages use the citizen email as `Reply-To`; the configured authority address remains the recipient.
- Sending is disabled by default with `EMAIL_SENDING_ENABLED=false` and successful sends are limited to one per issue every 24 hours.
- Every attempt is recorded in `issue_email_logs`; successful delivery advances reported or verified issues to `ESCALATED`.

## Authority status workflow

- `GET /api/issues/{issueId}/status-workflow` returns whether the authority workflow is enabled, the current status, allowed next statuses, and public status history.
- `PATCH /api/issues/{issueId}/status` advances an issue through approved transitions only.
- The frontend admin authority portal is available at `/admin/authorities`.
- Allowed authority transitions are `REPORTED -> ESCALATED`, `VERIFIED -> ESCALATED`, `ESCALATED -> IN_PROGRESS`, and `IN_PROGRESS -> RESOLVED`.
- Community verification still controls `REPORTED -> VERIFIED`; email escalation can still move `REPORTED` or `VERIFIED` to `ESCALATED`.
- Authority workflow is enabled by default. Set `AUTHORITY_WORKFLOW_ENABLED=false` only when you intentionally want to hide admin status controls.

## Gamification

- `GET /api/gamification/leaderboard` returns ranked citizen contributors.
- `GET /api/gamification/summary` returns total contributors, reports, verifications, and points awarded.
- Points are derived from existing civic actions: 20/report, 10/verification, and 25 bonus points when a report becomes community verified.
- Firebase provides the signed-in account identity. Contributor display names and optional emails remain the attribution fields used by the points model.

## Zero Trust civil ledger

- `GET /api/ledger/integrity` is admin-only and recalculates the HMAC-SHA-256 audit chain.
- Status history and community verification events are appended to `civic_audit_ledger`.
- Each ledger entry stores the previous hash and its own keyed hash, making direct database tampering detectable without giving database-only attackers enough information to forge a replacement chain.
- Store `LEDGER_HMAC_SECRET` in Secret Manager and never expose it to the frontend. Public certificate hashes are verification receipts, not credentials.

## Cloud Run secrets

The deployed service reads `SPRING_DATASOURCE_PASSWORD`, `GEMINI_API_KEY`,
`GOOGLE_CLOUD_VISION_API_KEY`, `RESEND_API_KEY`, and `LEDGER_HMAC_SECRET` from
Google Secret Manager. Never redeploy these credentials with `--set-env-vars`.
The local `.env` file is excluded from Git, Cloud Build source uploads, and the
Docker image.

## Image evidence review

- Uploaded images are checked before submission with Google Cloud Vision Label/Object Detection.
- On Cloud Run, the backend uses Google Application Default Credentials from the Cloud Run service account.
- Set `GOOGLE_CLOUD_VISION_AUTH_MODE=auto` or `oauth`, enable the Cloud Vision API, and grant the Cloud Run service account a Vision-capable role such as `Cloud Vision AI User`.
- `GOOGLE_CLOUD_VISION_API_KEY` is only a local fallback; Cloud Vision may reject API-key auth for `images:annotate`.
- If Google Cloud Vision is unavailable, the image can still be attached and queued for manual authority review.
- Image media responses include `validationStatus`, `validationConfidence`, `validationSummary`, `validationLabels`, and `validatedAt`.
- The app flags suspicious images for manual review instead of deleting reports automatically.
