# Community Hero AI API

Base URL: `http://localhost:8080/api`

## Authentication

The frontend uses Firebase Google sign-in and sends the Firebase ID token as a
Bearer token with every API request. The backend verifies the token with the
Firebase Admin SDK before serving protected endpoints.

Current route behavior:

- Citizen pages require a signed-in Google account.
- Admin authority pages require a signed-in, email-verified Google account whose email is listed in both `VITE_ADMIN_EMAILS` and the backend `ADMIN_EMAILS` allowlist.
- Citizen endpoints require any signed-in, email-verified Firebase account.
- `/api/health`, `/uploads/**`, preflight requests, and the error handler remain public.

Frontend role checks are only navigation controls. The backend `ADMIN_EMAILS`
allowlist is the security boundary for admin operations.

## Authority Status Workflow

Frontend admin portal: `http://localhost:5173/admin/authorities`

## Civic Case Manager Agent

The bounded agent reuses the saved Gemini analysis and autonomously gathers
issue context, evidence validation, nearby duplicates, ward health, community
verification, and workflow history. It stores concise tool observations, not
private chain-of-thought. Every consequential recommendation requires admin
approval and is executed through the existing authority workflow.

Citizen-safe latest summary:

`GET /issues/{id}/agent/public-summary`

Admin endpoints:

- `GET /admin/issues/{id}/agent-runs`
- `GET /admin/issues/{id}/agent-runs/latest`
- `POST /admin/issues/{id}/agent-runs`
- `POST /admin/issues/{id}/agent-runs/{runId}/approve`
- `POST /admin/issues/{id}/agent-runs/{runId}/reject`

Approval and rejection requests contain `actorName`, optional `note`, and
optional `evidenceUrl`. The agent cannot directly send email, delete evidence,
resolve a case, or bypass allowed status transitions.

### Get workflow state

`GET /issues/{id}/status-workflow`

Returns:

```json
{
  "workflowEnabled": true,
  "currentStatus": "ESCALATED",
  "allowedTransitions": ["IN_PROGRESS"],
  "history": [
    {
      "id": 1,
      "fromStatus": "VERIFIED",
      "toStatus": "ESCALATED",
      "actorName": "Community Hero AI",
      "actorType": "SYSTEM",
      "note": "Complaint emailed to configured authority: roads@example.gov",
      "evidenceUrl": null,
      "createdAt": "2026-06-22T10:15:30"
    }
  ],
  "warning": null
}
```

### Update status

`PATCH /issues/{id}/status`

Request:

```json
{
  "targetStatus": "IN_PROGRESS",
  "actorName": "Ward Officer",
  "note": "Inspection team assigned and repair work order opened.",
  "evidenceUrl": "https://example.com/work-order/123"
}
```

Allowed authority transitions:

- `REPORTED -> ESCALATED`
- `VERIFIED -> ESCALATED`
- `ESCALATED -> IN_PROGRESS`
- `IN_PROGRESS -> RESOLVED`

Notes:

- `REPORTED -> VERIFIED` happens automatically after three community verifications.
- `ESCALATED` can also happen after a successful authority email.
- `POST /api/issues/{id}/authority-email/emergency-request` returns `confirmationRequired: true` when AI does not consider the issue urgent. Resubmit with `overrideAiAssessment: true` to force citizen escalation; the override is recorded in status history.
- Email delivery uses the Resend HTTPS API. Emergency requests are sent to the category-specific `AUTHORITY_EMAIL_*` recipient and use the citizen email as `Reply-To`.
- The endpoint can be disabled with `AUTHORITY_WORKFLOW_ENABLED=false`, but it is enabled by default for the admin portal.
- Authority mutations are protected by backend Firebase verification and the backend admin allowlist.

### Resolution Certificate

`GET /issues/{id}/certificate`

Returns a public accountability certificate for an issue. The response includes issue details, final status, resolution note, evidence URL, public status timeline, SLA assessment, community verification count, and the latest tamper-evident ledger hash. Certificates are marked official only after the issue reaches `RESOLVED`; unresolved issues return a preview.

