import api from './api'

export async function validateIssueImage(file, category) {
  const data = new FormData()
  data.append('file', file)
  data.append('category', category)
  try {
    const response = await api.post('/media/validate-image', data, { timeout: 30000 })
    return {
      validationStatus: response.data.validationStatus,
      validationConfidence: response.data.validationConfidence,
      validationSummary: response.data.validationSummary,
      validationLabels: response.data.validationLabels,
      validatedAt: response.data.validatedAt,
    }
  } catch (error) {
    return fallbackValidation(error)
  }
}

function fallbackValidation(error) {
  const timedOut = error.code === 'ECONNABORTED' || error.message?.toLowerCase().includes('timeout')
  const offline = error.message === 'Network Error'
  return {
    validationStatus: 'UNAVAILABLE',
    validationConfidence: 0,
    validationSummary: timedOut
      ? 'Google Cloud Vision took too long to respond. The image will still be attached and queued for manual authority review.'
      : offline
        ? 'The backend image validation service could not be reached. Make sure Spring Boot is running on port 8080. The image will still be attached for manual review.'
        : error.response?.data?.message || error.message || 'Google Cloud Vision validation is unavailable. Manual review recommended.',
    validationLabels: '',
    validatedAt: new Date().toISOString(),
  }
}
