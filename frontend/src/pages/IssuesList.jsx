import { useEffect, useState } from 'react'
import api from '../api'
import { EmptyState, IssueCard, LoadingSpinner } from '../components'
import { clusterPublicIssues } from '../issueClusters'

export default function IssuesList() {
  const [items, setItems] = useState(null)
  useEffect(() => {
    api.get('/issues').then(async r => {
      setItems(await clusterPublicIssues(r.data))
    })
  }, [])
  if (!items) return <LoadingSpinner />
  return items.length ? <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{items.map(i => <IssueCard key={i.id} issue={i} />)}</div> : <EmptyState title="No issues yet" description="Be the first to report a civic issue." />
}
