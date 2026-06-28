import { Link } from 'react-router-dom'

const statusTone = {
  REPORTED: 'border-slate-300 bg-slate-100 text-slate-700',
  VERIFIED: 'border-emerald-200 bg-emerald-50 text-emerald-800',
  ESCALATED: 'border-orange-200 bg-orange-50 text-orange-800',
  IN_PROGRESS: 'border-blue-200 bg-blue-50 text-blue-800',
  RESOLVED: 'border-civic-200 bg-civic-50 text-civic-800',
}

export const StatusBadge = ({ status }) => <span className={`badge ${statusTone[status] || statusTone.REPORTED}`}>{status}</span>
export const CategoryBadge = ({ category }) => <span className="badge border-civic-200 bg-civic-50 text-civic-800">{category}</span>
export const DashboardCard = ({ label, value }) => <div className="card relative overflow-hidden"><div className="absolute right-0 top-0 h-24 w-24 rounded-full bg-civic-200/45 blur-2xl" /><div className="relative text-sm font-medium text-slate-600">{label}</div><div className="relative mt-2 font-display text-4xl font-black tracking-[-0.06em] text-slate-950">{value}</div></div>
export const IssueCard = ({ issue }) => {
  const evidenceImage = issue.media?.find(item => item.mediaType === 'IMAGE')?.mediaUrl
  const place = [issue.locality, issue.city, issue.state].filter(Boolean).join(', ') || issue.ward
  return <div className="card group overflow-hidden transition duration-300 hover:-translate-y-1 hover:border-civic-700/25 hover:bg-white">
    {evidenceImage ? <div className="relative mb-4 overflow-hidden rounded-2xl border border-civic-900/10"><img src={evidenceImage} alt="" className="h-40 w-full object-cover transition duration-500 group-hover:scale-105" /><div className="absolute inset-0 bg-gradient-to-t from-slate-950/35 to-transparent" />{issue.duplicateReportCount > 1 ? <span className="absolute right-3 top-3 rounded-full bg-slate-950/85 px-3 py-1 text-xs font-black text-white">+{issue.duplicateReportCount} reports</span> : null}</div> : null}
    <div className="flex items-start justify-between gap-3">
      <div><h3 className="font-display text-xl font-black tracking-[-0.04em] text-slate-950">{issue.title}</h3><p className="mt-1 text-sm text-slate-600">{place}</p></div>
      <StatusBadge status={issue.status} />
    </div>
    <div className="mt-4 flex flex-wrap gap-2"><CategoryBadge category={issue.category} /><span className="badge border-amber-200 bg-amber-50 text-amber-800">Severity: {issue.severity || 'TBD'}</span></div>
    <p className="mt-4 line-clamp-3 text-sm leading-6 text-slate-600">{issue.description}</p>
    <div className="mt-5 flex items-center justify-between border-t border-civic-900/10 pt-4 text-xs text-slate-500"><span>{issue.createdAt ? new Date(issue.createdAt).toLocaleDateString() : 'Recent'}</span><Link className="font-bold text-civic-800 transition hover:text-civic-950" to={`/issues/${issue.id}`}>View details</Link></div>
  </div>
}
export const LoadingSpinner = () => <div className="py-16 text-center text-slate-600">Loading...</div>
export const EmptyState = ({ title, description }) => <div className="card text-center"><h3 className="font-display text-xl font-black tracking-[-0.04em]">{title}</h3><p className="mt-2 text-slate-600">{description}</p></div>
