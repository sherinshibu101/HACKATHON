import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../api'
import { CategoryBadge, LoadingSpinner, StatusBadge } from '../components'

const severityRank = { CRITICAL: 4, HIGH: 3, MEDIUM: 2, LOW: 1 }
const statusTone = {
  REPORTED: 'bg-slate-100 text-slate-700',
  VERIFIED: 'bg-emerald-100 text-emerald-800',
  ESCALATED: 'bg-orange-100 text-orange-800',
  IN_PROGRESS: 'bg-blue-100 text-blue-800',
  RESOLVED: 'bg-civic-100 text-civic-800',
}

function PortalStat({ label, value, hint }) {
  return <article className="rounded-3xl border border-civic-900/10 bg-white/90 p-5 shadow-sm">
    <p className="text-xs font-black uppercase tracking-[0.18em] text-slate-500">{label}</p>
    <p className="mt-2 text-3xl font-black text-slate-950">{value}</p>
    {hint ? <p className="mt-1 text-xs font-semibold text-slate-500">{hint}</p> : null}
  </article>
}

function HistoryList({ history = [] }) {
  if (!history.length) return <p className="rounded-2xl border border-dashed p-5 text-center text-sm text-slate-500">No workflow history yet.</p>
  return <div className="grid gap-3">{history.slice().reverse().map(item => <article key={item.id || `${item.toStatus}-${item.createdAt}`} className="rounded-2xl bg-slate-50 p-4">
    <div className="flex flex-wrap items-center justify-between gap-2">
      <span className={`badge ${statusTone[item.toStatus] || 'bg-slate-100 text-slate-700'}`}>{item.toStatus.replaceAll('_', ' ')}</span>
      <time className="text-xs font-bold text-slate-500">{new Date(item.createdAt).toLocaleString()}</time>
    </div>
    <p className="mt-3 text-sm leading-6 text-slate-700">{item.note}</p>
    <div className="mt-3 flex flex-wrap gap-2 text-xs font-bold text-slate-500">
      <span>{item.actorName}</span>
      <span className="rounded-full bg-white px-2 py-1">{item.actorType}</span>
      {item.evidenceUrl ? <a className="text-civic-700 underline" href={item.evidenceUrl} target="_blank" rel="noreferrer">Evidence</a> : null}
    </div>
  </article>)}</div>
}

