import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import api from '../api'
import { LoadingSpinner, StatusBadge } from '../components'

const formatDate = value => value ? new Date(value).toLocaleString() : 'Not recorded'
const formatPlace = certificate => [
  certificate.formattedAddress,
  certificate.locality,
  certificate.city,
  certificate.district,
  certificate.state,
].filter(Boolean)[0] || [certificate.locality, certificate.city, certificate.state].filter(Boolean).join(', ') || certificate.ward || 'Location unavailable'

export default function ResolutionCertificate() {
  const { id } = useParams()
  const [certificate, setCertificate] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    setError('')
    api.get(`/issues/${id}/certificate`)
      .then(({ data }) => setCertificate(data))
      .catch(err => setError(err.response?.data?.message || 'Resolution certificate could not be loaded.'))
  }, [id])

  if (error) return <div className="card text-red-700">{error}</div>
  if (!certificate) return <LoadingSpinner />

  return (
    <div className="mx-auto grid max-w-5xl gap-6">
      <div className="no-print flex flex-wrap items-center justify-between gap-3">
        <Link to={`/issues/${certificate.issueId}`} className="btn-secondary">Back to issue</Link>
        <button onClick={() => window.print()} className="btn-primary">Print / Save as PDF</button>
      </div>

      <section className="relative overflow-hidden rounded-[2rem] border border-civic-900/10 bg-white p-8 shadow-[0_24px_70px_rgba(15,79,78,0.12)] print:shadow-none">
        <div className="pointer-events-none absolute -right-20 -top-24 h-72 w-72 rounded-full bg-civic-200/50 blur-3xl" />
        <div className="pointer-events-none absolute bottom-0 left-10 h-48 w-48 rounded-full bg-amber-100/70 blur-3xl" />

        <div className="relative border-b border-civic-900/10 pb-6">
          <p className="linear-kicker">Public accountability proof</p>
          <div className="mt-3 flex flex-wrap items-start justify-between gap-5">
            <div>
              <h1 className="font-display text-4xl font-black tracking-[-0.05em] text-slate-950 md:text-5xl">Resolution Certificate</h1>
              <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-600">{certificate.availabilityMessage}</p>
            </div>
            <div className="rounded-2xl border border-civic-900/10 bg-civic-50 p-4 text-right">
              <p className="text-xs font-black uppercase tracking-[0.18em] text-civic-700">Certificate No.</p>
              <p className="mt-1 font-mono text-lg font-black text-slate-950">{certificate.certificateNumber}</p>
            </div>
          </div>
          {!certificate.certificateAvailable ? <div className="mt-5 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm font-semibold text-amber-800">This is a preview. The official certificate appears after an authority marks the issue resolved.</div> : null}
        </div>

        <div className="relative mt-6 grid gap-4 md:grid-cols-3">
          <div className="rounded-2xl bg-slate-50 p-4 md:col-span-2">
            <p className="text-xs font-black uppercase tracking-[0.18em] text-slate-500">Issue</p>
            <h2 className="mt-2 text-2xl font-black text-slate-950">{certificate.title}</h2>
            <p className="mt-3 text-sm leading-6 text-slate-700">{certificate.description}</p>
          </div>
          <div className="rounded-2xl bg-slate-50 p-4">
            <p className="text-xs font-black uppercase tracking-[0.18em] text-slate-500">Final Status</p>
            <div className="mt-3"><StatusBadge status={certificate.status} /></div>
            <p className="mt-4 text-sm text-slate-600">{certificate.category?.replaceAll('_', ' ')} / {certificate.severity || 'Severity pending'}</p>
          </div>
        </div>

        <div className="relative mt-4 grid gap-4 md:grid-cols-4">
          <Info label="Reported" value={formatDate(certificate.reportedAt)} />
          <Info label="Resolved" value={formatDate(certificate.resolvedAt)} />
          <Info label="Resolution Time" value={certificate.certificateAvailable ? `${certificate.resolutionHours} hours` : 'Pending'} />
          <Info label="Community Proof" value={`${certificate.verificationCount} verification${certificate.verificationCount === 1 ? '' : 's'}`} />
        </div>

        <div className="relative mt-6 grid gap-4 lg:grid-cols-[1.1fr_0.9fr]">
          <section className="rounded-3xl border border-civic-900/10 bg-white p-5">
            <p className="text-xs font-black uppercase tracking-[0.18em] text-civic-700">Resolution summary</p>
            <p className="mt-3 whitespace-pre-wrap text-sm leading-7 text-slate-700">{certificate.resolutionSummary}</p>
            {certificate.resolutionEvidenceUrl ? <a href={certificate.resolutionEvidenceUrl} target="_blank" rel="noreferrer" className="mt-4 inline-flex text-sm font-black text-civic-800 underline">View resolution evidence</a> : null}
          </section>
          <section className="rounded-3xl border border-civic-900/10 bg-white p-5">
            <p className="text-xs font-black uppercase tracking-[0.18em] text-civic-700">Civic location</p>
            <p className="mt-3 text-sm leading-7 text-slate-700">{formatPlace(certificate)}</p>
            <p className="mt-2 text-xs font-semibold text-slate-500">Ward: {certificate.ward || 'UNASSIGNED'}</p>
            <p className="mt-1 text-xs font-semibold text-slate-500">{certificate.latitude}, {certificate.longitude}</p>
          </section>
        </div>

        <section className="relative mt-6 rounded-3xl border border-civic-900/10 bg-civic-50 p-5">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.18em] text-civic-700">Zero Trust ledger proof</p>
              <h3 className="mt-1 text-xl font-black text-slate-950">{certificate.ledgerVerified ? 'Ledger verified' : 'Ledger warning'}</h3>
              <p className="mt-2 text-sm leading-6 text-slate-700">{certificate.ledgerMessage}</p>
            </div>
            <span className={`rounded-full px-3 py-1 text-xs font-black ${certificate.resolvedOnTime ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'}`}>{certificate.slaAssessment}</span>
          </div>
          <p className="mt-4 break-all rounded-2xl bg-white p-3 font-mono text-xs text-slate-600">Audit hash: {certificate.auditHash || 'Unavailable'}</p>
          {certificate.ledgerEntryId ? <p className="mt-2 text-xs font-semibold text-slate-500">Latest issue ledger entry: #{certificate.ledgerEntryId}</p> : null}
        </section>

        <section className="relative mt-6">
          <p className="text-xs font-black uppercase tracking-[0.18em] text-civic-700">Public timeline</p>
          <div className="mt-4 grid gap-3">
            {certificate.timeline?.length ? certificate.timeline.map(item => (
              <article key={item.id || `${item.toStatus}-${item.createdAt}`} className="rounded-2xl border border-slate-200 bg-white p-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <StatusBadge status={item.toStatus} />
                  <time className="text-xs font-bold text-slate-500">{formatDate(item.createdAt)}</time>
                </div>
                <p className="mt-3 text-sm leading-6 text-slate-700">{item.note}</p>
                <p className="mt-2 text-xs font-bold text-slate-500">{item.actorName} / {item.actorType}</p>
              </article>
            )) : <p className="rounded-2xl border border-dashed border-slate-300 p-5 text-center text-sm text-slate-500">No public workflow history recorded.</p>}
          </div>
        </section>
      </section>
    </div>
  )
}

function Info({ label, value }) {
  return <div className="rounded-2xl bg-slate-50 p-4">
    <p className="text-xs font-black uppercase tracking-[0.16em] text-slate-500">{label}</p>
    <p className="mt-2 text-sm font-black leading-6 text-slate-900">{value}</p>
  </div>
}
