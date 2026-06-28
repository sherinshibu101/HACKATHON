import axios from 'axios'

let authTokenProvider = async () => null

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
})

api.interceptors.request.use(async config => {
  const token = await authTokenProvider()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export function setAuthTokenProvider(provider) {
  authTokenProvider = typeof provider === 'function' ? provider : async () => null
}

export default api
