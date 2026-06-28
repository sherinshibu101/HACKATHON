import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth.jsx'

export default function SignOut() {
  const auth = useAuth()
  const [status, setStatus] = useState('Signing you out...')

  useEffect(() => {
    let active = true
    auth.signOut()
      .then(() => {
        if (active) setStatus('You have been signed out.')
      })
      .catch(() => {
        if (active) setStatus('Sign out could not be completed. Please try again.')
      })
    return () => { active = false }
  }, [])

  return <section className="mx-auto max-w-2xl">
    <div className="card text-center">
      <p className="text-xs font-black uppercase tracking-[0.2em] text-civic-700">Session ended</p>
      <h1 className="mt-2 text-3xl font-black">Sign out</h1>
      <p className="mt-3 text-slate-600">{status}</p>
      <div className="mt-6 flex flex-wrap justify-center gap-3">
        <Link className="btn-primary" to="/login">Sign in again</Link>
        <Link className="btn-secondary" to="/">Return home</Link>
      </div>
    </div>
  </section>
}
