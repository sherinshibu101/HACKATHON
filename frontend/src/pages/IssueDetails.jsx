import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import api from '../api'
import { useAuth } from '../auth.jsx'
import { CategoryBadge, LoadingSpinner, StatusBadge } from '../components'

const severityStyles = {
  LOW: 'bg-emerald-100 text-emerald-800 ring-emerald-200',
  MEDIUM: 'bg-yellow-100 text-yellow-800 ring-yellow-200',
  HIGH: 'bg-orange-100 text-orange-800 ring-orange-200',
  CRITICAL: 'bg-red-100 text-red-800 ring-red-200',
}

function InsightCard({ eyebrow, value, className = '' }) {
  return <div className={`rounded-2xl border border-white/70 bg-white/85 p-5 shadow-sm backdrop-blur ${className}`}><p className="text-xs font-bold uppercase tracking-[0.18em] text-slate-500">{eyebrow}</p><div className="mt-2 text-lg font-black text-slate-900">{value || 'Analysis unavailable'}</div></div>
}

function CopyCard({ title, children }) {
  return <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"><h4 className="font-black text-slate-900">{title}</h4><p className="mt-3 whitespace-pre-wrap text-sm leading-7 text-slate-600">{children || 'Not generated yet.'}</p></article>
}

const imageValidationTone = {
  VALID: 'bg-emerald-100 text-emerald-800',
  SUSPECT: 'bg-red-100 text-red-800',
  UNAVAILABLE: 'bg-slate-100 text-slate-700',
  FAILED: 'bg-orange-100 text-orange-800',
  NOT_APPLICABLE: 'bg-slate-100 text-slate-700',
}

function MediaGallery({ issue, onDelete, canManage = false }) {
  const media = issue.media || []
  if (!media.length) return null
  return <section className="card"><div className="flex flex-wrap items-end justify-between gap-3"><div><p className="text-xs font-black uppercase tracking-[0.2em] text-civic-700">Citizen evidence</p><h3 className="mt-1 text-2xl font-black">Photos and video</h3></div><span className="badge bg-slate-100 text-slate-700">{media.length} attachment{media.length === 1 ? '' : 's'}</span></div><div className="mt-5 grid gap-4 md:grid-cols-2">{media.map(item => <article key={item.id} className="group relative overflow-hidden rounded-2xl bg-slate-950 shadow-lg">
    {item.mediaType === 'VIDEO'
      ? <video src={item.mediaUrl} controls preload="metadata" className="aspect-video w-full bg-black object-contain">Your browser does not support video playback.</video>
      : <a href={item.mediaUrl} target="_blank" rel="noreferrer"><img src={item.mediaUrl} alt={item.originalFilename || issue.title} className="aspect-video w-full object-cover transition duration-500 group-hover:scale-105" /></a>}
    <div className="grid gap-2 px-4 py-3 text-xs text-white"><div className="flex items-center justify-between gap-3"><span className="truncate">{item.originalFilename || item.mediaType}</span>{canManage && item.id !== 'legacy' ? <button onClick={() => onDelete(item.id)} className="font-bold text-red-300 hover:text-red-200">Remove</button> : null}</div>{item.validationStatus ? <div className="rounded-2xl bg-white p-3 text-slate-700"><span className={`badge ${imageValidationTone[item.validationStatus] || imageValidationTone.UNAVAILABLE}`}>{item.validationStatus.replaceAll('_', ' ')}</span>{item.validationConfidence != null ? <span className="ml-2 font-black">{item.validationConfidence}%</span> : null}<p className="mt-2 leading-5">{item.validationSummary}</p>{item.validationLabels ? <p className="mt-1 line-clamp-2 text-[11px] text-slate-400">{item.validationLabels}</p> : null}</div> : null}</div>
  </article>)}</div></section>
}

