import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import StudentDashboard from './pages/StudentDashboard.jsx'
import FacultyDashboard from './pages/FacultyDashboard.jsx'
import StudentProfile from './pages/StudentProfile.jsx'
import FacultyProfile from './pages/FacultyProfile.jsx'
import StudentTeam from './pages/StudentTeam.jsx'
import StudentProjectProposal from './pages/StudentProjectProposal.jsx'
import FacultyProjectProposals from './pages/FacultyProjectProposals.jsx'
import ProtectedRoute from './routes/ProtectedRoute.jsx'
import { useAuth } from './context/AuthContext.jsx'

function RootRedirect() {
  const { isAuthenticated, user } = useAuth()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (user.role === 'FACULTY') {
    return <Navigate to="/faculty/dashboard" replace />
  }

  return <Navigate to="/student/dashboard" replace />
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<RootRedirect />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route
          path="/student/dashboard"
          element={
            <ProtectedRoute allowedRoles={['STUDENT']}>
              <StudentDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/student/profile"
          element={
            <ProtectedRoute allowedRoles={['STUDENT']}>
              <StudentProfile />
            </ProtectedRoute>
          }
        />
        <Route
          path="/student/team"
          element={
            <ProtectedRoute allowedRoles={['STUDENT']}>
              <StudentTeam />
            </ProtectedRoute>
          }
        />
        <Route
          path="/student/proposal"
          element={
            <ProtectedRoute allowedRoles={['STUDENT']}>
              <StudentProjectProposal />
            </ProtectedRoute>
          }
        />
        <Route
          path="/faculty/dashboard"
          element={
            <ProtectedRoute allowedRoles={['FACULTY']}>
              <FacultyDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/faculty/profile"
          element={
            <ProtectedRoute allowedRoles={['FACULTY']}>
              <FacultyProfile />
            </ProtectedRoute>
          }
        />
        <Route
          path="/faculty/proposals"
          element={
            <ProtectedRoute allowedRoles={['FACULTY']}>
              <FacultyProjectProposals />
            </ProtectedRoute>
          }
        />
      </Routes>
    </BrowserRouter>
  )
}

export default App