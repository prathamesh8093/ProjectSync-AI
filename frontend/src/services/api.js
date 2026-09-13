import axios from 'axios'

const STORAGE_KEY = 'projectsync_auth'

const api = axios.create({
  baseURL: 'http://localhost:8081/api',
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor: attach JWT from sessionStorage if present
api.interceptors.request.use((config) => {
  const raw = sessionStorage.getItem(STORAGE_KEY)

  if (raw) {
    try {
      const parsed = JSON.parse(raw)
      if (parsed.token) {
        config.headers.Authorization = `Bearer ${parsed.token}`
      }
    } catch {
      // Ignore malformed storage data; request goes out without a token.
    }
  }

  return config
})

// Response interceptor: clear stored auth on 401 (invalid/expired token)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      sessionStorage.removeItem(STORAGE_KEY)
    }
    return Promise.reject(error)
  }
)

export default api