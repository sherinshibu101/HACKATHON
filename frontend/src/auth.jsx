import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { setAuthTokenProvider } from './api'

const firebaseCdnVersion = '10.14.1'
const AuthContext = createContext(null)

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
}

const adminEmails = (import.meta.env.VITE_ADMIN_EMAILS || '')
  .split(',')
  .map(email => email.trim().toLowerCase())
  .filter(Boolean)

function hasFirebaseConfig() {
  return Boolean(firebaseConfig.apiKey && firebaseConfig.authDomain && firebaseConfig.projectId && firebaseConfig.appId)
}

async function loadFirebase() {
  const [{ initializeApp, getApps }, authModule] = await Promise.all([
    import(/* @vite-ignore */ `https://www.gstatic.com/firebasejs/${firebaseCdnVersion}/firebase-app.js`),
    import(/* @vite-ignore */ `https://www.gstatic.com/firebasejs/${firebaseCdnVersion}/firebase-auth.js`),
  ])
  const app = getApps().length ? getApps()[0] : initializeApp(firebaseConfig)
  return { app, ...authModule }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [authApi, setAuthApi] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!hasFirebaseConfig()) {
      setError('Firebase is not configured. Add VITE_FIREBASE_* values in frontend/.env.')
      setLoading(false)
      return undefined
    }
    let unsubscribe = null
    loadFirebase()
      .then(api => {
        const auth = api.getAuth(api.app)
        setAuthTokenProvider(() => auth.currentUser ? api.getIdToken(auth.currentUser) : null)
        setAuthApi({ ...api, auth })
        unsubscribe = api.onAuthStateChanged(auth, setUser)
      })
      .catch(() => setError('Firebase Authentication could not be loaded. Check internet access and Firebase config.'))
      .finally(() => setLoading(false))
    return () => {
      unsubscribe?.()
      setAuthTokenProvider(null)
    }
  }, [])

  const value = useMemo(() => {
    const email = user?.email?.toLowerCase() || ''
    const isAdmin = Boolean(email && adminEmails.includes(email))
    return {
      user,
      loading,
      error,
      isConfigured: hasFirebaseConfig(),
      isAdmin,
      adminEmails,
      signInWithGoogle: async () => {
        if (!authApi) throw new Error('Firebase Auth is not ready yet.')
        const provider = new authApi.GoogleAuthProvider()
        provider.setCustomParameters({ prompt: 'select_account' })
        await authApi.signInWithPopup(authApi.auth, provider)
      },
      signOut: async () => {
        if (authApi) await authApi.signOut(authApi.auth)
      },
    }
  }, [authApi, loading, user, error])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
