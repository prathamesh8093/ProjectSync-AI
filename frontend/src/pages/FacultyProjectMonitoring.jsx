import { useEffect, useState } from 'react'
import {
  LayoutDashboard,
  UserRound,
  Users,
  FileText,
  ClipboardCheck,
  RefreshCw,
} from 'lucide-react'
import DashboardLayout from '../layouts/DashboardLayout.jsx'
import Badge from '../components/ui/Badge.jsx'
import Button from '../components/ui/Button.jsx'
import { getFacultyProjects } from '../services/facultyProjectMonitoringService.js'
import '../styles/facultyProjectMonitoring.css'

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

const statusTone = {
  NOT_STARTED: 'neutral',
  ACTIVE: 'primary',
  COMPLETED: 'success',
}

function messageForError(error) {
  if (!error.response) {
    return 'Unable to connect to the server. Please try again.'
  }

  switch (error.response.status) {
    case 401:
      return 'Your session has expired. Please log in again.'
    case 403:
      return 'Only faculty can access project monitoring.'
    case 404:
      return 'Project not found.'
    default:
      return 'Unable to load project monitoring data. Please try again.'
  }
}

function FacultyProjectMonitoring() {
  const [projects, setProjects] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [refreshing, setRefreshing] = useState(false)

  const loadProjects = async (showRefreshing = false) => {
    if (showRefreshing) {
      setRefreshing(true)
    } else {
      setLoading(true)
    }

    setError('')

    try {
      const response = await getFacultyProjects()
      setProjects(response.data || [])
    } catch (requestError) {
      console.error(requestError)
      setError(messageForError(requestError))
    } finally {
      if (showRefreshing) {
        setRefreshing(false)
      } else {
        setLoading(false)
      }
    }
  }

  useEffect(() => {
    loadProjects()
  }, [])

  return (
    <DashboardLayout navItems={facultyNavItems}>
      <div className="faculty-monitoring-header">
        <div>
          <h1 className="content-title">Faculty Project Monitoring</h1>
          <p className="content-subtitle">
            Monitor approved student projects and their current progress.
          </p>
        </div>

        <Button
          variant="secondary"
          onClick={() => loadProjects(true)}
          loading={refreshing}
        >
          <RefreshCw size={16} />
          {refreshing ? 'Refreshing...' : 'Refresh'}
        </Button>
      </div>

      {loading ? (
        <div className="faculty-monitoring-card">
          <p className="faculty-monitoring-loading">
            Loading project monitoring data...
          </p>
        </div>
      ) : error ? (
        <div className="faculty-monitoring-card">
          <p className="faculty-monitoring-error">{error}</p>
        </div>
      ) : projects.length === 0 ? (
        <div className="faculty-monitoring-empty-card">
          <h2>No approved projects</h2>
          <p>
            Projects created from approved proposals will appear here for
            faculty monitoring.
          </p>
        </div>
      ) : (
        <div className="faculty-monitoring-list">
          {projects.map((project) => (
            <div
              key={project.projectId}
              className="faculty-monitoring-project-card"
            >
              <div className="faculty-monitoring-project-top">
                <div>
                  <h2>{project.projectName}</h2>
                  <p className="faculty-monitoring-team">
                    {project.teamName}
                  </p>
                </div>

                <Badge
                  tone={statusTone[project.projectStatus] || 'neutral'}
                >
                  {project.projectStatus}
                </Badge>
              </div>

              <div className="faculty-monitoring-progress-section">
                <div className="faculty-monitoring-progress-header">
                  <span>Overall Progress</span>
                  <strong>{project.overallProgress}%</strong>
                </div>

                <div className="faculty-monitoring-progress-track">
                  <div
                    className="faculty-monitoring-progress-fill"
                    style={{
                      width: `${Math.max(
                        0,
                        Math.min(100, project.overallProgress || 0)
                      )}%`,
                    }}
                  />
                </div>
              </div>

              <div className="faculty-monitoring-stats">
                <div className="faculty-monitoring-stat">
                  <span>Total Tasks</span>
                  <strong>{project.totalTasks}</strong>
                </div>

                <div className="faculty-monitoring-stat">
                  <span>Completed</span>
                  <strong>{project.completedTasks}</strong>
                </div>

                <div className="faculty-monitoring-stat">
                  <span>In Progress</span>
                  <strong>{project.inProgressTasks}</strong>
                </div>

                <div className="faculty-monitoring-stat">
                  <span>In Review</span>
                  <strong>{project.inReviewTasks}</strong>
                </div>

                <div className="faculty-monitoring-stat">
                  <span>To Do</span>
                  <strong>{project.todoTasks}</strong>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </DashboardLayout>
  )
}

export default FacultyProjectMonitoring