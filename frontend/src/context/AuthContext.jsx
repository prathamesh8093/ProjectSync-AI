import { createContext, useContext, useState } from 'react'

const AuthContext = createContext(null)

const STORAGE_KEY = 'projectsync_auth'

function loadStoredAuth() {
  const raw = localStorage.getItem(STORAGE_KEY)
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

  const login = ({ token, email, fullName, role }) => {
    const user = { email, fullName, role }
    setAuth({ token, user })
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ token, user }))
  }

  const logout = () => {
    setAuth({ token: null, user: null })
    localStorage.removeItem(STORAGE_KEY)
  }

  const value = {
    token: auth.token,
    user: auth.user,
    isAuthenticated: Boolean(auth.token),
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