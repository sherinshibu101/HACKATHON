import { useEffect, useRef, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'

const knowledge = [
  {
    id: 'report',
    title: 'How do I report an issue?',
    keywords: ['report', 'submit', 'new issue', 'complaint', 'pothole', 'leak', 'streetlight', 'waste', 'drainage'],
    answer: 'Open Report Issue, describe the problem, choose the matching category, confirm the map pin, and optionally attach photo or video evidence. The app checks for nearby duplicates before creating the report.',
    actions: [{ label: 'Report an issue', to: '/report' }],
  },
  {
    id: 'location',
    title: 'How does location work?',
    keywords: ['location', 'gps', 'map pin', 'latitude', 'longitude', 'address', 'permission', 'wrong place', 'accuracy'],
    answer: 'Use "Use my current location" and allow precise location access in your browser. If GPS accuracy is poor, search for a landmark or locality and move the map pin to the exact issue spot before confirming it.',
    actions: [{ label: 'Open location form', to: '/report' }, { label: 'View issue map', to: '/map' }],
  },
  {
    id: 'evidence',
    title: 'Why was my photo rejected?',
    keywords: ['photo', 'image', 'video', 'evidence', 'rejected', 'vision', 'mismatch', 'camera', 'gallery', 'upload'],
    answer: 'Google Cloud Vision checks whether an uploaded photo matches the selected civic category. A clear mismatch blocks submission. Remove the photo or select the correct category. Photos can come from your camera or gallery; videos are accepted as supporting evidence.',
    actions: [{ label: 'Add evidence', to: '/report' }],
  },
  {
    id: 'duplicate',
    title: 'What happens with duplicate reports?',
    keywords: ['duplicate', 'same issue', 'already reported', 'nearby', 'merged', 'existing issue', 'returned issue'],
    answer: 'The app checks for a similar unresolved issue nearby. Review the existing report first instead of creating another card. If a previously resolved problem has genuinely returned, you can choose "Report as returned issue" after the warning.',
    actions: [{ label: 'Browse existing issues', to: '/issues' }],
  },
  {
    id: 'verify',
    title: 'How do I verify an issue?',
    keywords: ['verify', 'verification', 'confirm', 'community verified', 'neighbor', 'three'],
    answer: 'Open the issue details and use Verify This Issue if you personally observed it. You can add an optional comment. Three community verifications move a reported issue to Community Verified.',
    actions: [{ label: 'Find an issue', to: '/issues' }],
  },
  {
    id: 'status',
    title: 'What do the statuses mean?',
    keywords: ['status', 'timeline', 'reported', 'analyzed', 'escalated', 'in progress', 'resolved', 'certificate'],
    answer: 'Reported means the complaint was received. AI Analyzed adds civic guidance. Community Verified requires three confirmations. Escalated means authority attention was requested. In Progress means work started, and Resolved means the authority closed it with a resolution record.',
    actions: [{ label: 'Track issues', to: '/issues' }],
  },
  {
    id: 'emergency',
    title: 'How do I request urgent action?',
    keywords: ['emergency', 'urgent', 'danger', 'escalate', 'email', 'authority', 'accident', 'critical'],
    answer: 'Open the issue details and use Request urgent authority action when there is immediate danger. Explain the risk clearly. If the AI does not classify it as urgent, you may still continue after acknowledging the warning; that override is recorded for accountability.',
    actions: [{ label: 'Choose an issue', to: '/issues' }],
  },
  {
    id: 'ai',
    title: 'What does the AI agent do?',
    keywords: ['ai', 'gemini', 'category', 'complaint draft', 'analysis', 'agent'],
    answer: 'The AI Civic Resolution Agent analyzes the report and prepares citizen-facing help such as a complaint draft. Internal severity, routing, risk, and dispatch recommendations are reserved for authority review.',
    actions: [{ label: 'View reported issues', to: '/issues' }],
  },
  {
    id: 'points',
    title: 'How do points and badges work?',
    keywords: ['points', 'badge', 'leaderboard', 'reward', 'gamification', 'score'],
    answer: 'Citizens earn 20 points for a report and 10 points for a verification. A report that becomes community verified earns a 25-point bonus. Rankings and earned badges appear on the leaderboard.',
    actions: [{ label: 'View leaderboard', to: '/leaderboard' }],
  },
  {
    id: 'privacy',
    title: 'Is my information public?',
    keywords: ['privacy', 'email public', 'personal', 'secure', 'data', 'ledger', 'hash'],
    answer: 'Issue details and public status history support civic transparency. Verification emails are not shown in the recent-verifications list. The public ledger hash is only a verification receipt; the private signing key and admin controls are never exposed to citizens.',
    actions: [],
  },
]

const quickQuestionIds = ['report', 'location', 'verify', 'status']

function normalize(value) {
  return value.toLowerCase().replace(/[^a-z0-9\s]/g, ' ').replace(/\s+/g, ' ').trim()
}

function findAnswer(question) {
  const normalized = normalize(question)
  const words = normalized.split(' ').filter(word => word.length > 2)
  let best = null
  let bestScore = 0

  knowledge.forEach(item => {
    const score = item.keywords.reduce((total, keyword) => {
      const cleanKeyword = normalize(keyword)
      if (normalized.includes(cleanKeyword)) return total + (cleanKeyword.includes(' ') ? 4 : 2)
      return total + cleanKeyword.split(' ').filter(word => words.includes(word)).length
    }, 0)
    if (score > bestScore) {
      best = item
      bestScore = score
    }
  })

  if (best && bestScore >= 2) return best
  return {
    id: 'fallback',
    answer: 'I could not confidently match that question yet. I can help with reporting, location, image evidence, duplicate reports, community verification, emergency escalation, status tracking, AI assistance, and points.',
    actions: [{ label: 'Browse all issues', to: '/issues' }, { label: 'Report an issue', to: '/report' }],
  }
}

function routeGreeting(pathname) {
  if (pathname === '/report') return 'Need help with location, evidence, or submitting this report?'
  if (pathname.startsWith('/issues/')) return 'I can explain verification, status, emergency escalation, or the complaint draft.'
  if (pathname === '/map') return 'Ask me about the map, clusters, heatmaps, or your current location.'
  return 'Ask me how to report, verify, track, or escalate a civic issue.'
}

export default function CitizenHelpChat() {
  const location = useLocation()
  const [open, setOpen] = useState(false)
  const [input, setInput] = useState('')
  const [typing, setTyping] = useState(false)
  const [messages, setMessages] = useState(() => [{
    id: 1,
    role: 'assistant',
    text: 'Hello! I am the Community Help Guide. I can explain how this website works and point you to the right page.',
    actions: [],
  }])
  const endRef = useRef(null)
  const inputRef = useRef(null)
  const timerRef = useRef(null)

  useEffect(() => {
    if (!open) return undefined
    inputRef.current?.focus()
    const closeOnEscape = event => {
      if (event.key === 'Escape') setOpen(false)
    }
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [open])

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, typing, open])

  useEffect(() => () => clearTimeout(timerRef.current), [])

  const ask = question => {
    const cleanQuestion = question.trim()
    if (!cleanQuestion || typing) return
    const answer = findAnswer(cleanQuestion)
    setMessages(current => [...current, {
      id: Date.now(), role: 'user', text: cleanQuestion, actions: [],
    }])
    setInput('')
    setTyping(true)
    timerRef.current = setTimeout(() => {
      setMessages(current => [...current, {
        id: Date.now() + 1, role: 'assistant', text: answer.answer, actions: answer.actions,
      }])
      setTyping(false)
    }, 350)
  }

  return (
    <aside className="no-print fixed bottom-4 right-4 z-[80] flex flex-col items-end sm:bottom-6 sm:right-6">
      {open ? (
        <section
          className="help-chat-enter mb-3 flex h-[min(650px,76vh)] w-[calc(100vw-2rem)] max-w-[410px] flex-col overflow-hidden rounded-[1.75rem] border border-civic-900/15 bg-[#fffef9] shadow-[0_28px_90px_rgba(8,47,45,0.3)]"
          role="dialog"
          aria-label="Community Help Guide"
        >
          <header className="relative overflow-hidden bg-civic-900 px-5 py-4 text-white">
            <div className="absolute -right-8 -top-12 h-28 w-28 rounded-full bg-teal-300/15" />
            <div className="relative flex items-start justify-between gap-4">
              <div className="flex items-center gap-3">
                <span className="grid h-10 w-10 shrink-0 place-items-center rounded-2xl bg-teal-300 font-black text-civic-950">CH</span>
                <div><h2 className="font-display text-lg font-black">Community Help Guide</h2><p className="text-xs text-teal-100">Website questions, answered instantly</p></div>
              </div>
              <button type="button" onClick={() => setOpen(false)} className="grid h-9 w-9 place-items-center rounded-full bg-white/10 transition hover:bg-white/20" aria-label="Close help chat">
                <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" /></svg>
              </button>
            </div>
          </header>

          <div className="border-b border-civic-900/10 bg-civic-50 px-5 py-3 text-xs font-semibold leading-5 text-civic-900">{routeGreeting(location.pathname)}</div>

          <div className="flex-1 space-y-4 overflow-y-auto px-4 py-5" aria-live="polite">
            {messages.map(message => (
              <div key={message.id} className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                <div className={`max-w-[88%] rounded-2xl px-4 py-3 text-sm leading-6 ${message.role === 'user' ? 'rounded-br-md bg-civic-700 text-white' : 'rounded-bl-md border border-civic-900/10 bg-white text-slate-700 shadow-sm'}`}>
                  <p>{message.text}</p>
                  {message.actions?.length ? <div className="mt-3 flex flex-wrap gap-2">{message.actions.map(action => <Link key={action.to} to={action.to} onClick={() => setOpen(false)} className="rounded-full bg-civic-50 px-3 py-1.5 text-xs font-black text-civic-800 transition hover:bg-civic-100">{action.label}</Link>)}</div> : null}
                </div>
              </div>
            ))}
            {messages.length === 1 ? <div className="flex flex-wrap gap-2">{quickQuestionIds.map(id => { const item = knowledge.find(entry => entry.id === id); return <button type="button" key={id} onClick={() => ask(item.title)} className="rounded-full border border-civic-900/10 bg-civic-50 px-3 py-2 text-left text-xs font-bold text-civic-900 transition hover:border-civic-500 hover:bg-civic-100">{item.title}</button> })}</div> : null}
            {typing ? <div className="flex justify-start"><div className="flex gap-1 rounded-2xl rounded-bl-md border border-civic-900/10 bg-white px-4 py-3" aria-label="Guide is typing"><span className="help-chat-dot" /><span className="help-chat-dot" /><span className="help-chat-dot" /></div></div> : null}
            <div ref={endRef} />
          </div>

          <form className="border-t border-civic-900/10 bg-white p-3" onSubmit={event => { event.preventDefault(); ask(input) }}>
            <div className="flex items-end gap-2 rounded-2xl border border-civic-900/15 bg-[#fbfaf4] p-2 focus-within:border-civic-600 focus-within:ring-4 focus-within:ring-civic-100">
              <label htmlFor="citizen-help-question" className="sr-only">Ask a website question</label>
              <textarea id="citizen-help-question" ref={inputRef} rows="1" maxLength="300" value={input} onChange={event => setInput(event.target.value)} onKeyDown={event => { if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); ask(input) } }} placeholder="Ask how something works..." className="max-h-24 min-h-10 flex-1 resize-none border-0 bg-transparent px-2 py-2 text-sm ring-0 focus:border-0 focus:ring-0" />
              <button type="submit" disabled={!input.trim() || typing} className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-civic-700 text-white transition hover:bg-civic-800 disabled:cursor-not-allowed disabled:opacity-40" aria-label="Send question">
                <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden="true"><path d="M4 12h14M13 6l6 6-6 6" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" /></svg>
              </button>
            </div>
            <p className="mt-2 text-center text-[10px] text-slate-400">Website guidance only. For immediate danger, contact local emergency services.</p>
          </form>
        </section>
      ) : null}

      <button type="button" onClick={() => setOpen(value => !value)} className="group flex items-center gap-3 rounded-full border border-white/20 bg-civic-900 py-2.5 pl-3 pr-5 text-white shadow-[0_18px_50px_rgba(8,47,45,0.35)] transition hover:-translate-y-1 hover:bg-civic-800" aria-expanded={open} aria-label="Open Community Help Guide">
        <span className="grid h-10 w-10 place-items-center rounded-full bg-teal-300 text-civic-950 transition group-hover:rotate-3">
          <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden="true"><path d="M5 5h14v10H9l-4 4V5z" fill="none" stroke="currentColor" strokeWidth="2" strokeLinejoin="round" /><path d="M8 9h8M8 12h5" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" /></svg>
        </span>
        <span className="text-left"><span className="block text-sm font-black">Need help?</span><span className="block text-[10px] text-teal-100">Ask the citizen guide</span></span>
      </button>
    </aside>
  )
}