function StatusTimeline({ issue, workflow }) {
  const statusIndex = { REPORTED: 0, VERIFIED: 2, ESCALATED: 3, IN_PROGRESS: 4, RESOLVED: 5 }
  const reached = statusIndex[issue.status] ?? 0
  const current = issue.status === 'REPORTED' && issue.aiGeneratedAt ? 1 : reached
  const historyStatuses = new Set((workflow?.history || []).map(item => item.toStatus))
  const steps = [
    ['Reported', true],
    ['AI Analyzed', Boolean(issue.aiGeneratedAt)],
    ['Community Verified', issue.communityVerified || historyStatuses.has('VERIFIED')],
    ['Escalated', historyStatuses.has('ESCALATED') || reached >= 3],
    ['In Progress', historyStatuses.has('IN_PROGRESS') || reached >= 4],
    ['Resolved', historyStatuses.has('RESOLVED') || reached >= 5],
  ]
  return <section className="card"><h3 className="text-xl font-black">Accountability timeline</h3><div className="mt-6 grid grid-cols-2 gap-y-6 sm:grid-cols-3 lg:grid-cols-6">{steps.map(([label, done], index) => <div key={label} className="relative flex flex-col items-center text-center">
    {index > 0 ? <div className={`absolute right-1/2 top-4 hidden h-0.5 w-full lg:block ${done ? 'bg-civic-500' : 'bg-slate-200'}`} /> : null}
    <div className={`relative z-10 grid h-9 w-9 place-items-center rounded-full border-4 text-xs font-black ${done ? 'border-civic-100 bg-civic-600 text-white' : 'border-slate-100 bg-slate-200 text-slate-500'} ${index === current ? 'ring-4 ring-civic-200' : ''}`}>{done ? 'OK' : index + 1}</div>
    <p className={`mt-2 text-xs font-bold ${index === current ? 'text-civic-700' : 'text-slate-500'}`}>{label}</p>
  </div>)}</div></section>
}

function StatusHistory({ history = [] }) {
  if (!history.length) return <p className="rounded-2xl border border-dashed border-slate-200 p-5 text-sm text-slate-500">Status history will appear here as the issue moves through the workflow.</p>
  return <div className="grid gap-3">{history.slice().reverse().map(item => <article key={item.id || `${item.toStatus}-${item.createdAt}`} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
    <div className="flex flex-wrap items-center justify-between gap-2"><div className="flex flex-wrap items-center gap-2"><StatusBadge status={item.toStatus} />{item.fromStatus ? <span className="text-xs font-bold text-slate-400">from {item.fromStatus.replaceAll('_', ' ')}</span> : null}</div><time className="text-xs font-bold text-slate-400">{new Date(item.createdAt).toLocaleString()}</time></div>
    <p className="mt-3 text-sm leading-6 text-slate-700">{item.note}</p>
    <div className="mt-3 flex flex-wrap items-center gap-2 text-xs font-bold text-slate-500"><span>{item.actorName}</span><span className="rounded-full bg-slate-100 px-2 py-1">{item.actorType}</span>{item.evidenceUrl ? <a className="text-civic-700 underline" href={item.evidenceUrl} target="_blank" rel="noreferrer">View evidence</a> : null}</div>
  </article>)}</div>
}

