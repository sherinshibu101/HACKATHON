import { useEffect, useRef, useState } from 'react'

const preferredVideoTypes = ['video/webm;codecs=vp8,opus', 'video/webm', 'video/mp4']

export default function CameraCapture({ mode, onCapture, onClose }) {
  const videoRef = useRef(null)
  const streamRef = useRef(null)
  const recorderRef = useRef(null)
  const chunksRef = useRef([])
  const timerRef = useRef(null)
  const cancelledRef = useRef(false)
  const [ready, setReady] = useState(false)
  const [recording, setRecording] = useState(false)
  const [seconds, setSeconds] = useState(0)
  const [error, setError] = useState('')

  const stopStream = () => {
    clearInterval(timerRef.current)
    streamRef.current?.getTracks().forEach(track => track.stop())
    streamRef.current = null
  }

  useEffect(() => {
    let active = true
    if (!navigator.mediaDevices?.getUserMedia) {
      setError('Live camera access is not supported by this browser. Use the gallery option instead.')
      return () => { active = false }
    }
    navigator.mediaDevices.getUserMedia({
      video: { facingMode: { ideal: 'environment' } },
      audio: mode === 'VIDEO',
    }).then(stream => {
      if (!active) {
        stream.getTracks().forEach(track => track.stop())
        return
      }
      streamRef.current = stream
      videoRef.current.srcObject = stream
      setReady(true)
    }).catch(err => setError(err.name === 'NotAllowedError'
      ? 'Camera permission was denied. Allow camera access in your browser settings.'
      : 'The camera could not be opened on this device.'))
    return () => {
      active = false
      stopStream()
    }
  }, [mode])

  const finish = () => {
    stopStream()
    onClose()
  }

  const takePhoto = () => {
    const video = videoRef.current
    const canvas = document.createElement('canvas')
    canvas.width = video.videoWidth
    canvas.height = video.videoHeight
    canvas.getContext('2d').drawImage(video, 0, 0)
    canvas.toBlob(blob => {
      if (!blob) {
        setError('The photo could not be captured. Please try again.')
        return
      }
      onCapture(new File([blob], `civic-photo-${Date.now()}.jpg`, { type: 'image/jpeg' }), 'IMAGE')
      finish()
    }, 'image/jpeg', 0.9)
  }

  const startRecording = () => {
    if (typeof MediaRecorder === 'undefined') {
      setError('Video recording is not supported by this browser. You can select a video from your gallery instead.')
      return
    }
    const mimeType = preferredVideoTypes.find(type => MediaRecorder.isTypeSupported(type))
    if (!mimeType) {
      setError('Video recording is not supported by this browser. You can select a video from your gallery instead.')
      return
    }
    chunksRef.current = []
    cancelledRef.current = false
    const recorder = new MediaRecorder(streamRef.current, { mimeType })
    recorderRef.current = recorder
    recorder.ondataavailable = event => { if (event.data.size) chunksRef.current.push(event.data) }
    recorder.onstop = () => {
      if (!cancelledRef.current) {
        const contentType = mimeType.split(';')[0]
        const extension = contentType === 'video/mp4' ? 'mp4' : 'webm'
        const blob = new Blob(chunksRef.current, { type: contentType })
        onCapture(new File([blob], `civic-video-${Date.now()}.${extension}`, { type: contentType }), 'VIDEO')
      }
      finish()
    }
    recorder.start(1000)
    setRecording(true)
    setSeconds(0)
    timerRef.current = setInterval(() => {
      setSeconds(current => {
        if (current >= 59 && recorder.state === 'recording') recorder.stop()
        return current + 1
      })
    }, 1000)
  }

  const stopRecording = () => {
    if (recorderRef.current?.state === 'recording') recorderRef.current.stop()
  }

  const cancel = () => {
    cancelledRef.current = true
    if (recorderRef.current?.state === 'recording') recorderRef.current.stop()
    else finish()
  }

  return <div className="fixed inset-0 z-[60] grid place-items-center bg-slate-950/80 p-4 backdrop-blur" role="dialog" aria-modal="true">
    <div className="w-full max-w-3xl overflow-hidden rounded-3xl bg-slate-950 text-white shadow-2xl">
      <div className="flex items-center justify-between p-4"><div><p className="text-xs font-black uppercase tracking-[0.2em] text-emerald-300">Live evidence</p><h3 className="text-xl font-black">{mode === 'PHOTO' ? 'Take a photo' : 'Record a short video'}</h3></div><button type="button" onClick={cancel} className="rounded-full bg-white/10 px-4 py-2 text-sm font-bold">Cancel</button></div>
      <div className="relative bg-black"><video ref={videoRef} autoPlay playsInline muted className="aspect-video max-h-[65vh] w-full object-contain" />{!ready && !error ? <div className="absolute inset-0 grid place-items-center text-sm text-slate-300">Starting camera...</div> : null}{recording ? <div className="absolute left-4 top-4 rounded-full bg-red-600 px-3 py-1 text-xs font-black">REC {seconds}s / 60s</div> : null}</div>
      {error ? <div className="m-4 rounded-2xl bg-red-500/20 p-4 text-sm text-red-200">{error}</div> : null}
      <div className="flex justify-center p-5">{mode === 'PHOTO'
        ? <button type="button" disabled={!ready} onClick={takePhoto} className="btn bg-white text-slate-950 disabled:opacity-40">Capture photo</button>
        : recording
          ? <button type="button" onClick={stopRecording} className="btn bg-red-600 text-white">Stop and use video</button>
          : <button type="button" disabled={!ready} onClick={startRecording} className="btn bg-white text-slate-950 disabled:opacity-40">Start recording</button>}
      </div>
    </div>
  </div>
}
