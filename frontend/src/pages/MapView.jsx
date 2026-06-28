import { useEffect, useMemo, useState } from 'react'
import L from 'leaflet'
import 'leaflet.markercluster'
import 'leaflet.heat'
import { CircleMarker, MapContainer, Popup, TileLayer, useMap } from 'react-leaflet'
import api from '../api'
import { LoadingSpinner } from '../components'

const fallbackCenter = [20.5937, 78.9629]
const severityWeight = { LOW: 0.35, MEDIUM: 0.55, HIGH: 0.8, CRITICAL: 1 }

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, char => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;',
  }[char]))
}

function issuePopup(issue) {
  const place = issue.formattedAddress || [issue.locality, issue.city, issue.state].filter(Boolean).join(', ')
  return `<div style="display:grid;gap:6px;min-width:190px">
    <strong>${escapeHtml(issue.title)}</strong>
    <span>${escapeHtml(place)}</span>
    <span>${escapeHtml(issue.category)} / ${escapeHtml(issue.status)}</span>
    <span>Impact: ${escapeHtml(issue.impactScore ?? 'TBD')} / Severity: ${escapeHtml(issue.severity ?? 'TBD')}</span>
    <a style="font-weight:700;color:#0f766e" href="/issues/${issue.id}">View Details</a>
  </div>`
}

function validIssues(items) {
  return items.filter(issue => Number.isFinite(Number(issue.latitude)) && Number.isFinite(Number(issue.longitude)))
}

function ClusterLayer({ issues, enabled }) {
  const map = useMap()
  useEffect(() => {
    if (!enabled) return undefined
    const cluster = L.markerClusterGroup({
      chunkedLoading: true,
      maxClusterRadius: 55,
      spiderfyOnMaxZoom: true,
      showCoverageOnHover: false,
    })
    validIssues(issues).forEach(issue => {
      L.marker([Number(issue.latitude), Number(issue.longitude)])
        .bindPopup(issuePopup(issue))
        .addTo(cluster)
    })
    map.addLayer(cluster)
    return () => map.removeLayer(cluster)
  }, [enabled, issues, map])
  return null
}

function HeatLayer({ issues, enabled }) {
  const map = useMap()
  useEffect(() => {
    if (!enabled) return undefined
    const points = validIssues(issues).map(issue => [
      Number(issue.latitude),
      Number(issue.longitude),
      severityWeight[issue.severity] || 0.5,
    ])
    const layer = L.heatLayer(points, {
      radius: 32,
      blur: 24,
      maxZoom: 17,
      gradient: { 0.25: '#22c55e', 0.5: '#facc15', 0.75: '#fb923c', 1: '#ef4444' },
    })
    map.addLayer(layer)
    return () => map.removeLayer(layer)
  }, [enabled, issues, map])
  return null
}

function MapAutoView({ issues, currentLocation }) {
  const map = useMap()
  useEffect(() => {
    if (currentLocation) {
      map.setView(currentLocation, 15, { animate: true })
      return
    }
    const coordinates = validIssues(issues).map(issue => [Number(issue.latitude), Number(issue.longitude)])
    if (coordinates.length > 1) map.fitBounds(coordinates, { padding: [40, 40] })
    else if (coordinates.length === 1) map.setView(coordinates[0], 15)
  }, [currentLocation, issues, map])
  return null
}

export default function MapView() {
  const [items, setItems] = useState(null)
  const [currentLocation, setCurrentLocation] = useState(null)
  const [locationMessage, setLocationMessage] = useState('Finding your current location...')
  const [layerMode, setLayerMode] = useState('BOTH')

  useEffect(() => {
    api.get('/issues').then(r => setItems(r.data)).catch(() => setItems([]))
    if (!navigator.geolocation) {
      setLocationMessage('Browser location is not supported. Showing reported issue locations.')
      return
    }
    navigator.geolocation.getCurrentPosition(position => {
      setCurrentLocation([position.coords.latitude, position.coords.longitude])
      setLocationMessage(`Showing your current location. Accuracy about ${Math.round(position.coords.accuracy)} m.`)
    }, error => {
      setLocationMessage(error.code === 1
        ? 'Location permission was denied. Allow browser location to center the map on you.'
        : 'Current location could not be detected. Showing reported issue locations.')
    }, { enableHighAccuracy: true, timeout: 12000, maximumAge: 30000 })
  }, [])

  const mapCenter = useMemo(() => {
    if (currentLocation) return currentLocation
    const firstIssue = validIssues(items || [])[0]
    return firstIssue ? [Number(firstIssue.latitude), Number(firstIssue.longitude)] : fallbackCenter
  }, [currentLocation, items])

  if (!items) return <LoadingSpinner />
  const clustered = layerMode === 'CLUSTERS' || layerMode === 'BOTH'
  const heated = layerMode === 'HEAT' || layerMode === 'BOTH'

  return <div className="grid gap-5">
    <section className="card">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div><p className="text-xs font-black uppercase tracking-[0.2em] text-civic-700">Interactive civic intelligence</p><h1 className="mt-1 text-3xl font-black">Issue Heatmap and Clusters</h1><p className="mt-2 text-sm text-slate-600">Clusters keep high-volume maps fast. Heatmaps reveal neighborhoods with concentrated civic pressure.</p></div>
        <div className="flex flex-wrap gap-2">{[
          ['BOTH', 'Clusters + Heat'],
          ['CLUSTERS', 'Clusters'],
          ['HEAT', 'Heatmap'],
        ].map(([value, label]) => <button key={value} onClick={() => setLayerMode(value)} className={`rounded-full px-4 py-2 text-sm font-black ${layerMode === value ? 'bg-civic-600 text-white' : 'bg-slate-100 text-slate-600'}`}>{label}</button>)}</div>
      </div>
      <div className="mt-4 flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-slate-50 p-4 text-sm text-slate-600"><span>{locationMessage}</span><button className="font-bold text-civic-700 underline" onClick={() => currentLocation && setCurrentLocation([...currentLocation])}>Re-center on me</button></div>
    </section>

    <div className="card overflow-hidden p-0">
      <MapContainer center={mapCenter} zoom={currentLocation ? 15 : 12} className="h-[70vh] w-full">
        <TileLayer attribution="&copy; OpenStreetMap contributors" url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
        <MapAutoView issues={items} currentLocation={currentLocation} />
        <HeatLayer issues={items} enabled={heated} />
        <ClusterLayer issues={items} enabled={clustered} />
        {currentLocation ? <CircleMarker center={currentLocation} radius={10} pathOptions={{ color: '#0f766e', fillColor: '#14b8a6', fillOpacity: 0.75, weight: 3 }}><Popup>Your current location</Popup></CircleMarker> : null}
      </MapContainer>
    </div>
  </div>
}