Frontend route: `/issues/{id}/certificate`

## Gamification

Frontend leaderboard: `http://localhost:5173/leaderboard`

### Leaderboard

`GET /gamification/leaderboard`

Returns ranked contributors with points, report counts, verification counts, community-verified report bonuses, and badges.

Point model:

- `20` points for reporting an issue
- `10` points for verifying an issue
- `25` bonus points when one of your reports becomes community verified

### Summary

`GET /gamification/summary`

Returns total contributors, total points awarded, reports scored, and verifications scored.

Current implementation uses reporter/verifier display name and optional email as a lightweight demo identity. It does not require login.

## Zero Trust Civil Ledger

### Verify ledger integrity

`GET /ledger/integrity`

Admin only. Recalculates the tamper-evident civic audit chain. New entries use
HMAC-SHA-256 with a server-only secret; legacy SHA-256 entries are migrated at
backend startup after their existing chain has been verified.

Ledger events currently recorded:

- Issue status history entries.
- Community verification submissions.

Response:

```json
{
  "valid": true,
  "totalEntries": 12,
  "compromisedEntryId": null,
  "lastHash": "a public verification hash",
  "message": "System integrity verified. Civic ledger hash chain is intact."
}
```

If any ledger row is manually changed in the database, recalculation fails and the response returns `valid: false`. Certificate pages may show the latest hash as a public receipt; this does not expose the HMAC secret or permit a user to forge a valid replacement chain.

## Map Intelligence

Frontend map: `http://localhost:5173/map`

The map uses OpenStreetMap tiles through Leaflet and does not require a paid maps API key.

Features:

- Browser current-location centering when the user grants permission.
- Marker clustering through `leaflet.markercluster`.
- Density heatmaps through `leaflet.heat`.
- Layer modes for `Clusters + Heat`, `Clusters`, and `Heatmap`.

### Report location lookup

- `GET /api/location/reverse?latitude={latitude}&longitude={longitude}` resolves a confirmed map coordinate into address fields.
- `GET /api/location/search?query={place or address}` returns up to five Indian place matches with coordinates and address details.

The report form rejects browser readings with an accuracy radius above 1 km, offers place/landmark search, and requires the citizen to confirm the map pin before submission.

## Google Cloud Vision Image Validation

When citizens upload image evidence, the report page calls the backend Google Cloud Vision integration before submission. Vision labels and localized objects are mapped to the selected civic category.

Pre-submit validation endpoint:

`POST /media/validate-image`

Multipart fields:

- `file`: JPG, PNG, or WebP image
- `category`: `POTHOLE`, `WATER_LEAKAGE`, `STREETLIGHT_DAMAGE`, `WASTE_MANAGEMENT`, or `DRAINAGE_ISSUE`

The browser sends the validation metadata with the media upload:

`POST /issues/{issueId}/media`

Uploaded media bytes are persisted in the private Google Cloud Storage bucket configured by `MEDIA_GCS_BUCKET`. The API stores metadata in MySQL and serves bucket objects through `/uploads/{storageKey}`; the Cloud Run filesystem is not used.

Multipart fields:

- `files`: one or more image/video files
- `validationResults`: optional JSON array aligned with `files`

Example `validationResults` item:

```json
{
  "validationStatus": "VALID",
  "validationConfidence": 78,
  "validationSummary": "Google Cloud Vision found visual evidence consistent with the selected civic category.",
  "validationLabels": "Road surface 92%, Asphalt 88%, Water 78%",
  "validatedAt": "2026-06-23T10:00:00.000Z"
}
```

Validation statuses:

- `VALID`: Google Cloud Vision labels match the selected civic category.
- `SUSPECT`: Vision labels do not strongly match; manual review recommended.
- `UNAVAILABLE`: Google Cloud Vision key is not configured.
- `FAILED`: Vision request failed.
- `NOT_APPLICABLE`: media is video, not image.

Required environment variable:

- `GOOGLE_CLOUD_VISION_API_KEY`

No Gemini key, Hugging Face hosted inference token, or browser model download is required for this image validation path. Validation metadata is returned inside issue media responses.