function CitizenAgentPanel({ agent }) {
  if (!agent) return null
  return <section className="relative overflow-hidden rounded-[2rem] border border-civic-900/10 bg-civic-950 p-6 text-white shadow-[0_24px_70px_rgba(8,47,45,0.25)] md:p-8">
    <div className="absolute -right-16 -top-20 h-56 w-56 rounded-full bg-teal-300/15 blur-2xl" />
    <div className="relative flex flex-wrap items-start justify-between gap-4">
      <div><p className="text-xs font-black uppercase tracking-[0.24em] text-teal-300">Autonomous case review</p><h3 className="mt-2 font-display text-2xl font-black">Civic Case Manager</h3></div>
      <span className="rounded-full border border-white/15 bg-white/10 px-3 py-1 text-xs font-black">{agent.status.replaceAll('_', ' ')}</span>
    </div>
    <p className="relative mt-5 max-w-3xl text-sm leading-7 text-teal-50">{agent.citizenSummary || 'The case manager is preparing a citizen-safe summary.'}</p>
    <div className="relative mt-5 grid gap-3 sm:grid-cols-[1fr_auto]">
      <div className="rounded-2xl border border-white/10 bg-white/10 p-4"><p className="text-[10px] font-black uppercase tracking-[0.2em] text-teal-300">Recommended next step</p><p className="mt-2 text-sm font-semibold leading-6">{agent.recommendedNextAction || 'Continue monitoring the public timeline.'}</p></div>
      <div className="rounded-2xl border border-white/10 bg-white/10 p-4 text-center"><p className="text-[10px] font-black uppercase tracking-[0.2em] text-teal-300">Signal confidence</p><p className="mt-1 text-3xl font-black">{agent.confidence ?? 0}%</p></div>
    </div>
    <p className="relative mt-4 text-xs text-teal-100/70">Recommendations never change official status without authority approval.</p>
  </section>
}

