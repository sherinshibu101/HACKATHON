import { useEffect } from 'react'
import L from 'leaflet'
import { MapContainer, Marker, TileLayer, useMap, useMapEvents } from 'react-leaflet'
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png'
import markerIcon from 'leaflet/dist/images/marker-icon.png'
import markerShadow from 'leaflet/dist/images/marker-shadow.png'

const issueMarkerIcon = L.icon({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
})

function Recenter({ position }) {
  const map = useMap()
  useEffect(() => { map.setView(position, 17) }, [map, position[0], position[1]])
  return null
}

function MapClickHandler({ onPick }) {
  useMapEvents({ click: event => onPick(event.latlng.lat, event.latlng.lng) })
  return null
}

export default function LocationPicker({ latitude, longitude, onPick }) {
  const lat = Number(latitude)
  const lng = Number(longitude)
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
    return <div className="grid h-52 place-items-center rounded-2xl border border-dashed border-civic-300 bg-white text-center text-sm text-slate-500">Use your current location or enter coordinates to display the confirmation map.</div>
  }
  const position = [lat, lng]
  return <div className="overflow-hidden rounded-2xl border border-civic-200">
    <MapContainer center={position} zoom={17} className="h-72 w-full">
      <TileLayer attribution="&copy; OpenStreetMap contributors" url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
      <Recenter position={position} />
      <MapClickHandler onPick={onPick} />
      <Marker icon={issueMarkerIcon} position={position} draggable eventHandlers={{ dragend: event => { const point = event.target.getLatLng(); onPick(point.lat, point.lng) } }} />
    </MapContainer>
    <p className="bg-white px-4 py-3 text-xs text-slate-500">Click the map or drag the marker to the exact issue location.</p>
  </div>
}
