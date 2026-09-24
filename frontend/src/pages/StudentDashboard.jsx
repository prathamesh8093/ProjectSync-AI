import { Link } from 'react-router-dom'
import {
  LayoutDashboard,
  UserRound,
  Users,
  FileText,
  ListChecks,
  GitBranch,
} from 'lucide-react'
import DashboardLayout from '../layouts/DashboardLayout.jsx'
import '../styles/dashboard.css'

const studentNavItems = [
  { label: 'Dashboard', icon: LayoutDashboard, path: '/student/dashboard' },
  { label: 'My Profile', icon: UserRound, path: '/student/profile' },
  { label: 'My Team', icon: Users, path: '/student/team' },
  { label: 'Project Proposal', icon: FileText, path: '/student/proposal' },
  { label: 'Tasks / Projects', icon: ListChecks, path: '/student/projects' },
  { label: 'Version Control (VCS)', icon: GitBranch, path: '/student/vcs' },
]

function StudentDashboard() {
  return (
    <DashboardLayout navItems={studentNavItems}>
      <h1 className="content-title">Student Dashboard</h1>
      <p className="content-subtitle">
        Quick access to your project management features.
      </p>

      <div className="card-grid">
        <Link to="/student/profile" className="dashboard-card">
          <h2>My Profile</h2>
          <p>View and update your academic profile.</p>
        </Link>

        <Link to="/student/team" className="dashboard-card">
          <h2>My Team</h2>
          <p>View or create your project team.</p>
        </Link>

        <Link to="/student/proposal" className="dashboard-card">
          <h2>Project Proposal</h2>
          <p>View or manage your team's project proposal.</p>
        </Link>

        <Link to="/student/projects" className="dashboard-card">
          <h2>Tasks / Projects</h2>
          <p>
            Open your Agile project workspace, manage progress,
            and prepare for sprint planning.
          </p>
        </Link>

        <Link to="/student/vcs" className="dashboard-card">
          <h2>Version Control (VCS)</h2>
          <p>
            Real Git repository management, staging, commits, branches,
            diff inspection, and GitHub synchronization.
          </p>
        </Link>
      </div>
    </DashboardLayout>
  )
}

export default StudentDashboard