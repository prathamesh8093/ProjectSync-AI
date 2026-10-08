import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  LayoutDashboard,
  UserRound,
  Users,
  FileText,
  ClipboardCheck,
  RefreshCw,
  CalendarDays,
  ListChecks,
  UserCheck,
  ArrowLeft,
  ArrowRight,
  FolderGit2,
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

const projectStatusTone = {
  NOT_STARTED: 'neutral',
  ACTIVE: 'primary',
  COMPLETED: 'success',
}

const sprintStatusTone = {
  PLANNED: 'neutral',
  ACTIVE: 'primary',
  COMPLETED: 'success',
}

const taskStatusTone = {
  TODO: 'neutral',
  IN_PROGRESS: 'primary',
  IN_REVIEW: 'warning',
  DONE: 'success',
}

const priorityTone = {
  LOW: 'neutral',
  MEDIUM: 'primary',
  HIGH: 'warning',
  CRITICAL: 'danger',
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

function formatDate(dateValue) {
  if (!dateValue) {
    return '—'
  }

  const date = new Date(`${dateValue}T00:00:00`)

  if (Number.isNaN(date.getTime())) {
    return dateValue
  }

  return date.toLocaleDateString(undefined, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

function clampProgress(value) {
  return Math.max(0, Math.min(100, Number(value) || 0))
}

function FacultyProjectMonitoring() {
  const [projects, setProjects] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [refreshing, setRefreshing] = useState(false)
  const [searchParams, setSearchParams] = useSearchParams()

  const selectedProjectId = searchParams.get('projectId')
  const selectedProject = projects.find(
    (p) =>
      String(p.projectId) === String(selectedProjectId) ||
      (p.projectCode &&
        String(p.projectCode).toLowerCase() ===
          String(selectedProjectId).toLowerCase())
  )

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
      {!selectedProject ? (
        <>
          <div className="faculty-monitoring-header">
            <div>
              <h1 className="content-title">Faculty Project Monitoring</h1>
              <p className="content-subtitle">
                Select an approved student project to view its real-time progress, sprints, and task deliverables.
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
                Loading approved student projects...
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
                Projects created from approved proposals will appear here for faculty monitoring.
              </p>
            </div>
          ) : (
            <div className="faculty-monitoring-selection-grid">
              {projects.map((project) => (
                <div
                  key={project.projectId}
                  className="faculty-monitoring-select-card"
                >
                  <div className="faculty-monitoring-select-card-header">
                    <span className="faculty-monitoring-code-badge">
                      {project.projectCode || 'PRJ-CODE'}
                    </span>
                    <Badge
                      tone={
                        projectStatusTone[project.projectStatus] || 'neutral'
                      }
                    >
                      {project.projectStatus}
                    </Badge>
                  </div>

                  <h3 className="faculty-monitoring-select-title">
                    {project.projectName}
                  </h3>
                  <p className="faculty-monitoring-select-team">
                    <Users size={15} /> Team: {project.teamName}
                  </p>

                  <div className="faculty-monitoring-select-meta">
                    <div className="faculty-monitoring-select-meta-item">
                      <span>Sprints</span>
                      <strong>{(project.sprints || []).length}</strong>
                    </div>
                    <div className="faculty-monitoring-select-meta-item">
                      <span>Total Tasks</span>
                      <strong>{project.totalTasks || 0}</strong>
                    </div>
                    <div className="faculty-monitoring-select-meta-item">
                      <span>Completed</span>
                      <strong>{project.completedTasks || 0}</strong>
                    </div>
                  </div>

                  <div className="faculty-monitoring-select-action">
                    <Button
                      variant="primary"
                      onClick={() =>
                        setSearchParams({
                          projectId: project.projectCode || project.projectId,
                        })
                      }
                    >
                      Monitor Project Progress
                      <ArrowRight size={16} />
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      ) : (
        <>
          <div className="faculty-monitoring-top-nav">
            <Button
              variant="secondary"
              onClick={() => setSearchParams({})}
            >
              <ArrowLeft size={16} />
              Back to Projects List
            </Button>

            <div className="faculty-monitoring-nav-right">
              <div className="faculty-monitoring-switcher">
                <label htmlFor="project-switcher">Switch Project:</label>
                <select
                  id="project-switcher"
                  value={selectedProject.projectCode || selectedProject.projectId}
                  onChange={(e) =>
                    setSearchParams({ projectId: e.target.value })
                  }
                >
                  {projects.map((p) => (
                    <option
                      key={p.projectId}
                      value={p.projectCode || p.projectId}
                    >
                      {p.projectCode ? `[${p.projectCode}] ` : ''}
                      {p.projectName}
                    </option>
                  ))}
                </select>
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
          </div>

          <div className="faculty-monitoring-project-card">
            <div className="faculty-monitoring-project-top">
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.35rem' }}>
                  <span className="faculty-monitoring-code-badge">
                    {selectedProject.projectCode || 'PRJ-CODE'}
                  </span>
                  <h2 style={{ margin: 0 }}>{selectedProject.projectName}</h2>
                </div>
                <p className="faculty-monitoring-team">
                  Team: {selectedProject.teamName}
                </p>
              </div>

              <Badge
                tone={
                  projectStatusTone[selectedProject.projectStatus] || 'neutral'
                }
              >
                {selectedProject.projectStatus}
              </Badge>
            </div>

            <div className="faculty-monitoring-progress-section">
              <div className="faculty-monitoring-progress-header">
                <span>Overall Progress</span>
                <strong>{selectedProject.overallProgress}%</strong>
              </div>

              <div className="faculty-monitoring-progress-track">
                <div
                  className="faculty-monitoring-progress-fill"
                  style={{
                    width: `${clampProgress(selectedProject.overallProgress)}%`,
                  }}
                />
              </div>
            </div>

            <div className="faculty-monitoring-stats">
              <div className="faculty-monitoring-stat">
                <span>Total Tasks</span>
                <strong>{selectedProject.totalTasks}</strong>
              </div>

              <div className="faculty-monitoring-stat">
                <span>Completed</span>
                <strong>{selectedProject.completedTasks}</strong>
              </div>

              <div className="faculty-monitoring-stat">
                <span>In Progress</span>
                <strong>{selectedProject.inProgressTasks}</strong>
              </div>

              <div className="faculty-monitoring-stat">
                <span>In Review</span>
                <strong>{selectedProject.inReviewTasks}</strong>
              </div>

              <div className="faculty-monitoring-stat">
                <span>To Do</span>
                <strong>{selectedProject.todoTasks}</strong>
              </div>
            </div>

            <div className="faculty-monitoring-section">
              <div className="faculty-monitoring-section-header">
                <div>
                  <h3>
                    <CalendarDays size={18} />
                    Sprint Monitoring
                  </h3>
                  <p>
                    Review sprint schedules, status, workload, and progress.
                  </p>
                </div>
              </div>

              {(selectedProject.sprints || []).length === 0 ? (
                <div className="faculty-monitoring-section-empty">
                  No sprints have been created for this project.
                </div>
              ) : (
                <div className="faculty-monitoring-sprint-list">
                  {selectedProject.sprints.map((sprint) => (
                    <div
                      key={sprint.sprintId}
                      className="faculty-monitoring-sprint-card"
                    >
                      <div className="faculty-monitoring-sprint-top">
                        <div>
                          <h4>{sprint.name}</h4>

                          {sprint.goal && (
                            <p className="faculty-monitoring-sprint-goal">
                              {sprint.goal}
                            </p>
                          )}
                        </div>

                        <Badge
                          tone={
                            sprintStatusTone[sprint.status] || 'neutral'
                          }
                        >
                          {sprint.status}
                        </Badge>
                      </div>

                      <div className="faculty-monitoring-sprint-meta">
                        <div>
                          <span>Start</span>
                          <strong>{formatDate(sprint.startDate)}</strong>
                        </div>

                        <div>
                          <span>End</span>
                          <strong>{formatDate(sprint.endDate)}</strong>
                        </div>

                        <div>
                          <span>Tasks</span>
                          <strong>{sprint.totalTasks}</strong>
                        </div>

                        <div>
                          <span>Completed</span>
                          <strong>{sprint.completedTasks}</strong>
                        </div>
                      </div>

                      <div className="faculty-monitoring-sprint-progress">
                        <div className="faculty-monitoring-progress-header">
                          <span>Sprint Progress</span>
                          <strong>{sprint.progress}%</strong>
                        </div>

                        <div className="faculty-monitoring-progress-track">
                          <div
                            className="faculty-monitoring-progress-fill"
                            style={{
                              width: `${clampProgress(sprint.progress)}%`,
                            }}
                          />
                        </div>
                      </div>

                      <div className="faculty-monitoring-sprint-stats">
                        <span>
                          To Do: <strong>{sprint.todoTasks}</strong>
                        </span>
                        <span>
                          In Progress:{' '}
                          <strong>{sprint.inProgressTasks}</strong>
                        </span>
                        <span>
                          In Review: <strong>{sprint.inReviewTasks}</strong>
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="faculty-monitoring-section">
              <div className="faculty-monitoring-section-header">
                <div>
                  <h3>
                    <ListChecks size={18} />
                    Task Monitoring
                  </h3>
                  <p>
                    Review task assignments, priorities, statuses, and
                    progress.
                  </p>
                </div>
              </div>

              {(selectedProject.tasks || []).length === 0 ? (
                <div className="faculty-monitoring-section-empty">
                  No tasks have been created for this project.
                </div>
              ) : (
                <div className="faculty-monitoring-task-list">
                  {selectedProject.tasks.map((task) => (
                    <div
                      key={task.taskId}
                      className="faculty-monitoring-task-card"
                    >
                      <div className="faculty-monitoring-task-main">
                        <div className="faculty-monitoring-task-title-row">
                          <h4>{task.title}</h4>

                          <div className="faculty-monitoring-task-badges">
                            <Badge
                              tone={
                                priorityTone[task.priority] || 'neutral'
                              }
                            >
                              {task.priority}
                            </Badge>

                            <Badge
                              tone={
                                taskStatusTone[task.status] || 'neutral'
                              }
                            >
                              {task.status}
                            </Badge>
                          </div>
                        </div>

                        {task.description && (
                          <p className="faculty-monitoring-task-description">
                            {task.description}
                          </p>
                        )}

                        <div className="faculty-monitoring-task-meta">
                          <div>
                            <ListChecks size={15} />
                            <span>
                              Sprint:{' '}
                              <strong>
                                {task.sprintName || 'Unassigned'}
                              </strong>
                            </span>
                          </div>

                          <div>
                            <UserCheck size={15} />
                            <span>
                              Assigned to:{' '}
                              <strong>
                                {task.assignedToName || 'Unassigned'}
                              </strong>
                            </span>
                          </div>
                        </div>
                      </div>

                      <div className="faculty-monitoring-task-progress">
                        <div className="faculty-monitoring-progress-header">
                          <span>Progress</span>
                          <strong>{task.progress}%</strong>
                        </div>

                        <div className="faculty-monitoring-progress-track">
                          <div
                            className="faculty-monitoring-progress-fill"
                            style={{
                              width: `${clampProgress(task.progress)}%`,
                            }}
                          />
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </>
      )}
    </DashboardLayout>
  )
}

export default FacultyProjectMonitoring