import { Link } from 'react-router-dom'
import {
  LayoutDashboard,
  UserRound,
  Users,
  FileText,
  ClipboardCheck,
} from 'lucide-react'
import DashboardLayout from '../layouts/DashboardLayout.jsx'
import '../styles/dashboard.css'

const facultyNavItems = [
  { label: 'Dashboard', icon: LayoutDashboard, path: '/faculty/dashboard' },
  { label: 'Faculty Profile', icon: UserRound, path: '/faculty/profile' },
  { label: 'Student Teams', icon: Users, path: null },
  { label: 'Project Proposals', icon: FileText, path: '/faculty/proposals' },
  {
    label: 'Project Monitoring',
    icon: ClipboardCheck,
    path: '/faculty/projects',
  },
]

function FacultyDashboard() {
  return (
    <DashboardLayout navItems={facultyNavItems}>
      <h1 className="content-title">Faculty Dashboard</h1>
      <p className="content-subtitle">
        Quick access to student and project oversight features.
      </p>

      <div className="card-grid">
        <div className="dashboard-card">
          <h2>Faculty Profile</h2>
          <p>Coming soon.</p>
        </div>

        <div className="dashboard-card">
          <h2>Student Teams</h2>
          <p>Coming soon.</p>
        </div>

        <Link to="/faculty/proposals" className="dashboard-card">
          <h2>Project Proposals</h2>
          <p>Review and manage submitted project proposals.</p>
        </Link>

        <Link to="/faculty/projects" className="dashboard-card">
          <h2>Project Monitoring</h2>
          <p>
            Monitor approved student projects, task progress, and project
            status.
          </p>
        </Link>
      </div>
    </DashboardLayout>
  )
}

export default FacultyDashboard