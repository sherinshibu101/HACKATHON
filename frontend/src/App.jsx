import { Link, Navigate, NavLink, Route, Routes, useLocation } from 'react-router-dom'
import Home from './pages/Home'
import ReportIssue from './pages/ReportIssue'
import IssuesList from './pages/IssuesList'
import IssueDetails from './pages/IssueDetails'
import MapView from './pages/MapView'
import Dashboard from './pages/Dashboard'
import AuthorityPortal from './pages/AuthorityPortal'
import Leaderboard from './pages/Leaderboard'
import ResolutionCertificate from './pages/ResolutionCertificate'
import Login from './pages/Login'
import SignOut from './pages/SignOut'
import { useAuth } from './auth.jsx'
import CitizenHelpChat from './CitizenHelpChat.jsx'

const navClass = ({ isActive }) => `rounded-full px-3 py-2 text-sm font-bold transition ${isActive ? 'bg-civic-700 text-white shadow-sm' : 'text-slate-600 hover:bg-civic-50 hover:text-civic-900'}`

function RequireAuth({ children, admin = false }) {
  const auth = useAuth()
  const location = useLocation()
  if (auth.loading) return <div className="card mx-auto max-w-md text-center">Checking sign-in...</div>
  if (!auth.user) return <Navigate to="/login" state={{ from: location }} replace />
  if (admin && !auth.isAdmin) return <Navigate to="/unauthorized" replace />
  return children
}

function Unauthorized() {
  const auth = useAuth()
  return <section className="card mx-auto max-w-2xl text-center"><p className="text-xs font-black uppercase tracking-[0.2em] text-red-700">Admin access denied</p><h1 className="mt-2 font-display text-3xl font-black tracking-[-0.04em]">This Gmail is not an admin account</h1><p className="mt-3 text-slate-600">{auth.user?.email} is signed in, but it is not listed in <b>VITE_ADMIN_EMAILS</b>.</p><div className="mt-6 flex justify-center gap-3"><Link className="btn-secondary" to="/">Go to citizen app</Link><button className="btn-primary" onClick={auth.signOut}>Sign out</button></div></section>
}

function RoleHome() {
  const auth = useAuth()
  if (auth.isAdmin) return <Navigate to="/admin/authorities" replace />
  return <Home />
}

export default function App() {
  const location = useLocation()
  const auth = useAuth()
  const isAdminRoute = location.pathname.startsWith('/admin') || location.pathname === '/authorities'

  return (
    <div className="min-h-screen text-slate-950">
      <header className="sticky top-0 z-30 border-b border-civic-900/10 bg-[#fbfaf4]/85 backdrop-blur-2xl">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4">
          <Link to={isAdminRoute ? '/admin/authorities' : '/'} className="group flex items-center gap-3 text-sm font-black tracking-tight text-slate-950">
            <span className="grid h-8 w-8 place-items-center rounded-xl bg-civic-700 text-white shadow-[0_10px_25px_rgba(15,118,110,0.22)]">CH</span>
            <span className="font-display text-lg tracking-[-0.04em]">Community Hero AI</span>
          </Link>
          <nav className="hidden items-center gap-1 rounded-full border border-civic-900/10 bg-white/85 p-1 shadow-[0_12px_40px_rgba(15,79,78,0.12)] backdrop-blur-xl md:flex">
            {isAdminRoute ? <>
              <NavLink to="/admin/authorities" className={navClass}>Authority Desk</NavLink>
              <NavLink to="/dashboard" className={navClass}>Public Dashboard</NavLink>
              <NavLink to="/issues" className={navClass}>Public Issues</NavLink>
            </> : <>
              <NavLink to="/" className={navClass}>Home</NavLink>
              <NavLink to="/report" className={navClass}>Report Issue</NavLink>
              <NavLink to="/issues" className={navClass}>Issues</NavLink>
              <NavLink to="/map" className={navClass}>Map</NavLink>
              <NavLink to="/dashboard" className={navClass}>Dashboard</NavLink>
              <NavLink to="/leaderboard" className={navClass}>Leaderboard</NavLink>
              {auth.isAdmin ? <NavLink to="/admin/authorities" className={navClass}>Authority Desk</NavLink> : null}
            </>}
            {auth.user ? <div className="ml-2 flex items-center gap-2 rounded-full border border-civic-900/10 bg-civic-50 px-3 py-2 text-xs font-bold text-slate-700"><span className="max-w-[150px] truncate">{auth.user.displayName || auth.user.email}</span><Link className="text-civic-800 hover:text-civic-950" to="/sign-out">Sign out</Link></div> : <NavLink to="/login" className={navClass}>Sign in</NavLink>}
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-7xl px-4 py-8 md:py-10">
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/sign-out" element={<SignOut />} />
          <Route path="/unauthorized" element={<Unauthorized />} />
          <Route path="/citizen" element={<Navigate to="/" replace />} />
          <Route path="/" element={<RequireAuth><RoleHome /></RequireAuth>} />
          <Route path="/report" element={<RequireAuth><ReportIssue /></RequireAuth>} />
          <Route path="/issues" element={<RequireAuth><IssuesList /></RequireAuth>} />
          <Route path="/issues/:id" element={<RequireAuth><IssueDetails /></RequireAuth>} />
          <Route path="/issues/:id/certificate" element={<RequireAuth><ResolutionCertificate /></RequireAuth>} />
          <Route path="/map" element={<RequireAuth><MapView /></RequireAuth>} />
          <Route path="/dashboard" element={<RequireAuth><Dashboard /></RequireAuth>} />
          <Route path="/leaderboard" element={<RequireAuth><Leaderboard /></RequireAuth>} />
          <Route path="/admin" element={<Navigate to="/admin/authorities" replace />} />
          <Route path="/admin/authorities" element={<RequireAuth admin><AuthorityPortal /></RequireAuth>} />
          <Route path="/authorities" element={<Navigate to="/admin/authorities" replace />} />
          <Route path="/authority" element={<Navigate to="/admin/authorities" replace />} />
        </Routes>
      </main>
      {auth.user && !auth.isAdmin && !isAdminRoute ? <CitizenHelpChat /> : null}
    </div>
  )
}
