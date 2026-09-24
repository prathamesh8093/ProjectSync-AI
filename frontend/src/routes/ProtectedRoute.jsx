import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

function ProtectedRoute({ children, allowedRoles, allowDevAccess = false }) {
  const { isAuthenticated, user, isDevMode } = useAuth()

  // LOCAL-DEVELOPMENT-ONLY: Allow direct access to dev-flagged routes (e.g. VCS)
  // strictly during local development (import.meta.env.DEV) without requiring Supabase login.
  // In production builds (import.meta.env.DEV === false), this bypass is disabled.
  if (!isAuthenticated && isDevMode && allowDevAccess) {
    return children
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (allowedRoles && (!user || !allowedRoles.includes(user.role))) {
    return <Navigate to="/" replace />
  }

  return children
}

export default ProtectedRoute