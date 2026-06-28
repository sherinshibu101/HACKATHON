import { Link } from 'react-router-dom'

const workflow = [
  ['01', 'Report', 'Citizen submits location, evidence, and civic category.'],
  ['02', 'Validate', 'Vision checks evidence and duplicate detection groups repeat cases.'],
  ['03', 'Resolve', 'Gemini drafts complaints while authorities act from the admin desk.'],
]

const signals = [
  ['AI Civic Agent', 'Complaint draft, routing intelligence, urgency signals'],
  ['Community Proof', 'Verification count, grouped duplicates, public history'],
  ['Authority Desk', 'Status workflow, dispatch notes, zero-trust audit trail'],
]

export default function Home() {
  return (
    <div className="grid gap-8">
      <section className="linear-panel px-6 py-10 md:px-10 md:py-14">
        <div className="relative grid gap-10 lg:grid-cols-[1.04fr_0.96fr] lg:items-center">
          <div className="animate-soft-rise">
            <p className="linear-kicker">Civic operating system</p>
            <h1 className="linear-title mt-5 max-w-4xl">Infrastructure issues, routed from citizen signal to accountable action.</h1>
            <p className="linear-muted mt-6 max-w-2xl">
              Community Hero AI turns local reports into validated civic cases with image checks, duplicate intelligence,
              AI-generated complaint drafts, community verification, and an authority workflow built for transparency.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link className="btn-primary" to="/report">Report an issue</Link>
              <Link className="btn-secondary" to="/map">Explore live map</Link>
              <Link className="btn-secondary" to="/dashboard">View dashboard</Link>
            </div>
          </div>

          <div className="animate-soft-rise rounded-[1.8rem] border border-civic-900/10 bg-white/80 p-4 shadow-civic" style={{ animationDelay: '120ms' }}>
            <div className="rounded-[1.35rem] border border-civic-900/10 bg-civic-50/70 p-4">
              <div className="flex items-center justify-between border-b border-civic-900/10 pb-4">
                <div>
                  <p className="text-xs font-bold uppercase tracking-[0.2em] text-civic-700">Active civic case</p>
                  <h2 className="mt-1 font-display text-2xl font-black tracking-[-0.05em] text-slate-950">Burst pipe near school road</h2>
                </div>
                <span className="badge border-red-200 bg-red-50 text-red-800">CRITICAL</span>
              </div>
              <div className="mt-4 grid gap-3 sm:grid-cols-3">
                <div className="rounded-2xl border border-civic-900/10 bg-white p-4">
                  <p className="text-xs font-semibold text-slate-600">Impact</p>
                  <p className="mt-1 font-display text-3xl font-black tracking-[-0.06em] text-civic-900">94</p>
                </div>
                <div className="rounded-2xl border border-civic-900/10 bg-white p-4">
                  <p className="text-xs font-semibold text-slate-600">Reports</p>
                  <p className="mt-1 font-display text-3xl font-black tracking-[-0.06em] text-civic-900">+3</p>
                </div>
                <div className="rounded-2xl border border-civic-900/10 bg-white p-4">
                  <p className="text-xs font-semibold text-slate-600">Status</p>
                  <p className="mt-2 text-sm font-black text-civic-800">IN PROGRESS</p>
                </div>
              </div>
              <div className="mt-4 rounded-2xl border border-civic-200 bg-white p-4">
                <p className="text-xs font-black uppercase tracking-[0.18em] text-civic-700">Gemini draft ready</p>
                <p className="mt-2 text-sm leading-6 text-slate-700">
                  Respected Sir/Madam, I would like to report a severe water leakage near the school road that requires urgent inspection...
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="grid gap-4 md:grid-cols-3">
        {workflow.map(([step, title, body]) => (
          <article key={step} className="card">
            <span className="text-xs font-black text-civic-700">{step}</span>
            <h2 className="mt-4 font-display text-2xl font-black tracking-[-0.05em]">{title}</h2>
            <p className="mt-3 text-sm leading-6 text-slate-600">{body}</p>
          </article>
        ))}
      </section>

      <section className="grid gap-4 lg:grid-cols-3">
        {signals.map(([title, body]) => (
          <article key={title} className="rounded-[1.75rem] border border-civic-900/10 bg-white/80 p-6 shadow-[0_16px_40px_rgba(15,79,78,0.08)] transition hover:-translate-y-1 hover:border-civic-300 hover:bg-white">
            <h3 className="font-display text-xl font-black tracking-[-0.04em]">{title}</h3>
            <p className="mt-3 text-sm leading-6 text-slate-600">{body}</p>
          </article>
        ))}
      </section>
    </div>
  )
}
