import { useEffect, useState } from 'react'
import api from '../api'
import { LoadingSpinner } from '../components'

function BadgePill({ children }) {
  return <span className="rounded-full border border-civic-200 bg-civic-50 px-3 py-1 text-xs font-black text-civic-800">{children}</span>
}

export default function Leaderboard() {
  const [leaders, setLeaders] = useState(null)
  const [summary, setSummary] = useState(null)
  const [error, setError] = useState('')

  const load = async () => {
    setError('')
    try {
      const [leaderboardResult, summaryResult] = await Promise.all([
        api.get('/gamification/leaderboard'),
        api.get('/gamification/summary'),
      ])
      setLeaders(leaderboardResult.data)
      setSummary(summaryResult.data)
    } catch (err) {
      setError(err.response?.data?.message || 'Leaderboard could not be loaded.')
      setLeaders([])
    }
  }

  useEffect(() => { load() }, [])
  if (!leaders) return <LoadingSpinner />

  const topThree = leaders.slice(0, 3)

  return <div className="grid gap-8">
    <section className="linear-panel p-6 md:p-8">
      <div className="absolute -right-24 -top-24 h-72 w-72 rounded-full bg-civic-200/45 blur-3xl" />
      <div className="relative flex flex-wrap items-end justify-between gap-5">
        <div>
          <p className="linear-kicker">Citizen engagement</p>
          <h1 className="mt-3 font-display text-4xl font-black tracking-[-0.05em] text-slate-950 md:text-5xl">Civic Champions Leaderboard</h1>
          <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-600">Celebrate citizens who report issues, verify community problems, and help move civic work toward resolution.</p>
        </div>
        <button className="btn-secondary" onClick={load}>Refresh</button>
      </div>
    </section>

    {error ? <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div> : null}

    <section className="grid gap-4 md:grid-cols-4">
      <article className="card"><p className="text-sm text-slate-500">Contributors</p><p className="mt-2 text-3xl font-black">{summary?.contributors || 0}</p></article>
      <article className="card"><p className="text-sm text-slate-500">Points Awarded</p><p className="mt-2 text-3xl font-black">{summary?.pointsAwarded || 0}</p></article>
      <article className="card"><p className="text-sm text-slate-500">Reports</p><p className="mt-2 text-3xl font-black">{summary?.reportsSubmitted || 0}</p></article>
      <article className="card"><p className="text-sm text-slate-500">Verifications</p><p className="mt-2 text-3xl font-black">{summary?.verificationsSubmitted || 0}</p></article>
    </section>

    {topThree.length ? <section className="grid gap-4 md:grid-cols-3">{topThree.map(leader => <article key={leader.rank} className="rounded-3xl border border-civic-900/10 bg-white p-6 text-center text-slate-950 shadow-[0_18px_45px_rgba(15,79,78,0.10)]">
      <div className="mx-auto grid h-16 w-16 place-items-center rounded-full bg-gradient-to-br from-yellow-300 to-orange-500 text-2xl font-black text-slate-950">#{leader.rank}</div>
      <h2 className="mt-4 text-xl font-black text-slate-950">{leader.displayName}</h2>
      <p className="mt-2 text-4xl font-black text-civic-700">{leader.points}</p>
      <p className="text-xs font-bold text-slate-600">points</p>
      <div className="mt-4 flex flex-wrap justify-center gap-2">{leader.badges.map(badge => <BadgePill key={badge}>{badge}</BadgePill>)}</div>
    </article>)}</section> : null}

    <section className="card">
      <div className="flex flex-wrap items-end justify-between gap-3"><div><p className="text-xs font-black uppercase tracking-[0.2em] text-civic-700">All contributors</p><h2 className="mt-1 text-2xl font-black">Community ranking</h2></div><p className="text-xs text-slate-500">20/report, 10/verification, 25/community-verified report</p></div>
      <div className="mt-5 grid gap-3">{leaders.length ? leaders.map(leader => <article key={`${leader.rank}-${leader.displayName}`} className="grid gap-3 rounded-2xl bg-slate-50 p-4 md:grid-cols-[auto_1fr_auto] md:items-center">
        <div className="grid h-11 w-11 place-items-center rounded-full bg-civic-700 text-sm font-black text-white">#{leader.rank}</div>
        <div>
          <h3 className="font-black">{leader.displayName}</h3>
          <p className="mt-1 text-xs text-slate-500">{leader.reportsSubmitted} reports / {leader.verificationsSubmitted} verifications / {leader.communityVerifiedReports} verified reports</p>
          <div className="mt-2 flex flex-wrap gap-2">{leader.badges.map(badge => <BadgePill key={badge}>{badge}</BadgePill>)}</div>
        </div>
        <div className="text-right"><p className="text-2xl font-black text-civic-700">{leader.points}</p><p className="text-xs font-bold text-slate-600">points</p></div>
      </article>) : <p className="rounded-2xl border border-dashed p-8 text-center text-sm text-slate-500">No contributors yet. Report or verify an issue to start the leaderboard.</p>}</div>
    </section>
  </div>
}
