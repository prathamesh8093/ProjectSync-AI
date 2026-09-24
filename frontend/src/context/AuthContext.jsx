import { createContext, useContext, useState } from 'react'

const AuthContext = createContext(null)

const STORAGE_KEY = 'projectsync_auth'

// Fallback user profile strictly for local development when testing without Supabase
const LOCAL_DEV_USER = {
  fullName: 'Local Developer',
  email: 'dev@projectsync.local',
  role: 'STUDENT',
  isLocalDev: true,
}

function loadStoredAuth() {
  const raw = sessionStorage.getItem(STORAGE_KEY)
  if (!raw) return { token: null, user: null }

  try {
    const parsed = JSON.parse(raw)
    return {
      token: parsed.token || null,
      user: parsed.user || null,
    }
  } catch {
    return { token: null, user: null }
  }
}

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(loadStoredAuth)

  // Standard Vite dev mode check (true during 'npm run dev', false in production builds)
  const isDevMode = Boolean(import.meta.env.DEV)

  const login = ({ token, email, fullName, role }) => {
    const user = { email, fullName, role }
    setAuth({ token, user })
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify({ token, user }))
  }

  const logout = () => {
    setAuth({ token: null, user: null })
    sessionStorage.removeItem(STORAGE_KEY)
  }

  // Use real user if logged in; fall back to local dev user only in development mode
  const effectiveUser = auth.user || (isDevMode ? LOCAL_DEV_USER : null)

  const value = {
    token: auth.token,
    user: effectiveUser,
    isAuthenticated: Boolean(auth.token),
    isDevMode,
    login,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}