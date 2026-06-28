# Community Hero AI Frontend

## Run locally
```bash
cd frontend
npm install
npm run dev
```

Copy `.env.example` to `.env` and set `VITE_API_BASE_URL`.

## Firebase Google sign-in

Create a Firebase project, add a Web App, enable Authentication -> Google provider, then set:

```env
VITE_FIREBASE_API_KEY=...
VITE_FIREBASE_AUTH_DOMAIN=...
VITE_FIREBASE_PROJECT_ID=...
VITE_FIREBASE_APP_ID=...
VITE_ADMIN_EMAILS=admin1@gmail.com,admin2@gmail.com
```

Citizen pages require Google sign-in. Admin pages require Google sign-in plus an email listed in `VITE_ADMIN_EMAILS`.
