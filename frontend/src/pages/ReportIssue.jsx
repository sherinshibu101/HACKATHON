import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api from '../api'
import CameraCapture from '../CameraCapture'
import LocationPicker from '../LocationPicker'
import { useAuth } from '../auth.jsx'
import { validateIssueImage } from '../visionValidation'

const initialForm = {
  title: '', reporterName: '', reporterEmail: '', description: '', category: 'POTHOLE', latitude: '', longitude: '',
  country: '', state: '', district: '', city: '', locality: '', ward: '', postalCode: '',
  formattedAddress: '', locationAccuracyMeters: null, locationSource: 'MANUAL',
}
const imageTypes = ['image/jpeg', 'image/png', 'image/webp']
const videoTypes = ['video/mp4', 'video/webm']
const validationTone = {
  CHECKING: 'bg-blue-100 text-blue-800',
  VALID: 'bg-emerald-100 text-emerald-800',
  SUSPECT: 'bg-red-100 text-red-800',
  UNAVAILABLE: 'bg-slate-100 text-slate-700',
  FAILED: 'bg-orange-100 text-orange-800',
}

const blockingImageStatuses = new Set(['SUSPECT'])
const PRECISE_LOCATION_METERS = 100
const MAX_AUTOMATIC_LOCATION_METERS = 1000

function tempId() {
  return window.crypto?.randomUUID?.() || `${Date.now()}-${Math.random()}`
}

function MediaPreview({ item, onRemove }) {
  return <div className="relative overflow-hidden rounded-2xl bg-slate-950">
    {item.kind === 'IMAGE'
      ? <img src={item.previewUrl} alt={item.file.name} className="h-40 w-full object-cover" />
      : <video src={item.previewUrl} className="h-40 w-full object-cover" muted controls />}
    <button type="button" onClick={onRemove} className="absolute right-2 top-2 rounded-full bg-slate-950/80 px-3 py-1 text-xs font-bold text-white">Remove</button>
    <div className="grid gap-2 px-3 py-2 text-xs text-white"><p className="truncate">{item.file.name}</p>{item.kind === 'IMAGE' && item.validationStatus ? <div className="rounded-2xl bg-white p-3 text-slate-700"><span className={`badge ${validationTone[item.validationStatus] || validationTone.UNAVAILABLE}`}>{item.validationStatus === 'CHECKING' ? 'Checking image...' : item.validationStatus.replaceAll('_', ' ')}</span>{item.validationConfidence != null ? <span className="ml-2 font-black">{item.validationConfidence}%</span> : null}<p className="mt-2 leading-5">{item.validationSummary}</p>{item.validationLabels ? <p className="mt-1 line-clamp-2 text-[11px] text-slate-400">{item.validationLabels}</p> : null}</div> : null}</div>
  </div>
}

