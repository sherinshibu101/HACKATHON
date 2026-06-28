import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../auth.jsx'

export default function Login() {
  const auth = useAuth()
  const location = useLocation()
  const requestedPath = location.state?.from?.pathname
  const destination = requestedPath && requestedPath !== '/login'
    ? requestedPath
    : auth.isAdmin ? '/admin/authorities' : '/'

  if (auth.loading) return <div className="card mx-auto max-w-xl text-center">Loading authentication...</div>
  if (auth.user) return <Navigate to={destination} replace />

  return (
    <section className="mx-auto grid max-w-5xl gap-6 lg:grid-cols-[1.05fr_0.95fr]">
      <div className="linear-panel p-8 md:p-10">
        <div className="relative">
          <p className="linear-kicker">One sign-in, two workspaces</p>
          <h1 className="mt-5 font-display text-5xl font-black tracking-[-0.06em] text-slate-950 md:text-6xl">Civic access that routes itself.</h1>
          <p className="linear-muted mt-5 max-w-xl">
            Sign in once with Google. Admin allowlisted accounts open the authority desk automatically;
            every other Gmail opens the citizen experience.
          </p>
          <div className="mt-8 grid gap-3">
            {['Citizen reporting', 'Authority operations', 'Transparent public history'].map(item => (
              <div key={item} className="flex items-center gap-3 rounded-2xl border border-civic-900/10 bg-white/75 px-4 py-3">
                <span className="h-2 w-2 rounded-full bg-civic-600 shadow-glow" />
                <span className="text-sm font-semibold text-slate-700">{item}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="card grid content-center gap-5">
        <div>
          <p className="linear-kicker">Google authentication</p>
          <h2 className="mt-2 font-display text-3xl font-black tracking-[-0.05em]">Continue to Community Hero AI</h2>
          <p className="mt-3 text-sm leading-6 text-slate-600">Use the Gmail account assigned for your demo role.</p>
        </div>
        {auth.error ? <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{auth.error}</div> : null}
        <button onClick={auth.signInWithGoogle} disabled={!auth.isConfigured} className="btn-primary w-full disabled:cursor-not-allowed disabled:opacity-50">Sign in with Google</button>
        <p className="rounded-2xl border border-civic-900/10 bg-civic-50 p-4 text-xs leading-5 text-slate-600">
          Tip: if Google opens the wrong Gmail, sign out of Google or choose "Use another account" in the popup.
        </p>
      </div>
    </section>
  )
}