export default function IssueDetails() {
  const { id } = useParams()
  const location = useLocation()
  const auth = useAuth()
  const [issue, setIssue] = useState(null)
  const [verifications, setVerifications] = useState([])
  const [duplicates, setDuplicates] = useState([])
  const [verificationForm, setVerificationForm] = useState({ verifierName: '', verifierEmail: '', comment: '' })
  const [emergencyForm, setEmergencyForm] = useState({ requesterName: '', requesterEmail: '', reason: '' })
  const [loading, setLoading] = useState(true)
  const [analyzing, setAnalyzing] = useState(false)
  const [verifying, setVerifying] = useState(false)
  const [error, setError] = useState('')
  const [verificationMessage, setVerificationMessage] = useState('')
  const [emergencyMessage, setEmergencyMessage] = useState('')
  const [emergencyOverrideRequired, setEmergencyOverrideRequired] = useState(false)
  const [requestingEmergency, setRequestingEmergency] = useState(false)
  const [mediaNotice, setMediaNotice] = useState(location.state?.notice || '')
  const [emailPreview, setEmailPreview] = useState(null)
  const [emailLoading, setEmailLoading] = useState(false)
  const [emailSending, setEmailSending] = useState(false)
  const [emailConfirmed, setEmailConfirmed] = useState(false)
  const [emailMessage, setEmailMessage] = useState('')
  const [workflow, setWorkflow] = useState(null)
  const [agentSummary, setAgentSummary] = useState(null)

  const load = async () => {
    const issueResult = await api.get(`/issues/${id}`)
    setIssue(issueResult.data)
    setError('')
    const [verificationResult, duplicateResult, workflowResult, agentResult] = await Promise.allSettled([
      api.get(`/issues/${id}/verifications`), api.get(`/issues/${id}/duplicates`), api.get(`/issues/${id}/status-workflow`), api.get(`/issues/${id}/agent/public-summary`),
    ])
    setVerifications(verificationResult.status === 'fulfilled' ? verificationResult.value.data : [])
    setDuplicates(duplicateResult.status === 'fulfilled' ? duplicateResult.value.data : [])
    setWorkflow(workflowResult.status === 'fulfilled' ? workflowResult.value.data : null)
    setAgentSummary(agentResult.status === 'fulfilled' ? agentResult.value.data || null : null)
  }

  useEffect(() => {
    load().catch(() => setError('This issue could not be loaded.')).finally(() => setLoading(false))
  }, [id])

  const regenerate = async () => {
    setAnalyzing(true)
    setError('')
    try {
      const { data } = await api.post(`/issues/${id}/analyze`)
      setIssue(data)
      if (data.aiAnalysisMessage !== 'AI analysis completed successfully.') setError(data.aiAnalysisMessage)
    } catch (err) {
      setError(err.response?.data?.message || 'AI analysis could not be regenerated.')
    } finally { setAnalyzing(false) }
  }

  const verify = async (event) => {
    event.preventDefault()
    setVerifying(true)
    setVerificationMessage('')
    try {
      await api.post(`/issues/${id}/verify`, verificationForm)
      await load()
      setVerificationForm({ verifierName: '', verifierEmail: '', comment: '' })
      setVerificationMessage('Thank you. Your verification now strengthens this community report.')
    } catch (err) {
      setVerificationMessage(err.response?.data?.message || 'Verification could not be recorded.')
    } finally { setVerifying(false) }
  }

  const requestEmergencyEscalation = async (event) => {
    event.preventDefault()
    setRequestingEmergency(true)
    setEmergencyMessage('')
    try {
      const { data } = await api.post(`/issues/${id}/authority-email/emergency-request`, {
        requesterName: emergencyForm.requesterName || auth.user?.displayName || 'Concerned citizen',
        requesterEmail: emergencyForm.requesterEmail || auth.user?.email || '',
        reason: emergencyForm.reason,
        overrideAiAssessment: emergencyOverrideRequired,
      })
      setEmergencyMessage(data.message)
      setEmergencyOverrideRequired(Boolean(data.confirmationRequired))
      if (!data.confirmationRequired) {
        setEmergencyForm({ requesterName: '', requesterEmail: '', reason: '' })
        await load()
      }
    } catch (err) {
      setEmergencyMessage(err.response?.data?.message || 'Emergency escalation request could not be submitted.')
    } finally { setRequestingEmergency(false) }
  }

  const deleteMedia = async (mediaId) => {
    if (!window.confirm('Remove this media attachment?')) return
    try {
      await api.delete(`/issues/${id}/media/${mediaId}`)
      await load()
      setMediaNotice('Media attachment removed.')
    } catch (err) {
      setMediaNotice(err.response?.data?.message || 'Media attachment could not be removed.')
    }
  }

  const openEmailPreview = async () => {
    setEmailLoading(true)
    setEmailMessage('')
    try {
      const { data } = await api.get(`/issues/${id}/authority-email/preview`)
      setEmailPreview(data)
      setEmailConfirmed(false)
    } catch (err) {
      setEmailMessage(err.response?.data?.message || 'Email preview could not be prepared.')
    } finally { setEmailLoading(false) }
  }

  const sendAuthorityEmail = async () => {
    setEmailSending(true)
    try {
      const { data } = await api.post(`/issues/${id}/authority-email/send`, {
        recipient: emailPreview.recipient,
        subject: emailPreview.subject,
        body: emailPreview.body,
        confirmed: emailConfirmed,
      })
      setEmailMessage(data.message)
      setEmailPreview(null)
      await load()
    } catch (err) {
      setEmailMessage(err.response?.data?.message || 'The complaint email could not be sent.')
    } finally { setEmailSending(false) }
  }

  if (loading) return <LoadingSpinner />
  if (!issue) return <div className="card text-red-700">{error}</div>
  const severity = issue.severity || 'PENDING'

  return <div className="mx-auto grid max-w-5xl gap-6">
    {mediaNotice ? <div className="flex items-center justify-between gap-3 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800"><span>{mediaNotice}</span><button className="font-bold" onClick={() => setMediaNotice('')}>Dismiss</button></div> : null}
    <section className="card grid gap-4">
      <div className="flex flex-wrap items-start justify-between gap-4"><div><h2 className="text-3xl font-black">{issue.title}</h2><p className="mt-1 text-slate-600">{[issue.locality, issue.city, issue.state].filter(Boolean).join(', ') || issue.ward}</p></div><div className="flex flex-wrap gap-2"><StatusBadge status={issue.status} />{issue.communityVerified ? <span className="badge bg-emerald-100 text-emerald-800 ring-1 ring-emerald-200">Community Verified</span> : null}</div></div>
      <div className="flex flex-wrap items-center gap-2"><CategoryBadge category={issue.category} /><span className="badge bg-slate-900 text-white">{issue.verificationCount} community verification{issue.verificationCount === 1 ? '' : 's'}</span></div>
      <p className="leading-7 text-slate-700">{issue.description}</p>
      <div className="grid gap-3 text-sm md:grid-cols-2"><div className="rounded-2xl bg-slate-50 p-4"><b className="block">Reported location</b><span className="mt-1 block text-slate-600">{issue.formattedAddress || [issue.locality, issue.city, issue.district, issue.state, issue.postalCode, issue.country].filter(Boolean).join(', ')}</span><span className="mt-2 block text-xs text-slate-400">{issue.latitude}, {issue.longitude} / {issue.locationSource || 'MANUAL'}{issue.locationAccuracyMeters ? ` / accuracy ${Math.round(issue.locationAccuracyMeters)} m` : ''}</span></div><div className="rounded-2xl bg-slate-50 p-4"><b className="block">Civic routing</b><span className="mt-1 block text-slate-600">Ward: {issue.ward || 'UNASSIGNED'}</span><span className="mt-2 block text-xs text-slate-400">Created {new Date(issue.createdAt).toLocaleString()}</span></div></div>
    </section>

    <MediaGallery issue={issue} onDelete={deleteMedia} canManage={auth.isAdmin} />

    <StatusTimeline issue={issue} workflow={workflow} />

    {!auth.isAdmin ? <CitizenAgentPanel agent={agentSummary} /> : null}

    <section className="card">
      <div className="flex flex-wrap items-start justify-between gap-3"><div><p className="text-xs font-black uppercase tracking-[0.2em] text-civic-700">Public audit trail</p><h3 className="mt-1 text-2xl font-black">Status history</h3><p className="mt-2 text-sm text-slate-600">Official admin updates appear here for citizen transparency.</p></div><div className="flex flex-wrap gap-2">{issue.status === 'RESOLVED' ? <Link to={`/issues/${issue.id}/certificate`} className="btn-primary py-2 text-sm">View resolution certificate</Link> : <Link to={`/issues/${issue.id}/certificate`} className="btn-secondary py-2 text-sm">Certificate preview</Link>}{auth.isAdmin ? <Link to="/admin/authorities" className="btn-secondary py-2 text-sm">Admin portal</Link> : null}</div></div>
      <div className="mt-5"><StatusHistory history={workflow?.history || []} /></div>
    </section>

    <section className="relative overflow-hidden rounded-[2rem] bg-gradient-to-br from-slate-950 via-civic-700 to-emerald-500 p-[1px] shadow-2xl shadow-civic-700/20">
      <div className="relative overflow-hidden rounded-[calc(2rem-1px)] bg-gradient-to-br from-slate-50 via-civic-50 to-emerald-50 p-6 md:p-8">
        <div className="pointer-events-none absolute -right-16 -top-20 h-64 w-64 rounded-full bg-emerald-300/30 blur-3xl" />
        <div className="relative flex flex-wrap items-center justify-between gap-4"><div><p className="text-xs font-black uppercase tracking-[0.25em] text-civic-700">Gemini-powered assistance</p><h3 className="mt-2 text-2xl font-black text-slate-950 md:text-3xl">AI Civic Resolution Agent</h3><p className="mt-2 max-w-2xl text-sm text-slate-600">{auth.isAdmin ? 'Internal routing intelligence for authority review and action planning.' : 'Citizen-facing help to prepare a clear civic complaint from your report.'}</p></div>{auth.isAdmin ? <button onClick={regenerate} disabled={analyzing} className="btn-primary disabled:cursor-wait disabled:opacity-60">{analyzing ? 'Analyzing...' : 'Regenerate AI Analysis'}</button> : null}</div>
        {(error || !issue.aiGeneratedAt) ? <div className="relative mt-5 rounded-2xl bg-red-50 p-4 text-sm text-red-700 ring-1 ring-red-200">{error || issue.aiAnalysisMessage}</div> : null}
        {analyzing ? <div className="relative mt-5 h-1.5 overflow-hidden rounded-full bg-civic-100"><div className="ai-progress h-full rounded-full bg-civic-600" /></div> : null}
        {auth.isAdmin ? <div className="relative mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3"><InsightCard eyebrow="Severity" value={<span className={`inline-flex rounded-full px-3 py-1 text-sm ring-1 ${severityStyles[severity] || 'bg-slate-100 text-slate-700 ring-slate-200'}`}>{severity}</span>} /><InsightCard eyebrow="Impact score" value={issue.impactScore == null ? null : <span><span className="text-3xl">{issue.impactScore}</span><span className="text-slate-400"> / 100</span></span>} /><InsightCard eyebrow="Recommended department" value={issue.recommendedDepartment} /><InsightCard eyebrow="Resolution urgency" value={issue.resolutionUrgency} /><InsightCard eyebrow="Risk assessment" value={<p className="text-sm font-medium leading-6">{issue.riskExplanation || 'Analysis unavailable'}</p>} className="sm:col-span-2" /></div> : null}
        <div className={`relative mt-4 grid gap-4 ${auth.isAdmin ? 'lg:grid-cols-3' : 'lg:grid-cols-1'}`}>{auth.isAdmin ? <CopyCard title="Resolution Plan">{issue.suggestedAction}</CopyCard> : null}<CopyCard title="Complaint Draft">{issue.complaintDraft}</CopyCard>{auth.isAdmin ? <CopyCard title="Escalation Message">{issue.escalationMessage}</CopyCard> : null}</div>
        {auth.isAdmin ? <div className="relative mt-5 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-civic-200 bg-white/80 p-4"><div><h4 className="font-black">Send the complaint to the concerned authority</h4><p className="mt-1 text-xs text-slate-500">Admin-only action. Review the exact recipient and message before anything is sent.</p>{issue.authorityEmailSentAt ? <p className="mt-2 text-xs font-bold text-emerald-700">Last sent to {issue.authorityEmailRecipient} on {new Date(issue.authorityEmailSentAt).toLocaleString()}</p> : null}{emailMessage ? <p className="mt-2 text-xs font-bold text-civic-700">{emailMessage}</p> : null}</div><button onClick={openEmailPreview} disabled={emailLoading} className="btn-primary disabled:opacity-60">{emailLoading ? 'Preparing email...' : 'Email concerned authority'}</button></div> : null}
      </div>
    </section>

    {auth.isAdmin && issue.dispatchDepartment ? <section className="relative overflow-hidden rounded-[2rem] bg-gradient-to-br from-emerald-950 via-slate-900 to-civic-800 p-[1px] shadow-2xl">
      <div className="relative overflow-hidden rounded-[calc(2rem-1px)] bg-white p-6 md:p-8">
        <div className="pointer-events-none absolute -right-20 -top-20 h-72 w-72 rounded-full bg-emerald-300/30 blur-3xl" />
        <div className="relative flex flex-wrap items-start justify-between gap-4">
          <div><p className="text-xs font-black uppercase tracking-[0.24em] text-emerald-700">Admin dispatch analysis</p><h3 className="mt-2 text-2xl font-black text-slate-950">Dispatch Copilot Recommendation</h3><p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600">Internal AI analysis for admin review. This helps authorities route and prioritize the issue.</p></div>
        </div>
        <div className="relative mt-6 grid gap-4 lg:grid-cols-3">
          <InsightCard eyebrow="Dispatch Department" value={issue.dispatchDepartment} />
          <InsightCard eyebrow="Dispatch Priority" value={<span className={`inline-flex rounded-full px-3 py-1 text-sm font-black ${issue.dispatchPriority === 'CRITICAL' ? 'bg-red-100 text-red-800 ring-red-200' : issue.dispatchPriority === 'HIGH' ? 'bg-orange-100 text-orange-800 ring-orange-200' : issue.dispatchPriority === 'MEDIUM' ? 'bg-yellow-100 text-yellow-800 ring-yellow-200' : 'bg-emerald-100 text-emerald-800 ring-emerald-200'}`}>{issue.dispatchPriority}</span>} />
          <InsightCard eyebrow="Analyzed At" value={issue.dispatchAnalyzedAt ? new Date(issue.dispatchAnalyzedAt).toLocaleString() : 'N/A'} />
        </div>
        {issue.dispatchCitizenNotification ? <div className="relative mt-4 grid gap-4 lg:grid-cols-3"><CopyCard title="Draft Citizen Notification">{issue.dispatchCitizenNotification}</CopyCard></div> : null}
      </div>
    </section> : null}

    <section className="grid gap-6 lg:grid-cols-3">
      {!auth.isAdmin ? <form className="card grid gap-4" onSubmit={requestEmergencyEscalation}><div><p className="text-xs font-black uppercase tracking-[0.2em] text-red-600">Emergency escalation</p><h3 className="mt-1 text-2xl font-black">Request urgent authority action</h3><p className="mt-2 text-sm text-slate-600">Use this only if the issue poses immediate danger. Critical or high-impact issues may be emailed automatically; others are queued for admin review.</p></div>
        <input maxLength="100" className="rounded-2xl border px-4 py-3" placeholder="Your name" value={emergencyForm.requesterName || auth.user?.displayName || ''} onChange={event => setEmergencyForm({ ...emergencyForm, requesterName: event.target.value })} />
        <input type="email" maxLength="254" className="rounded-2xl border px-4 py-3" placeholder="Email optional" value={emergencyForm.requesterEmail || auth.user?.email || ''} onChange={event => setEmergencyForm({ ...emergencyForm, requesterEmail: event.target.value })} />
        <textarea required maxLength="1000" rows="4" className="rounded-2xl border px-4 py-3" placeholder="Why is this an emergency? Example: children are crossing a flooded road near school." value={emergencyForm.reason} onChange={event => setEmergencyForm({ ...emergencyForm, reason: event.target.value })} />
        {emergencyMessage ? <p className={`rounded-2xl p-3 text-sm ${emergencyOverrideRequired ? 'border border-amber-300 bg-amber-50 text-amber-900' : 'bg-red-50 text-red-700'}`}>{emergencyMessage}</p> : null}
        {emergencyOverrideRequired ? <p className="rounded-2xl border border-red-200 bg-red-50 p-3 text-xs font-semibold leading-5 text-red-800">Only continue if you genuinely believe there is immediate danger. Your manual override will be recorded in the public accountability history.</p> : null}
        <button disabled={requestingEmergency} className="rounded-full bg-red-600 px-5 py-3 text-sm font-black text-white transition hover:bg-red-700 disabled:opacity-60">{requestingEmergency ? 'Requesting escalation...' : emergencyOverrideRequired ? 'Send escalation anyway' : 'Request emergency escalation'}</button>
      </form> : null}
      <form className="card grid gap-4" onSubmit={verify}><div><p className="text-xs font-black uppercase tracking-[0.2em] text-civic-700">Community evidence</p><h3 className="mt-1 text-2xl font-black">Verify This Issue</h3><p className="mt-2 text-sm text-slate-600">Three independent confirmations move a reported issue to Community Verified.</p></div>
        <input required maxLength="100" className="rounded-2xl border px-4 py-3" placeholder="Your name" value={verificationForm.verifierName} onChange={event => setVerificationForm({ ...verificationForm, verifierName: event.target.value })} />
        <input type="email" className="rounded-2xl border px-4 py-3" placeholder="Email (optional, kept private)" value={verificationForm.verifierEmail} onChange={event => setVerificationForm({ ...verificationForm, verifierEmail: event.target.value })} />
        <textarea maxLength="1000" rows="4" className="rounded-2xl border px-4 py-3" placeholder="What did you observe? (optional)" value={verificationForm.comment} onChange={event => setVerificationForm({ ...verificationForm, comment: event.target.value })} />
        {verificationMessage ? <p className="rounded-2xl bg-civic-50 p-3 text-sm text-civic-700">{verificationMessage}</p> : null}
        <button disabled={verifying} className="btn-primary disabled:opacity-60">{verifying ? 'Recording verification...' : 'Verify This Issue'}</button>
      </form>
      <section className={`card ${auth.isAdmin ? 'lg:col-span-2' : ''}`}><div className="flex items-center justify-between"><h3 className="text-2xl font-black">Recent verifications</h3><span className="text-3xl font-black text-civic-700">{verifications.length}</span></div><div className="mt-4 grid gap-3">{verifications.length ? verifications.map(item => <article key={item.id} className="rounded-2xl bg-slate-50 p-4"><div className="flex justify-between gap-3"><b>{item.verifierName}</b><time className="text-xs text-slate-400">{new Date(item.createdAt).toLocaleDateString()}</time></div><p className="mt-2 text-sm leading-6 text-slate-600">{item.comment || 'Confirmed this issue at the reported location.'}</p></article>) : <p className="rounded-2xl border border-dashed p-6 text-center text-sm text-slate-500">Be the first neighbor to verify this report.</p>}</div></section>
    </section>

    {duplicates.length ? <section className="card"><p className="text-xs font-black uppercase tracking-[0.2em] text-orange-600">Possible related reports</p><h3 className="mt-1 text-2xl font-black">Nearby issues that may describe the same problem</h3><div className="mt-4 grid gap-3 md:grid-cols-2">{duplicates.map(item => <Link key={item.id} to={`/issues/${item.id}`} className="rounded-2xl border border-orange-100 bg-orange-50 p-4 transition hover:border-orange-400"><h4 className="font-bold">{item.title}</h4><p className="mt-2 text-xs text-slate-500">{Math.round(item.distanceMeters)} m away / {item.status}</p></Link>)}</div></section> : null}

    {emailPreview ? <div className="fixed inset-0 z-[70] grid place-items-center bg-slate-950/70 p-4 backdrop-blur-sm" role="dialog" aria-modal="true"><div className="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-3xl bg-white p-6 shadow-2xl md:p-8"><div className="flex items-start justify-between gap-4"><div><p className="text-xs font-black uppercase tracking-[0.2em] text-civic-700">Authority email preview</p><h3 className="mt-1 text-2xl font-black">Review before sending</h3></div><button className="rounded-full bg-slate-100 px-4 py-2 text-sm font-bold" onClick={() => setEmailPreview(null)}>Close</button></div><div className={`mt-5 rounded-2xl p-4 text-sm ${emailPreview.configured ? 'bg-amber-50 text-amber-800' : 'bg-red-50 text-red-700'}`}>{emailPreview.warning}</div><div className="mt-5 grid gap-4"><label className="grid gap-1"><span className="text-xs font-bold uppercase tracking-wider text-slate-500">To</span><input readOnly value={emailPreview.recipient || 'No authority configured'} className="rounded-2xl border bg-slate-50 px-4 py-3" /></label><label className="grid gap-1"><span className="text-xs font-bold uppercase tracking-wider text-slate-500">Subject</span><input readOnly value={emailPreview.subject} className="rounded-2xl border bg-slate-50 px-4 py-3" /></label><label className="grid gap-1"><span className="text-xs font-bold uppercase tracking-wider text-slate-500">Complaint</span><textarea readOnly rows="13" value={emailPreview.body} className="rounded-2xl border bg-slate-50 px-4 py-3 leading-6" /></label></div><label className="mt-5 flex items-start gap-3 rounded-2xl border p-4 text-sm"><input type="checkbox" className="mt-1 h-4 w-4" checked={emailConfirmed} onChange={event => setEmailConfirmed(event.target.checked)} /><span>I reviewed the recipient and complaint and authorize Community Hero AI to send this email.</span></label><div className="mt-5 flex justify-end gap-3"><button className="btn-secondary" onClick={() => setEmailPreview(null)}>Cancel</button><button className="btn-primary disabled:cursor-not-allowed disabled:opacity-50" disabled={!emailPreview.configured || !emailConfirmed || emailSending} onClick={sendAuthorityEmail}>{emailSending ? 'Sending...' : 'Confirm and send email'}</button></div></div></div> : null}
  </div>
}
