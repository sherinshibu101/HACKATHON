import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../api'
import { DashboardCard, LoadingSpinner } from '../components'
import { clusterPublicIssues } from '../issueClusters'

const wardStyles = {
  HEALTHY: 'border-emerald-200 bg-emerald-50 text-emerald-950',
  MODERATE: 'border-amber-200 bg-amber-50 text-amber-950',
  NEEDS_ATTENTION: 'border-orange-200 bg-orange-50 text-orange-950',
  CRITICAL: 'border-red-200 bg-red-50 text-red-950',
}

const wardAccentStyles = {
  HEALTHY: 'bg-emerald-100 text-emerald-900',
  MODERATE: 'bg-amber-100 text-amber-950',
  NEEDS_ATTENTION: 'bg-orange-100 text-orange-950',
  CRITICAL: 'bg-red-100 text-red-950',
}

export default function Dashboard() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadDashboard = async () => {
    setLoading(true)
    setError('')
    const results = await Promise.allSettled([
      api.get('/dashboard/summary'), api.get('/dashboard/category-stats'),
      api.get('/dashboard/ward-stats'), api.get('/dashboard/ward-health'),
      api.get('/dashboard/high-impact'), api.get('/gamification/leaderboard'),
      api.get('/gamification/summary'), api.get('/issues'),
    ])
    const value = (index, fallback) => results[index].status === 'fulfilled' ? results[index].value.data : fallback
    const failed = results.filter(result => result.status === 'rejected').length
    const clusteredIssues = results[7].status === 'fulfilled' ? await clusterPublicIssues(results[7].value.data) : []
    const consolidatedImpact = clusteredIssues
      .filter(issue => issue.status !== 'RESOLVED' && issue.impactScore != null)
      .sort((a, b) => (b.impactScore || 0) - (a.impactScore || 0))
      .slice(0, 5)
    setData({
      summary: value(0, { totalIssues: 0, reportedIssues: 0, inProgressIssues: 0, resolvedIssues: 0, communityVerifications: 0 }),
      categories: [...value(1, [])].sort((a, b) => b.count - a.count),
      wards: [...value(2, [])].sort((a, b) => b.count - a.count),
      health: [...value(3, [])].sort((a, b) => a.healthScore - b.healthScore),
      impact: consolidatedImpact.length ? consolidatedImpact : value(4, []),
      leaders: value(5, []),
      gamification: value(6, { contributors: 0, pointsAwarded: 0, reportsSubmitted: 0, verificationsSubmitted: 0 }),
    })
    if (failed) setError(`${failed} dashboard section${failed === 1 ? '' : 's'} could not be refreshed.`)
    setLoading(false)
  }

  useEffect(() => { loadDashboard() }, [])
  if (loading && !data) return <LoadingSpinner />

  return <div className="grid gap-8">
    <div className="flex flex-wrap items-end justify-between gap-4"><div><p className="text-xs font-black uppercase tracking-[0.2em] text-civic-700">Civic accountability</p><h1 className="mt-1 font-display text-4xl font-black tracking-[-0.05em]">Community Health Dashboard</h1><p className="mt-2 text-sm text-slate-600">Live issue pressure, community evidence, and ward-level response priorities.</p></div><button className="btn-secondary" onClick={loadDashboard} disabled={loading}>{loading ? 'Refreshing...' : 'Refresh dashboard'}</button></div>
    {error ? <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700"><span>{error} Make sure the backend was restarted, then retry.</span><button className="font-bold underline" onClick={loadDashboard}>Retry</button></div> : null}
    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
      <DashboardCard label="Total Issues" value={data.summary.totalIssues} />
      <DashboardCard label="Reported" value={data.summary.reportedIssues} />
      <DashboardCard label="In Progress" value={data.summary.inProgressIssues} />
      <DashboardCard label="Resolved" value={data.summary.resolvedIssues} />
      <DashboardCard label="Community Verifications" value={data.summary.communityVerifications} />
    </div>

    <section className="linear-panel p-6">
      <div className="relative flex flex-wrap items-end justify-between gap-4"><div><p className="linear-kicker">Public accountability</p><h2 className="mt-1 font-display text-2xl font-black tracking-[-0.04em]">Tamper-evident audit trail enabled</h2><p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600">Citizens can view public status history on each issue. Full SHA-256 ledger verification is available to admins in the authority portal.</p></div><Link className="btn-secondary" to="/issues">View issue histories</Link></div>
    </section>

    <section className="linear-panel p-6">
      <div className="relative flex flex-wrap items-end justify-between gap-4"><div><p className="linear-kicker">Gamified civic engagement</p><h2 className="mt-1 font-display text-2xl font-black tracking-[-0.04em]">Citizen contribution engine</h2><p className="mt-2 text-sm leading-6 text-slate-600">Reports, verifications, and community-verified issues now earn points and badges.</p></div><Link className="btn-primary" to="/leaderboard">View leaderboard</Link></div>
      <div className="relative mt-5 grid gap-3 md:grid-cols-4"><div className="rounded-2xl bg-civic-50 p-4"><b className="block text-2xl text-civic-900">{data.gamification.contributors}</b><span className="text-xs font-semibold text-slate-600">Contributors</span></div><div className="rounded-2xl bg-civic-50 p-4"><b className="block text-2xl text-civic-900">{data.gamification.pointsAwarded}</b><span className="text-xs font-semibold text-slate-600">Points awarded</span></div><div className="rounded-2xl bg-civic-50 p-4"><b className="block text-2xl text-civic-900">{data.gamification.reportsSubmitted}</b><span className="text-xs font-semibold text-slate-600">Reports scored</span></div><div className="rounded-2xl bg-civic-50 p-4"><b className="block text-2xl text-civic-900">{data.gamification.verificationsSubmitted}</b><span className="text-xs font-semibold text-slate-600">Verifications scored</span></div></div>
      <div className="relative mt-5 grid gap-3 md:grid-cols-3">{data.leaders.slice(0, 3).map(leader => <article key={leader.rank} className="rounded-2xl border border-civic-900/10 bg-white p-4 text-slate-950"><p className="text-xs font-black text-civic-700">#{leader.rank}</p><h3 className="mt-1 font-black">{leader.displayName}</h3><p className="mt-2 text-2xl font-black text-civic-900">{leader.points} pts</p><p className="mt-1 text-xs text-slate-500">{leader.badges.join(' / ') || 'New contributor'}</p></article>)}</div>
    </section>

    <section>
      <div className="flex flex-wrap items-end justify-between gap-3"><div><h2 className="font-display text-3xl font-black tracking-[-0.04em]">Ward health scores</h2><p className="mt-1 text-sm text-slate-600">Lowest-scoring wards appear first.</p></div><p className="max-w-xl text-sm leading-6 text-slate-600">Scores start at 100. Unresolved issues subtract 10 critical, 6 high, 3 medium, or 1 low point, plus 2 points when older than seven days.</p></div>
      <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-3">{data.health.map(ward => <article key={ward.ward} className={`rounded-3xl border p-6 shadow-[0_18px_45px_rgba(15,79,78,0.10)] ${wardStyles[ward.status]}`}>
        <div className="flex items-start justify-between gap-4"><div><p className="text-xs font-black uppercase tracking-[0.18em] opacity-75">{ward.status.replaceAll('_', ' ')}</p><h3 className="mt-1 text-xl font-black">{ward.ward}</h3></div><div className="text-right"><span className="text-4xl font-black">{ward.healthScore}</span><span className="opacity-70">/100</span></div></div>
        <div className="mt-6 grid grid-cols-3 gap-2 text-center text-xs font-semibold"><div className={`rounded-xl p-2 ${wardAccentStyles[ward.status]}`}><b className="block text-lg">{ward.unresolvedIssues}</b>Open</div><div className={`rounded-xl p-2 ${wardAccentStyles[ward.status]}`}><b className="block text-lg">{ward.criticalIssues}</b>Critical</div><div className={`rounded-xl p-2 ${wardAccentStyles[ward.status]}`}><b className="block text-lg">{ward.resolvedIssues}</b>Resolved</div></div>
      </article>)}{!data.health.length ? <p className="rounded-3xl border border-dashed border-slate-300 p-8 text-center text-sm text-slate-500 md:col-span-2 xl:col-span-3">Ward scores will appear after the first issue is reported.</p> : null}</div>
    </section>

    <div className="grid gap-6 lg:grid-cols-2">
      <section className="card"><h3 className="text-xl font-black">Most reported categories</h3><div className="mt-4 grid gap-2">{data.categories.map(category => <div key={category.category} className="flex justify-between rounded-2xl bg-slate-50 p-3"><span>{category.category.replaceAll('_', ' ')}</span><span className="font-black">{category.count}</span></div>)}{!data.categories.length ? <p className="text-sm text-slate-500">No category data yet.</p> : null}</div></section>
      <section className="card"><h3 className="text-xl font-black">Most affected wards</h3><div className="mt-4 grid gap-2">{data.wards.map(ward => <div key={ward.ward} className="flex justify-between rounded-2xl bg-slate-50 p-3"><span>{ward.ward}</span><span className="font-black">{ward.count}</span></div>)}{!data.wards.length ? <p className="text-sm text-slate-500">No ward data yet.</p> : null}</div></section>
    </div>

    <section className="card"><h3 className="text-xl font-black">Top high-impact unresolved issues</h3><p className="mt-1 text-sm text-slate-500">Nearby duplicate reports are consolidated into one civic case with a community report count.</p><div className="mt-4 grid gap-3 md:grid-cols-2 xl:grid-cols-5">{data.impact.map(issue => { const evidenceImage = issue.media?.find(item => item.mediaType === 'IMAGE')?.mediaUrl; return <Link key={issue.id} to={`/issues/${issue.id}`} className="overflow-hidden rounded-2xl border border-slate-200 transition hover:-translate-y-1 hover:border-civic-500 hover:shadow-lg">{evidenceImage ? <div className="relative"><img src={evidenceImage} alt="" className="h-28 w-full object-cover" />{issue.duplicateReportCount > 1 ? <span className="absolute right-2 top-2 rounded-full bg-slate-950/85 px-2 py-1 text-[11px] font-black text-white">+{issue.duplicateReportCount} reports</span> : null}</div> : null}<div className="p-4"><div className="text-3xl font-black text-civic-700">{issue.impactScore}</div><h4 className="mt-2 font-bold">{issue.title}</h4><p className="mt-2 text-xs text-slate-500">{issue.ward} / {issue.severity}</p></div></Link>})}{!data.impact.length ? <p className="text-sm text-slate-500">No analyzed unresolved issues yet.</p> : null}</div></section>
  </div>
}