export default function AuthorityPortal() {
  const [issues, setIssues] = useState(null)
  const [selectedId, setSelectedId] = useState(null)
  const [workflow, setWorkflow] = useState(null)
  const [filter, setFilter] = useState('ACTIVE')
  const [query, setQuery] = useState('')
  const [form, setForm] = useState({ targetStatus: '', actorName: 'Ward Authority Desk', note: '', evidenceUrl: '' })
  const [loadingWorkflow, setLoadingWorkflow] = useState(false)
  const [updating, setUpdating] = useState(false)
  const [message, setMessage] = useState('')
  const [integrity, setIntegrity] = useState(null)
  const [checkingIntegrity, setCheckingIntegrity] = useState(false)
  const [agentRun, setAgentRun] = useState(null)
  const [agentLoading, setAgentLoading] = useState(false)
  const [agentMessage, setAgentMessage] = useState('')
  const [agentReviewNote, setAgentReviewNote] = useState('')

  const loadIssues = async () => {
    const { data } = await api.get('/issues')
    const sorted = data.slice().sort((a, b) => {
      if ((a.status === 'RESOLVED') !== (b.status === 'RESOLVED')) return a.status === 'RESOLVED' ? 1 : -1
      return (b.impactScore || 0) - (a.impactScore || 0) || (severityRank[b.severity] || 0) - (severityRank[a.severity] || 0)
    })
    setIssues(sorted)
    setSelectedId(current => current || sorted[0]?.id || null)
  }

  useEffect(() => {
    loadIssues().catch(() => setIssues([]))
  }, [])

  useEffect(() => {
    if (!selectedId) {
      setWorkflow(null)
      return
    }
    setLoadingWorkflow(true)
    setAgentLoading(true)
    setMessage('')
    setAgentMessage('')
    Promise.allSettled([
      api.get(`/issues/${selectedId}/status-workflow`),
      api.get(`/admin/issues/${selectedId}/agent-runs/latest`),
    ]).then(([workflowResult, agentResult]) => {
      if (workflowResult.status === 'fulfilled') {
        const data = workflowResult.value.data
        setWorkflow(data)
        setForm(current => ({ ...current, targetStatus: data.allowedTransitions?.[0] || '', note: '', evidenceUrl: '' }))
      } else setMessage(workflowResult.reason?.response?.data?.message || 'Workflow could not be loaded.')
      setAgentRun(agentResult.status === 'fulfilled' ? agentResult.value.data || null : null)
    }).finally(() => { setLoadingWorkflow(false); setAgentLoading(false) })
  }, [selectedId])

  const selectedIssue = issues?.find(issue => issue.id === selectedId)

  const filteredIssues = useMemo(() => {
    const term = query.trim().toLowerCase()
    return (issues || []).filter(issue => {
      const activeMatch = filter === 'ALL' || (filter === 'ACTIVE' ? issue.status !== 'RESOLVED' : issue.status === filter)
      const textMatch = !term || [issue.title, issue.ward, issue.locality, issue.city, issue.category, issue.status]
        .filter(Boolean).join(' ').toLowerCase().includes(term)
      return activeMatch && textMatch
    })
  }, [issues, filter, query])

  const stats = useMemo(() => {
    const all = issues || []
    return {
      active: all.filter(issue => issue.status !== 'RESOLVED').length,
      escalated: all.filter(issue => issue.status === 'ESCALATED').length,
      inProgress: all.filter(issue => issue.status === 'IN_PROGRESS').length,
      resolved: all.filter(issue => issue.status === 'RESOLVED').length,
    }
  }, [issues])

  const updateStatus = async (event) => {
    event.preventDefault()
    if (!selectedIssue) return
    setUpdating(true)
    setMessage('')
    try {
      const { data } = await api.patch(`/issues/${selectedIssue.id}/status`, form)
      setWorkflow(data)
      setMessage(`Issue #${selectedIssue.id} moved to ${data.currentStatus.replaceAll('_', ' ')}.`)
      setForm(current => ({ ...current, targetStatus: data.allowedTransitions?.[0] || '', note: '', evidenceUrl: '' }))
      await loadIssues()
    } catch (err) {
      setMessage(err.response?.data?.message || 'Status update failed.')
    } finally {
      setUpdating(false)
    }
  }

  const deleteMedia = async (mediaId) => {
    if (!selectedIssue || !window.confirm('Remove this evidence attachment from the public issue?')) return
    setMessage('')
    try {
      await api.delete(`/issues/${selectedIssue.id}/media/${mediaId}`)
      setMessage('Evidence attachment removed by admin.')
      await loadIssues()
    } catch (err) {
      setMessage(err.response?.data?.message || 'Evidence attachment could not be removed.')
    }
  }

  const checkIntegrity = async () => {
    setCheckingIntegrity(true)
    try {
      const { data } = await api.get('/ledger/integrity')
      setIntegrity(data)
    } catch (err) {
      setIntegrity({
        valid: false,
        totalEntries: 0,
        message: err.response?.data?.message || 'Ledger integrity check failed.',
      })
    } finally {
      setCheckingIntegrity(false)
    }
  }

  const runAgent = async () => {
    if (!selectedIssue) return
    setAgentLoading(true)
    setAgentMessage('')
    try {
      const { data } = await api.post(`/admin/issues/${selectedIssue.id}/agent-runs`)
      setAgentRun(data)
      setAgentMessage('A new bounded investigation has completed. Review every tool observation before approval.')
    } catch (err) {
      setAgentMessage(err.response?.data?.message || 'The Civic Case Manager could not run.')
    } finally { setAgentLoading(false) }
  }

  const reviewAgent = async decision => {
    if (!selectedIssue || !agentRun) return
    setAgentLoading(true)
    setAgentMessage('')
    try {
      const { data } = await api.post(`/admin/issues/${selectedIssue.id}/agent-runs/${agentRun.id}/${decision}`, {
        actorName: form.actorName || 'Ward Authority Desk',
        note: agentReviewNote,
        evidenceUrl: form.evidenceUrl,
      })
      setAgentRun(data)
      setAgentReviewNote('')
      setAgentMessage(decision === 'approve' ? 'Recommendation approved and recorded.' : 'Recommendation rejected and recorded for review.')
      await loadIssues()
      const workflowResult = await api.get(`/issues/${selectedIssue.id}/status-workflow`)
      setWorkflow(workflowResult.data)
    } catch (err) {
      setAgentMessage(err.response?.data?.message || `Recommendation could not be ${decision}d.`)
    } finally { setAgentLoading(false) }
  }

  if (!issues) return <LoadingSpinner />

  return <div className="grid gap-6">
    <section className="linear-panel p-6 md:p-8">
      <div className="absolute -right-20 -top-24 h-72 w-72 rounded-full bg-civic-400/30 blur-3xl" />
      <div className="absolute bottom-0 left-1/3 h-40 w-40 rounded-full bg-emerald-400/20 blur-2xl" />
      <div className="relative flex flex-wrap items-end justify-between gap-5">
        <div>
          <p className="linear-kicker">Admin role / authority operations</p>
          <h1 className="mt-3 font-display text-4xl font-black tracking-[-0.05em] text-slate-950 md:text-5xl">Admin Authority Portal</h1>
          <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-600">A focused admin desk for ward officers to review escalated issues, add official notes, attach evidence links, and move reports toward resolution.</p>
        </div>
      </div>
    </section>

    <section className="grid gap-4 md:grid-cols-4">
      <PortalStat label="Active cases" value={stats.active} hint="Not yet resolved" />
      <PortalStat label="Escalated" value={stats.escalated} hint="Needs authority action" />
      <PortalStat label="In progress" value={stats.inProgress} hint="Work underway" />
      <PortalStat label="Resolved" value={stats.resolved} hint="Closed with note" />
    </section>

    <section className={`rounded-[2rem] border p-6 shadow-[0_18px_45px_rgba(15,79,78,0.10)] ${integrity?.valid === false ? 'border-red-200 bg-red-50 text-red-950' : 'border-civic-900/10 bg-white/90 text-slate-950'}`}>
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="linear-kicker">Zero Trust civil ledger</p>
          <h2 className="mt-1 text-2xl font-black">System Integrity Check</h2>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600">Admin-only verification. Every status update and community verification is chained with SHA-256 to detect direct database tampering.</p>
        </div>
        <button className="btn-secondary disabled:opacity-60" disabled={checkingIntegrity} onClick={checkIntegrity}>{checkingIntegrity ? 'Checking ledger...' : 'Run integrity check'}</button>
      </div>
      {integrity ? <div className={`mt-5 rounded-2xl p-4 ${integrity.valid ? 'bg-emerald-50 ring-1 ring-emerald-200' : 'bg-red-100 ring-1 ring-red-200'}`}><div className="flex flex-wrap items-center justify-between gap-3"><p className="text-lg font-black">{integrity.valid ? 'Ledger verified' : 'Warning: audit log compromised!'}</p><span className="rounded-full bg-white px-3 py-1 text-xs font-black text-slate-700">{integrity.totalEntries} entries</span></div><p className="mt-2 text-sm text-slate-700">{integrity.message}</p>{integrity.lastHash ? <p className="mt-3 break-all rounded-xl bg-white p-3 text-xs text-slate-600">Latest hash: {integrity.lastHash}</p> : null}</div> : <div className="mt-5 rounded-2xl bg-civic-50 p-4 text-sm text-slate-600">Click the button to verify the tamper-evident audit chain.</div>}
    </section>

    <section className="grid gap-6 lg:grid-cols-[0.9fr_1.4fr]">
      <aside className="card h-fit">
        <div className="flex items-center justify-between gap-3">
          <div>
            <p className="text-xs font-black uppercase tracking-[0.2em] text-civic-700">Admin case queue</p>
            <h2 className="mt-1 text-2xl font-black">Authority inbox</h2>
          </div>
          <button className="btn-secondary py-2 text-sm" onClick={loadIssues}>Refresh</button>
        </div>
        <div className="mt-5 grid gap-3">
          <input value={query} onChange={event => setQuery(event.target.value)} className="rounded-2xl border px-4 py-3" placeholder="Search title, ward, locality..." />
          <select value={filter} onChange={event => setFilter(event.target.value)} className="rounded-2xl border px-4 py-3">
            <option value="ACTIVE">Active issues</option>
            <option value="ESCALATED">Escalated only</option>
            <option value="IN_PROGRESS">In progress only</option>
            <option value="VERIFIED">Community verified</option>
            <option value="REPORTED">Reported</option>
            <option value="RESOLVED">Resolved</option>
            <option value="ALL">All issues</option>
          </select>
        </div>
        <div className="mt-5 grid max-h-[34rem] gap-3 overflow-y-auto pr-1">
          {filteredIssues.length ? filteredIssues.map(issue => <button key={issue.id} onClick={() => setSelectedId(issue.id)} className={`rounded-2xl border p-4 text-left transition ${selectedId === issue.id ? 'border-civic-500 bg-civic-50 shadow-sm' : 'border-slate-200 bg-white hover:border-civic-200'}`}>
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="font-black text-slate-950">#{issue.id} {issue.title}</p>
                <p className="mt-1 text-xs font-semibold text-slate-500">{[issue.ward, issue.locality, issue.city].filter(Boolean).join(' / ')}</p>
              </div>
              <span className={`badge ${statusTone[issue.status] || 'bg-slate-100 text-slate-700'}`}>{issue.status.replaceAll('_', ' ')}</span>
            </div>
            <div className="mt-3 flex flex-wrap gap-2"><CategoryBadge category={issue.category} /><span className="badge bg-orange-100 text-orange-800">Impact {issue.impactScore || 'TBD'}</span></div>
          </button>) : <p className="rounded-2xl border border-dashed p-5 text-center text-sm text-slate-500">No issues match this queue.</p>}
        </div>
      </aside>

      <section className="grid gap-6">
        {selectedIssue ? <article className="card">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.2em] text-civic-700">Selected case</p>
              <h2 className="mt-1 text-3xl font-black">#{selectedIssue.id} {selectedIssue.title}</h2>
              <p className="mt-2 text-sm text-slate-600">{selectedIssue.formattedAddress || [selectedIssue.locality, selectedIssue.city, selectedIssue.state].filter(Boolean).join(', ')}</p>
            </div>
            <div className="flex flex-wrap gap-2"><StatusBadge status={selectedIssue.status} /><Link className="btn-secondary py-2 text-sm" to={`/issues/${selectedIssue.id}`}>Public details</Link><Link className={selectedIssue.status === 'RESOLVED' ? 'btn-primary py-2 text-sm' : 'btn-secondary py-2 text-sm'} to={`/issues/${selectedIssue.id}/certificate`}>{selectedIssue.status === 'RESOLVED' ? 'View certificate' : 'Certificate preview'}</Link></div>
          </div>
          <div className="mt-5 grid gap-4 md:grid-cols-3">
            <div className="rounded-2xl bg-slate-50 p-4"><p className="text-xs font-bold uppercase tracking-wider text-slate-500">Severity</p><p className="mt-2 text-xl font-black">{selectedIssue.severity || 'Pending'}</p></div>
            <div className="rounded-2xl bg-slate-50 p-4"><p className="text-xs font-bold uppercase tracking-wider text-slate-500">Impact</p><p className="mt-2 text-xl font-black">{selectedIssue.impactScore || 'TBD'} / 100</p></div>
            <div className="rounded-2xl bg-slate-50 p-4"><p className="text-xs font-bold uppercase tracking-wider text-slate-500">AI Department</p><p className="mt-2 text-sm font-black">{selectedIssue.recommendedDepartment || 'Not assigned'}</p></div>
          </div>
          {selectedIssue.dispatchDepartment ? <div className="mt-5 grid gap-4 md:grid-cols-3">
            <div className="rounded-2xl bg-emerald-50 p-4 ring-1 ring-emerald-200"><p className="text-xs font-bold uppercase tracking-wider text-emerald-700">Dispatch Department</p><p className="mt-2 text-sm font-black">{selectedIssue.dispatchDepartment}</p></div>
            <div className="rounded-2xl bg-emerald-50 p-4 ring-1 ring-emerald-200"><p className="text-xs font-bold uppercase tracking-wider text-emerald-700">Dispatch Priority</p><p className="mt-2 text-sm font-black">{selectedIssue.dispatchPriority}</p></div>
            <div className="rounded-2xl bg-emerald-50 p-4 ring-1 ring-emerald-200"><p className="text-xs font-bold uppercase tracking-wider text-emerald-700">Analyzed</p><p className="mt-2 text-sm font-black">{selectedIssue.dispatchAnalyzedAt ? new Date(selectedIssue.dispatchAnalyzedAt).toLocaleString() : 'N/A'}</p></div>
          </div> : null}
          {selectedIssue.dispatchCitizenNotification ? <div className="mt-5 rounded-2xl bg-emerald-50 p-4 ring-1 ring-emerald-200">
            <p className="text-xs font-bold uppercase tracking-wider text-emerald-700">Draft Citizen Notification</p>
            <p className="mt-2 text-sm leading-6 text-slate-700 whitespace-pre-wrap">{selectedIssue.dispatchCitizenNotification}</p>
          </div> : null}
          <p className="mt-5 text-sm leading-7 text-slate-700">{selectedIssue.description}</p>
          <div className="mt-5 grid gap-4 md:grid-cols-2">
            {selectedIssue.resolutionUrgency ? <div className="rounded-2xl border border-orange-100 bg-orange-50 p-4"><p className="text-xs font-bold uppercase tracking-wider text-orange-700">Resolution urgency</p><p className="mt-2 text-sm font-black leading-6 text-slate-800">{selectedIssue.resolutionUrgency}</p></div> : null}
            {selectedIssue.riskExplanation ? <div className="rounded-2xl border border-red-100 bg-red-50 p-4"><p className="text-xs font-bold uppercase tracking-wider text-red-700">Risk assessment</p><p className="mt-2 text-sm leading-6 text-slate-700">{selectedIssue.riskExplanation}</p></div> : null}
          </div>
          {selectedIssue.suggestedAction ? <div className="mt-5 rounded-2xl border border-civic-100 bg-civic-50 p-4"><p className="text-xs font-bold uppercase tracking-wider text-civic-700">AI resolution plan</p><p className="mt-2 text-sm leading-6 text-slate-700">{selectedIssue.suggestedAction}</p></div> : null}
        </article> : null}

        {selectedIssue ? <article className="relative overflow-hidden rounded-[1.75rem] border border-civic-900/10 bg-civic-950 p-6 text-white shadow-[0_24px_70px_rgba(8,47,45,0.25)]">
          <div className="absolute -right-20 -top-24 h-64 w-64 rounded-full bg-teal-300/15 blur-3xl" />
          <div className="relative flex flex-wrap items-start justify-between gap-4">
            <div><p className="text-xs font-black uppercase tracking-[0.22em] text-teal-300">Bounded autonomous investigation</p><h2 className="mt-2 font-display text-2xl font-black">Civic Case Manager Agent</h2><p className="mt-2 max-w-2xl text-sm leading-6 text-teal-50/80">The agent gathers trusted signals and proposes an action. It cannot change status, email an authority, delete evidence, or resolve a case without human approval.</p></div>
            <button type="button" onClick={runAgent} disabled={agentLoading} className="rounded-full bg-teal-300 px-5 py-3 text-sm font-black text-civic-950 transition hover:bg-teal-200 disabled:opacity-50">{agentLoading ? 'Investigating...' : agentRun ? 'Run new investigation' : 'Start investigation'}</button>
          </div>
          {agentMessage ? <p className="relative mt-4 rounded-2xl border border-white/10 bg-white/10 p-3 text-sm text-teal-50">{agentMessage}</p> : null}
          {agentRun ? <div className="relative mt-6 grid gap-5">
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
              <div className="rounded-2xl bg-white/10 p-4"><p className="text-[10px] font-black uppercase tracking-wider text-teal-300">Run status</p><p className="mt-2 font-black">{agentRun.status.replaceAll('_', ' ')}</p></div>
              <div className="rounded-2xl bg-white/10 p-4"><p className="text-[10px] font-black uppercase tracking-wider text-teal-300">Confidence</p><p className="mt-2 text-2xl font-black">{agentRun.confidence ?? 0}%</p></div>
              <div className="rounded-2xl bg-white/10 p-4"><p className="text-[10px] font-black uppercase tracking-wider text-teal-300">Proposed status</p><p className="mt-2 font-black">{agentRun.proposedStatus?.replaceAll('_', ' ') || 'MONITOR'}</p></div>
              <div className="rounded-2xl bg-white/10 p-4"><p className="text-[10px] font-black uppercase tracking-wider text-teal-300">Target window</p><p className="mt-2 font-black">{agentRun.targetResolutionHours || 72} hours</p></div>
            </div>
            <div className="rounded-2xl border border-white/10 bg-white/10 p-4"><p className="text-[10px] font-black uppercase tracking-wider text-teal-300">Admin recommendation</p><p className="mt-2 whitespace-pre-wrap text-sm leading-7 text-white/90">{agentRun.adminRecommendation || agentRun.failureMessage}</p><p className="mt-3 text-sm font-black text-teal-200">Next: {agentRun.recommendedNextAction}</p></div>
            <div><p className="text-xs font-black uppercase tracking-[0.18em] text-teal-300">Agent activity log</p><div className="mt-3 grid gap-2">{(agentRun.steps || []).map(step => <div key={`${agentRun.id}-${step.stepNumber}`} className="grid gap-2 rounded-2xl border border-white/10 bg-black/10 p-4 sm:grid-cols-[auto_1fr]"><span className="grid h-8 w-8 place-items-center rounded-full bg-teal-300 font-black text-civic-950">{step.stepNumber}</span><div><div className="flex flex-wrap items-center gap-2"><b className="text-sm">{step.toolName.replaceAll('_', ' ')}</b><span className="text-xs text-teal-100/60">{step.actionSummary}</span></div><p className="mt-1 text-sm leading-6 text-teal-50/80">{step.observationSummary}</p></div></div>)}</div></div>
            {agentRun.status === 'COMPLETED' ? <div className="rounded-2xl border border-amber-300/30 bg-amber-200/10 p-4"><label className="grid gap-2"><span className="text-xs font-black uppercase tracking-wider text-amber-200">Human review note</span><textarea rows="3" maxLength="1000" value={agentReviewNote} onChange={event => setAgentReviewNote(event.target.value)} className="rounded-2xl border-white/10 bg-white px-4 py-3 text-slate-950" placeholder="Why are you approving or rejecting this recommendation?" /></label><div className="mt-3 flex flex-wrap justify-end gap-2"><button type="button" onClick={() => reviewAgent('reject')} disabled={agentLoading} className="rounded-full border border-red-300/30 bg-red-400/10 px-5 py-2.5 text-sm font-black text-red-100">Reject</button><button type="button" onClick={() => reviewAgent('approve')} disabled={agentLoading} className="rounded-full bg-teal-300 px-5 py-2.5 text-sm font-black text-civic-950">Approve recommendation</button></div></div> : <p className="text-xs text-teal-100/70">Reviewed by {agentRun.reviewedBy || 'system'}{agentRun.reviewedAt ? ` on ${new Date(agentRun.reviewedAt).toLocaleString()}` : ''}.</p>}
          </div> : <div className="relative mt-5 rounded-2xl border border-dashed border-white/20 p-5 text-sm text-teal-50/70">No agent investigation exists for this issue yet.</div>}
        </article> : null}

        {selectedIssue ? <article className={`rounded-[1.75rem] border p-5 shadow-sm ${selectedIssue.status === 'RESOLVED' ? 'border-emerald-200 bg-emerald-50' : 'border-amber-200 bg-amber-50'}`}>
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <p className={`text-xs font-black uppercase tracking-[0.2em] ${selectedIssue.status === 'RESOLVED' ? 'text-emerald-700' : 'text-amber-700'}`}>Public proof</p>
              <h2 className="mt-1 text-2xl font-black text-slate-950">Resolution certificate</h2>
              <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-700">
                {selectedIssue.status === 'RESOLVED'
                  ? 'This issue is resolved. Open the public certificate to review the final note, timeline, evidence, and ledger hash.'
                  : 'Certificate preview is available now. It becomes official after the authority workflow marks this issue RESOLVED.'}
              </p>
            </div>
            <Link className={selectedIssue.status === 'RESOLVED' ? 'btn-primary' : 'btn-secondary'} to={`/issues/${selectedIssue.id}/certificate`}>
              {selectedIssue.status === 'RESOLVED' ? 'View resolution certificate' : 'Open certificate preview'}
            </Link>
          </div>
        </article> : null}

        {selectedIssue ? <article className="card">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.2em] text-civic-700">Admin evidence review</p>
              <h2 className="mt-1 text-2xl font-black">Media and image validation</h2>
              <p className="mt-2 text-sm text-slate-600">Admin-only controls for reviewing citizen evidence and removing inappropriate attachments.</p>
            </div>
          </div>
          <div className="mt-5 grid gap-4 md:grid-cols-2">
            {(selectedIssue.media || []).length ? selectedIssue.media.map(item => <div key={item.id} className="overflow-hidden rounded-2xl bg-slate-950 text-white">
              {item.mediaType === 'VIDEO'
                ? <video src={item.mediaUrl} controls preload="metadata" className="aspect-video w-full bg-black object-contain" />
                : <img src={item.mediaUrl} alt={item.originalFilename || selectedIssue.title} className="aspect-video w-full object-cover" />}
              <div className="grid gap-2 p-4 text-xs">
                <div className="flex items-center justify-between gap-3"><span className="truncate font-bold">{item.originalFilename || item.mediaType}</span><button onClick={() => deleteMedia(item.id)} className="font-black text-red-300 hover:text-red-200">Remove evidence</button></div>
                {item.validationStatus ? <div className="rounded-2xl bg-white p-3 text-slate-700"><span className="badge bg-slate-100 text-slate-700">{item.validationStatus.replaceAll('_', ' ')}</span>{item.validationConfidence != null ? <span className="ml-2 font-black">{item.validationConfidence}%</span> : null}<p className="mt-2 leading-5">{item.validationSummary}</p>{item.validationLabels ? <p className="mt-1 line-clamp-2 text-[11px] text-slate-400">{item.validationLabels}</p> : null}</div> : null}
              </div>
            </div>) : <p className="rounded-2xl border border-dashed p-5 text-center text-sm text-slate-500 md:col-span-2">No citizen media is attached to this issue.</p>}
          </div>
        </article> : null}

        <article className="card">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.2em] text-civic-700">Official action</p>
              <h2 className="mt-1 text-2xl font-black">Update status</h2>
            </div>
            {loadingWorkflow ? <span className="text-sm font-bold text-slate-400">Loading workflow...</span> : null}
          </div>
          {message ? <div className="mt-4 rounded-2xl bg-civic-50 p-4 text-sm font-bold text-civic-700">{message}</div> : null}
          {workflow?.allowedTransitions?.length ? <form className="mt-5 grid gap-3" onSubmit={updateStatus}>
            <label className="grid gap-1"><span className="text-xs font-bold uppercase tracking-wider text-slate-500">Move to</span><select required value={form.targetStatus} onChange={event => setForm({ ...form, targetStatus: event.target.value })} className="rounded-2xl border px-4 py-3">{workflow.allowedTransitions.map(status => <option key={status} value={status}>{status.replaceAll('_', ' ')}</option>)}</select></label>
            <label className="grid gap-1"><span className="text-xs font-bold uppercase tracking-wider text-slate-500">Authority actor</span><input required maxLength="100" value={form.actorName} onChange={event => setForm({ ...form, actorName: event.target.value })} className="rounded-2xl border px-4 py-3" /></label>
            <label className="grid gap-1"><span className="text-xs font-bold uppercase tracking-wider text-slate-500">Official note / resolution note</span><textarea required maxLength="1000" rows="4" value={form.note} onChange={event => setForm({ ...form, note: event.target.value })} className="rounded-2xl border px-4 py-3" placeholder="What action was taken? What is the resolution evidence?" /></label>
            <label className="grid gap-1"><span className="text-xs font-bold uppercase tracking-wider text-slate-500">Evidence URL optional</span><input maxLength="500" value={form.evidenceUrl} onChange={event => setForm({ ...form, evidenceUrl: event.target.value })} className="rounded-2xl border px-4 py-3" placeholder="Work order, repair photo, public notice link" /></label>
            <button disabled={updating} className="btn-primary disabled:opacity-60">{updating ? 'Saving official update...' : 'Save official update'}</button>
          </form> : null}
          {workflow && !workflow.allowedTransitions?.length ? <div className="mt-4 rounded-2xl bg-emerald-50 p-4 text-sm font-bold text-emerald-800">No further transition is available for this issue.</div> : null}
        </article>

        <article className="card">
          <p className="text-xs font-black uppercase tracking-[0.2em] text-civic-700">Audit trail</p>
          <h2 className="mt-1 text-2xl font-black">Public status history</h2>
          <div className="mt-5"><HistoryList history={workflow?.history || []} /></div>
        </article>
      </section>
    </section>
  </div>
}