export default function ReportIssue() {
  const nav = useNavigate()
  const auth = useAuth()
  const [form, setForm] = useState(initialForm)
  const [media, setMedia] = useState([])
  const [submitting, setSubmitting] = useState(false)
  const [checking, setChecking] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)
  const [stage, setStage] = useState('')
  const [duplicates, setDuplicates] = useState([])
  const [error, setError] = useState('')
  const [cameraMode, setCameraMode] = useState(null)
  const [locating, setLocating] = useState(false)
  const [locationAccuracy, setLocationAccuracy] = useState(null)
  const [resolvingLocation, setResolvingLocation] = useState(false)
  const [locationMessage, setLocationMessage] = useState('')
  const [locationConfirmed, setLocationConfirmed] = useState(false)
  const [locationQuery, setLocationQuery] = useState('')
  const [locationResults, setLocationResults] = useState([])
  const [searchingLocation, setSearchingLocation] = useState(false)

  const payload = () => ({
    ...form,
    reporterName: form.reporterName || auth.user?.displayName || 'Community Member',
    reporterEmail: form.reporterEmail || auth.user?.email || '',
    latitude: Number(form.latitude),
    longitude: Number(form.longitude),
  })
  const imageCount = media.filter(item => item.kind === 'IMAGE').length
  const videoCount = media.filter(item => item.kind === 'VIDEO').length
  const blockedImageEvidence = media.filter(item => item.kind === 'IMAGE' && blockingImageStatuses.has(item.validationStatus))
  const blockedImageMessage = blockedImageEvidence.length
    ? 'This report cannot be submitted because one or more uploaded photos do not match the selected civic category. Remove the mismatched photo or choose the correct category, then try again.'
    : ''

  const validateImage = async (id, file, category = form.category) => {
    try {
      const result = await validateIssueImage(file, category)
      setMedia(current => current.map(item => item.id === id ? {
        ...item,
        ...result,
      } : item))
    } catch (err) {
      setMedia(current => current.map(item => item.id === id ? {
        ...item,
        validationStatus: 'FAILED',
        validationConfidence: 0,
        validationSummary: err.response?.data?.message || err.message || 'Google Cloud Vision validation failed before submission. Manual review recommended.',
        validationLabels: '',
        validatedAt: new Date().toISOString(),
      } : item))
    }
  }

  const addImages = (event) => {
    const selected = [...(event.target.files || [])]
    const valid = selected.filter(file => imageTypes.includes(file.type) && file.size <= 5 * 1024 * 1024)
    if (valid.length !== selected.length) setError('Some images were skipped. Use JPG, PNG, or WebP files under 5 MB.')
    if (imageCount + valid.length > 5) {
      setError('You can attach a maximum of 5 images.')
      event.target.value = ''
      return
    }
    const nextItems = valid.map(file => ({
      id: tempId(),
      file,
      kind: 'IMAGE',
      previewUrl: URL.createObjectURL(file),
      validationStatus: 'CHECKING',
      validationSummary: 'Google Cloud Vision is checking whether this image matches the selected category.',
    }))
    setMedia(current => [...current, ...nextItems])
    nextItems.forEach(item => validateImage(item.id, item.file))
    event.target.value = ''
  }

  const addVideo = (event) => {
    const file = event.target.files?.[0]
    if (!file) return
    if (videoCount || !videoTypes.includes(file.type) || file.size > 50 * 1024 * 1024) {
      setError(videoCount ? 'Only one video can be attached.' : 'Use an MP4 or WebM video under 50 MB.')
      event.target.value = ''
      return
    }
    setMedia(current => [...current, { id: tempId(), file, kind: 'VIDEO', previewUrl: URL.createObjectURL(file) }])
    setError('')
    event.target.value = ''
  }

  const removeMedia = (index) => {
    setMedia(current => {
      URL.revokeObjectURL(current[index].previewUrl)
      return current.filter((_, itemIndex) => itemIndex !== index)
    })
  }

  const addCapturedMedia = (file, kind) => {
    if (kind === 'IMAGE' && imageCount >= 5) {
      setError('You can attach a maximum of 5 images.')
      return
    }
    if (kind === 'VIDEO' && videoCount) {
      setError('Only one video can be attached.')
      return
    }
    if ((kind === 'IMAGE' && file.size > 5 * 1024 * 1024) || (kind === 'VIDEO' && file.size > 50 * 1024 * 1024)) {
      setError(kind === 'IMAGE' ? 'Captured image exceeds 5 MB.' : 'Captured video exceeds 50 MB. Record a shorter video.')
      return
    }
    const item = {
      id: tempId(),
      file,
      kind,
      previewUrl: URL.createObjectURL(file),
      validationStatus: kind === 'IMAGE' ? 'CHECKING' : undefined,
      validationSummary: kind === 'IMAGE' ? 'Google Cloud Vision is checking whether this image matches the selected category.' : undefined,
    }
    setMedia(current => [...current, item])
    if (kind === 'IMAGE') validateImage(item.id, item.file)
    setError('')
  }

  const resolveAddress = async (latitude, longitude, source, accuracy = null, confirmed = true) => {
    const numericLatitude = Number(latitude)
    const numericLongitude = Number(longitude)
    if (!Number.isFinite(numericLatitude) || !Number.isFinite(numericLongitude)) {
      setError('Enter valid coordinates or search for a place first.')
      return
    }
    const lat = numericLatitude.toFixed(6)
    const lng = numericLongitude.toFixed(6)
    setForm(current => ({
      ...current,
      latitude: lat,
      longitude: lng,
      locationSource: source,
      locationAccuracyMeters: accuracy,
      country: '', state: '', district: '', city: '', locality: '', ward: '', postalCode: '', formattedAddress: '',
    }))
    setLocationAccuracy(accuracy == null ? null : Math.round(accuracy))
    setLocationConfirmed(confirmed)
    setResolvingLocation(true)
    setLocationMessage('Looking up the address...')
    try {
      const { data } = await api.get('/location/reverse', { params: { latitude: lat, longitude: lng } })
      setForm(current => ({
        ...current,
        country: data.country || '',
        state: data.state || '',
        district: data.district || '',
        city: data.city || '',
        locality: data.locality || '',
        ward: data.ward || '',
        postalCode: data.postalCode || '',
        formattedAddress: data.formattedAddress || '',
      }))
      setLocationMessage(data.message)
    } catch {
      setLocationMessage('Coordinates were captured, but the address could not be detected. Please complete it manually.')
    } finally { setResolvingLocation(false) }
  }

  const useCurrentLocation = () => {
    if (!navigator.geolocation) {
      setError('Location services are not supported by this browser.')
      return
    }
    setLocating(true)
    setError('')
    setLocationMessage('Waiting for a precise location reading...')
    let bestPosition = null
    let watchId = null
    let finished = false

    const stopWatching = () => {
      if (watchId != null) navigator.geolocation.clearWatch(watchId)
    }
    const finishWithPosition = position => {
      if (finished) return
      finished = true
      stopWatching()
      const accuracy = Math.round(position.coords.accuracy)
      if (accuracy > MAX_AUTOMATIC_LOCATION_METERS) {
        setLocationAccuracy(accuracy)
        setLocationConfirmed(false)
        setLocationMessage(`The browser only provided an approximate ${Math.round(accuracy / 1000)} km area, so it was not used. Search for your locality or landmark below.`)
        setError('Your device could not provide a precise location. Search for Kottayam or your nearby landmark, then confirm the map pin.')
        setLocating(false)
        return
      }
      const precise = accuracy <= PRECISE_LOCATION_METERS
      resolveAddress(position.coords.latitude, position.coords.longitude, 'GPS', accuracy, precise)
        .then(() => {
          if (!precise) setLocationMessage(`Location is only accurate to about ${accuracy} m. Click or drag the map pin to confirm the exact issue location.`)
        })
        .finally(() => setLocating(false))
    }

    const timeoutId = window.setTimeout(() => {
      if (bestPosition) finishWithPosition(bestPosition)
      else {
        finished = true
        stopWatching()
        setError('Your location could not be determined. Search for your locality or enter coordinates manually.')
        setLocationMessage('No location reading was received.')
        setLocating(false)
      }
    }, 15000)

    watchId = navigator.geolocation.watchPosition(position => {
      if (!bestPosition || position.coords.accuracy < bestPosition.coords.accuracy) bestPosition = position
      setLocationAccuracy(Math.round(bestPosition.coords.accuracy))
      if (bestPosition.coords.accuracy <= PRECISE_LOCATION_METERS) {
        window.clearTimeout(timeoutId)
        finishWithPosition(bestPosition)
      }
    }, locationError => {
      if (locationError.code === 1) {
        window.clearTimeout(timeoutId)
        finished = true
        stopWatching()
        setError('Location permission was denied. Allow precise location access, or search for your locality below.')
        setLocating(false)
      }
    }, { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 })
  }

  const searchForLocation = async () => {
    const query = locationQuery.trim()
    if (query.length < 2) {
      setError('Enter a locality, landmark, or full address to search.')
      return
    }
    setSearchingLocation(true)
    setLocationResults([])
    setError('')
    try {
      const { data } = await api.get('/location/search', { params: { query } })
      setLocationResults(data)
      if (!data.length) setError('No matching place was found. Add a nearby landmark, district, or postal code and try again.')
    } catch {
      setError('Location search is temporarily unavailable. You can still enter coordinates manually.')
    } finally {
      setSearchingLocation(false)
    }
  }

  const createIssue = async () => {
    if (!locationConfirmed) {
      setError('Confirm the exact issue location by selecting a search result, looking up the coordinates, or moving the map pin.')
      return
    }
    if (blockedImageEvidence.length) {
      setError(blockedImageMessage)
      setDuplicates([])
      return
    }
    setSubmitting(true)
    setDuplicates([])
    setError('')
    setStage('AI Civic Resolution Agent is analyzing your report...')
    let issueId
    try {
      const { data } = await api.post('/issues', payload())
      issueId = data.id
      if (media.length) {
        setStage('Uploading your photo and video evidence...')
        const mediaData = new FormData()
        media.forEach(item => mediaData.append('files', item.file))
        mediaData.append('validationResults', JSON.stringify(media.map(item => ({
          validationStatus: item.validationStatus,
          validationConfidence: item.validationConfidence,
          validationSummary: item.validationSummary,
          validationLabels: item.validationLabels,
          validatedAt: item.validatedAt,
        }))))
        await api.post(`/issues/${issueId}/media`, mediaData, {
          onUploadProgress: event => setUploadProgress(event.total ? Math.round((event.loaded * 100) / event.total) : 0),
        })
      }
      nav(`/issues/${issueId}`)
    } catch (err) {
      const message = err.response?.data?.message || 'Your report could not be submitted. Please try again.'
      if (issueId) {
        nav(`/issues/${issueId}`, { state: { notice: `The report was saved, but media upload failed: ${message}` } })
      } else {
        setError(message)
        setSubmitting(false)
        setStage('')
      }
    }
  }

  const submit = async (event) => {
    event.preventDefault()
    if (!locationConfirmed) {
      setError('Confirm the exact issue location by selecting a search result, looking up the coordinates, or moving the map pin.')
      return
    }
    if (blockedImageEvidence.length) {
      setError(blockedImageMessage)
      setDuplicates([])
      return
    }
    setChecking(true)
    setError('')
    try {
      const { data } = await api.post('/issues/check-duplicates', payload())
      if (data.duplicateWarning) setDuplicates(data.possibleDuplicateIssues)
      else await createIssue()
    } catch (err) {
      setError(err.response?.data?.message || 'We could not check nearby reports. Please try again.')
    } finally { setChecking(false) }
  }

  const validatingImages = media.some(item => item.validationStatus === 'CHECKING')
  const busy = checking || submitting || validatingImages
  const resolvedDuplicate = duplicates.find(issue => issue.status === 'RESOLVED')
  const activeDuplicateCount = duplicates.filter(issue => issue.status !== 'RESOLVED').length
  const duplicateModalTitle = resolvedDuplicate && !activeDuplicateCount
    ? 'This issue was already marked resolved here.'
    : 'Possible similar issue already reported nearby.'
  const duplicateModalDescription = resolvedDuplicate && !activeDuplicateCount
    ? 'A matching issue at this location has already been resolved. Please open the existing issue first; only continue if the same problem has returned after repair.'
    : 'Verifying an existing report can build a stronger community signal. You can still create a separate report if this is a different problem.'
  return <>
    <form className="card mx-auto grid max-w-3xl gap-5" onSubmit={submit}>
      <div><p className="text-xs font-black uppercase tracking-[0.2em] text-civic-700">Citizen report</p><h2 className="mt-1 text-3xl font-black">Report an issue</h2><p className="mt-2 text-sm text-slate-600">Add clear visual evidence to help neighbors and civic teams understand the problem.</p></div>
      {stage ? <div className="rounded-2xl border border-civic-200 bg-civic-50 p-4 text-sm font-semibold text-civic-700">{stage}{uploadProgress ? <div className="mt-3 h-2 overflow-hidden rounded-full bg-white"><div className="h-full bg-civic-600 transition-all" style={{ width: `${uploadProgress}%` }} /></div> : null}</div> : null}
      {checking ? <div className="rounded-2xl bg-slate-50 p-4 text-sm font-semibold text-slate-600">Checking nearby community reports...</div> : null}
      {error ? <div className="rounded-2xl bg-red-50 p-4 text-sm text-red-700">{error}</div> : null}

      <section className="grid gap-4 rounded-3xl border border-emerald-200 bg-emerald-50 p-5">
        <div><h3 className="font-black">Citizen contributor</h3><p className="mt-1 text-xs text-slate-600">This powers the civic leaderboard. Email is optional and only used to group your own contributions.</p></div>
        <div className="grid gap-4 sm:grid-cols-2">
          <label className="grid gap-2"><span className="text-sm font-bold">Display name</span><input maxLength="100" className="rounded-2xl border bg-white px-4 py-3" placeholder="Asha, Team NSS, Ward 4 Resident" value={form.reporterName || auth.user?.displayName || ''} onChange={event => setForm({ ...form, reporterName: event.target.value })} /></label>
          <label className="grid gap-2"><span className="text-sm font-bold">Email optional</span><input type="email" maxLength="254" className="rounded-2xl border bg-white px-4 py-3" placeholder="you@example.com" value={form.reporterEmail || auth.user?.email || ''} onChange={event => setForm({ ...form, reporterEmail: event.target.value })} /></label>
        </div>
        <p className="rounded-2xl bg-white p-3 text-xs font-semibold text-emerald-700">Reporting earns 20 points. If neighbors verify your report, you can unlock Impact Maker badges.</p>
      </section>

      <label className="grid gap-2"><span className="text-sm font-bold">Title</span><input required maxLength="150" className="rounded-2xl border px-4 py-3" placeholder="Large pothole near the market" value={form.title} onChange={event => setForm({ ...form, title: event.target.value })} /></label>
      <label className="grid gap-2"><span className="text-sm font-bold">Description</span><textarea required maxLength="5000" rows="4" className="rounded-2xl border px-4 py-3" placeholder="Describe what you observed and any immediate risks" value={form.description} onChange={event => setForm({ ...form, description: event.target.value })} /></label>
      <section className="grid gap-4 rounded-3xl border border-civic-200 bg-civic-50 p-5"><div className="flex flex-wrap items-center justify-between gap-3"><div><h3 className="font-black">Issue location</h3><p className="mt-1 text-xs text-slate-600">Use precise location, search for the area, then confirm the exact map pin.</p></div><button type="button" onClick={useCurrentLocation} disabled={locating || resolvingLocation} className="btn-secondary">{locating ? 'Finding a precise location...' : 'Use my current location'}</button></div>
        {locationAccuracy != null ? <p className={`rounded-xl p-3 text-xs font-semibold ${locationAccuracy > MAX_AUTOMATIC_LOCATION_METERS ? 'bg-red-50 text-red-700' : locationAccuracy > PRECISE_LOCATION_METERS ? 'bg-amber-50 text-amber-800' : 'bg-emerald-50 text-emerald-700'}`}>Location accuracy: approximately {locationAccuracy >= 1000 ? `${Math.round(locationAccuracy / 1000)} km` : `${locationAccuracy} m`}.</p> : null}
        {locationMessage ? <p className="rounded-xl bg-white p-3 text-xs text-slate-600">{resolvingLocation ? 'Looking up the address...' : locationMessage}</p> : null}
        <div className="grid gap-2"><label className="text-sm font-bold" htmlFor="location-search">Search locality, landmark, or address</label><div className="flex gap-2"><input id="location-search" className="min-w-0 flex-1 rounded-2xl border bg-white px-4 py-3" placeholder="Example: Kottayam Railway Station, Kerala" value={locationQuery} onChange={event => setLocationQuery(event.target.value)} onKeyDown={event => { if (event.key === 'Enter') { event.preventDefault(); searchForLocation() } }} /><button type="button" className="btn-secondary" disabled={searchingLocation} onClick={searchForLocation}>{searchingLocation ? 'Searching...' : 'Search'}</button></div></div>
        {locationResults.length ? <div className="grid gap-2 rounded-2xl bg-white p-2">{locationResults.map(result => <button type="button" key={`${result.latitude}-${result.longitude}`} onClick={() => { setLocationResults([]); setLocationQuery(result.formattedAddress); resolveAddress(result.latitude, result.longitude, 'SEARCH', null, true) }} className="rounded-xl px-3 py-3 text-left text-sm text-slate-700 transition hover:bg-civic-50"><b className="block text-civic-800">{result.city || result.locality || 'Search result'}</b><span className="mt-1 block text-xs text-slate-500">{result.formattedAddress}</span></button>)}</div> : null}
        <div className="grid gap-4 sm:grid-cols-2">{['latitude','longitude'].map(key => <label className="grid gap-2" key={key}><span className="text-sm font-bold capitalize">{key}</span><input required type="number" step="any" className="rounded-2xl border bg-white px-4 py-3" value={form[key]} onChange={event => { setForm({ ...form, [key]: event.target.value, locationSource: 'MANUAL' }); setLocationConfirmed(false); setLocationAccuracy(null) }} /></label>)}</div>
        <div className="flex flex-wrap items-center justify-between gap-3"><span className={`text-xs font-bold ${locationConfirmed ? 'text-emerald-700' : 'text-amber-700'}`}>{locationConfirmed ? 'Exact location confirmed' : 'Location still needs confirmation'}</span><button type="button" disabled={!form.latitude || !form.longitude || resolvingLocation} onClick={() => resolveAddress(form.latitude, form.longitude, 'MANUAL', null, true)} className="text-sm font-bold text-civic-700 underline">Look up and confirm coordinates</button></div>
        <LocationPicker latitude={form.latitude} longitude={form.longitude} onPick={(lat, lng) => resolveAddress(lat, lng, 'MAP_PIN', null, true)} />
      </section>

      <section className="grid gap-4 rounded-3xl border border-slate-200 p-5"><div><h3 className="font-black">Detected address</h3><p className="mt-1 text-xs text-slate-500">Confirm these details. Ward is optional when it cannot be detected reliably.</p></div><div className="grid gap-4 sm:grid-cols-2">{[
        ['country', true], ['state', true], ['city', true], ['district', false],
        ['locality', true], ['ward', false], ['postalCode', false],
      ].map(([key, required]) => <label className="grid gap-2" key={key}><span className="text-sm font-bold capitalize">{key === 'postalCode' ? 'Postal code' : key}{!required ? ' (optional)' : ''}</span><input required={required} className="rounded-2xl border px-4 py-3" value={form[key]} onChange={event => setForm({ ...form, [key]: event.target.value })} /></label>)}</div><label className="grid gap-2"><span className="text-sm font-bold">Formatted address (optional)</span><textarea rows="2" maxLength="1000" className="rounded-2xl border px-4 py-3" value={form.formattedAddress} onChange={event => setForm({ ...form, formattedAddress: event.target.value })} /></label></section>
      <label className="grid gap-2"><span className="text-sm font-bold">Category</span><select className="rounded-2xl border px-4 py-3" value={form.category} onChange={event => { const category = event.target.value; setForm({ ...form, category }); media.filter(item => item.kind === 'IMAGE').forEach(item => { setMedia(current => current.map(currentItem => currentItem.id === item.id ? { ...currentItem, validationStatus: 'CHECKING', validationSummary: 'Re-checking image against the updated category.' } : currentItem)); validateImage(item.id, item.file, category) }) }}>{['POTHOLE','WATER_LEAKAGE','STREETLIGHT_DAMAGE','WASTE_MANAGEMENT','DRAINAGE_ISSUE'].map(category => <option key={category} value={category}>{category.replaceAll('_', ' ')}</option>)}</select></label>

      <section className="rounded-3xl border border-slate-200 bg-slate-50 p-5">
        <div className="flex flex-wrap items-start justify-between gap-3"><div><h3 className="font-black">Photo and video evidence</h3><p className="mt-1 text-xs text-slate-500">Up to 5 images and one short video. Media is optional.</p></div><span className="badge bg-white text-slate-600 ring-1 ring-slate-200">{imageCount} images / {videoCount} video</span></div>
        <div className="mt-3 rounded-2xl bg-white p-4 text-xs leading-5 text-slate-600">Images are checked before submission with Google Cloud Vision. Photos that clearly mismatch the selected category must be removed or reclassified before the report can be submitted.</div>
        {blockedImageEvidence.length ? <div className="mt-3 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-semibold text-red-700">{blockedImageMessage}</div> : null}
        <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <button type="button" onClick={() => setCameraMode('PHOTO')} className="rounded-2xl bg-civic-600 p-4 text-white transition hover:bg-civic-700"><b className="block text-sm">Take photo now</b><span className="text-xs text-civic-100">Open live camera</span></button>
          <button type="button" onClick={() => setCameraMode('VIDEO')} className="rounded-2xl bg-orange-600 p-4 text-white transition hover:bg-orange-700"><b className="block text-sm">Record video now</b><span className="text-xs text-orange-100">Up to 60 seconds</span></button>
          <label className="cursor-pointer rounded-2xl border-2 border-dashed border-civic-200 bg-white p-4 text-center transition hover:border-civic-500"><b className="block text-sm text-civic-700">Choose photos</b><span className="text-xs text-slate-500">From gallery or files</span><input type="file" multiple accept="image/jpeg,image/png,image/webp" className="sr-only" onChange={addImages} /></label>
          <label className="cursor-pointer rounded-2xl border-2 border-dashed border-orange-200 bg-white p-4 text-center transition hover:border-orange-500"><b className="block text-sm text-orange-700">Choose video</b><span className="text-xs text-slate-500">From gallery or files</span><input type="file" accept="video/mp4,video/webm" className="sr-only" onChange={addVideo} /></label>
        </div>
        {media.length ? <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">{media.map((item, index) => <MediaPreview key={item.previewUrl} item={item} onRemove={() => removeMedia(index)} />)}</div> : null}
      </section>

      <button className="btn-primary disabled:cursor-not-allowed disabled:opacity-60" type="submit" disabled={busy}>{checking ? 'Checking nearby reports...' : validatingImages ? 'Validating image evidence...' : submitting ? 'Submitting report...' : 'Check and submit report'}</button>
    </form>

    {duplicates.length ? <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/60 p-4 backdrop-blur-sm" role="dialog" aria-modal="true"><div className="max-h-[85vh] w-full max-w-2xl overflow-y-auto rounded-3xl bg-white p-6 shadow-2xl md:p-8"><p className={`text-xs font-black uppercase tracking-[0.2em] ${resolvedDuplicate && !activeDuplicateCount ? 'text-emerald-700' : 'text-orange-600'}`}>{resolvedDuplicate && !activeDuplicateCount ? 'Already resolved at this location' : 'Nearby match detected'}</p><h3 className="mt-2 text-2xl font-black">{duplicateModalTitle}</h3><p className="mt-2 text-sm text-slate-600">{duplicateModalDescription}</p>{blockedImageEvidence.length ? <div className="mt-4 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-semibold text-red-700">{blockedImageMessage}</div> : null}<div className="mt-5 grid gap-3">{duplicates.map(issue => <div key={issue.id} className={`flex flex-wrap items-center justify-between gap-3 rounded-2xl p-4 ring-1 ${issue.status === 'RESOLVED' ? 'bg-emerald-50 ring-emerald-100' : 'bg-orange-50 ring-orange-100'}`}><div><h4 className="font-bold">{issue.title}</h4><p className="mt-1 text-xs text-slate-500">{Math.round(issue.distanceMeters)} m away / {issue.status}</p></div><Link className="btn-secondary py-2" to={`/issues/${issue.id}`}>View existing issue</Link></div>)}</div><div className="mt-6 flex flex-wrap justify-end gap-3"><button className="btn-secondary" type="button" onClick={() => setDuplicates([])}>Edit my report</button><button className="btn-primary disabled:opacity-60" disabled={validatingImages || blockedImageEvidence.length > 0} type="button" onClick={createIssue}>{validatingImages ? 'Validating images...' : blockedImageEvidence.length ? 'Fix image evidence first' : resolvedDuplicate && !activeDuplicateCount ? 'Report as returned issue' : 'Continue submitting anyway'}</button></div></div></div> : null}
    {cameraMode ? <CameraCapture mode={cameraMode} onCapture={addCapturedMedia} onClose={() => setCameraMode(null)} /> : null}
  </>
}
