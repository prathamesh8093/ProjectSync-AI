import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import {
  createProject,
  getMyProject,
  updateProjectStatus,
} from '../services/projectService.js'
import {
  createSprint,
  getMyProjectSprints,
  updateSprintStatus,
} from '../services/sprintService.js'
import {
  createTask,
  getMyProjectTasks,
  updateTaskStatus,
} from '../services/taskService.js'
import {
  createProjectDocument,
  getMyProjectDocuments,
  updateProjectDocument,
  deleteProjectDocument,
} from '../services/projectDocumentService.js'
import {
  getMyTeam,
  getTeamMembers,
} from '../services/teamService.js'
import {
  getTaskHistory,
} from '../services/taskHistoryService.js'
import {
  getMyProjectProgress,
} from '../services/projectProgressService.js'
import {
  getMyProjectWorkload,
} from '../services/workloadService.js'
import '../styles/projectWorkspace.css'

function StudentProjectWorkspace() {
  const { user } = useAuth()

  const [project, setProject] = useState(null)
  const [sprints, setSprints] = useState([])
  const [tasks, setTasks] = useState([])
  const [documents, setDocuments] = useState([])
  const [selectedHistoryTaskId, setSelectedHistoryTaskId] = useState(null)
  const [taskHistory, setTaskHistory] = useState([])

  const [team, setTeam] = useState(null)
  const [teamMembers, setTeamMembers] = useState([])
  const [isLeader, setIsLeader] = useState(false)

  const [loading, setLoading] = useState(true)
  const [projectLoading, setProjectLoading] = useState(false)
  const [sprintLoading, setSprintLoading] = useState(false)
  const [taskLoading, setTaskLoading] = useState(false)
  const [documentLoading, setDocumentLoading] = useState(false)
  const [historyLoading, setHistoryLoading] = useState(false)
  const [progressLoading, setProgressLoading] = useState(false)
  const [workloadLoading, setWorkloadLoading] = useState(false)
  const [projectProgress, setProjectProgress] = useState(null)
  const [workload, setWorkload] = useState([])

  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const [showProjectForm, setShowProjectForm] = useState(false)
  const [showSprintForm, setShowSprintForm] = useState(false)
  const [showTaskForm, setShowTaskForm] = useState(false)
  const [showDocumentForm, setShowDocumentForm] = useState(false)

  const [editingDocumentId, setEditingDocumentId] = useState(null)

  const [projectForm, setProjectForm] = useState({
    proposalId: '',
    name: '',
    description: '',
  })

  const [sprintForm, setSprintForm] = useState({
    name: '',
    goal: '',
    startDate: '',
    endDate: '',
  })

  const [taskForm, setTaskForm] = useState({
    sprintId: '',
    assignedTo: '',
    title: '',
    description: '',
    priority: 'MEDIUM',
  })

  const [documentForm, setDocumentForm] = useState({
    title: '',
    content: '',
  })

  useEffect(() => {
    loadWorkspace()
  }, [])

  const loadWorkspace = async () => {
    setLoading(true)
    setError('')

    try {
      const teamResponse = await getMyTeam()
      const teamData = teamResponse.data

      setTeam(teamData)

      const membersResponse = await getTeamMembers(teamData.id)
      const members = membersResponse.data || []

      setTeamMembers(members)

      const currentMember = members.find(
        (member) => member.email === user?.email
      )

      setIsLeader(currentMember?.teamRole === 'LEADER')

      try {
        const projectResponse = await getMyProject()
        const projectData = projectResponse.data

        setProject(projectData)

        setProjectForm((previous) => ({
          ...previous,
          proposalId: projectData.proposalId || '',
          name: projectData.name || '',
          description: projectData.description || '',
        }))

        try {
          const sprintResponse = await getMyProjectSprints()
          setSprints(sprintResponse.data || [])
        } catch (sprintError) {
          if (sprintError.response?.status !== 404) {
            throw sprintError
          }

          setSprints([])
        }

        try {
          const taskResponse = await getMyProjectTasks()
          setTasks(taskResponse.data || [])
        } catch (taskError) {
          if (taskError.response?.status !== 404) {
            throw taskError
          }

          setTasks([])
        }

        try {
          const documentResponse = await getMyProjectDocuments()
          setDocuments(documentResponse.data || [])
        } catch (documentError) {
          if (documentError.response?.status !== 404) {
            throw documentError
          }

          setDocuments([])
        }

        try {
          const progressResponse = await getMyProjectProgress()
          setProjectProgress(progressResponse.data || null)
        } catch (progressError) {
          if (progressError.response?.status !== 404) {
            throw progressError
          }

          setProjectProgress(null)
        }

        try {
          const workloadResponse = await getMyProjectWorkload()
          setWorkload(workloadResponse.data || [])
        } catch (workloadError) {
          if (workloadError.response?.status !== 404) {
            throw workloadError
          }

          setWorkload([])
        }
      } catch (projectError) {
        if (projectError.response?.status === 404) {
          setProject(null)
          setSprints([])
          setTasks([])
          setDocuments([])
          setProjectProgress(null)
        } else {
          throw projectError
        }
      }
    } catch (requestError) {
      console.error(requestError)

      if (requestError.response?.status === 403) {
        setError(
          'You do not have permission to access the Agile project workspace.'
        )
      } else if (requestError.response?.status === 404) {
        setError('You are not part of a team yet.')
      } else {
        setError(
          requestError.response?.data ||
            'Unable to load the Agile project workspace.'
        )
      }
    } finally {
      setLoading(false)
    }
  }

  const handleProjectFormChange = (event) => {
    const { name, value } = event.target

    setProjectForm((previous) => ({
      ...previous,
      [name]: value,
    }))
  }

  const handleSprintFormChange = (event) => {
    const { name, value } = event.target

    setSprintForm((previous) => ({
      ...previous,
      [name]: value,
    }))
  }

  const handleTaskFormChange = (event) => {
    const { name, value } = event.target

    setTaskForm((previous) => ({
      ...previous,
      [name]: value,
    }))
  }

  const handleDocumentFormChange = (event) => {
    const { name, value } = event.target

    setDocumentForm((previous) => ({
      ...previous,
      [name]: value,
    }))
  }

  const handleCreateProject = async (event) => {
    event.preventDefault()

    if (!isLeader) {
      setError('Only the team leader can create the Agile project.')
      return
    }

    setProjectLoading(true)
    setError('')
    setSuccess('')

    try {
      const response = await createProject({
        proposalId: projectForm.proposalId,
        name: projectForm.name,
        description: projectForm.description,
      })

      setProject(response.data)

      setShowProjectForm(false)

      setSuccess('Agile project created successfully.')

      setProjectForm({
        proposalId: response.data.proposalId || '',
        name: response.data.name || '',
        description: response.data.description || '',
      })

      setSprints([])
      setTasks([])
      setDocuments([])
      setProjectProgress(null)
      setWorkload([])
    } catch (requestError) {
      console.error(requestError)

      setError(
        requestError.response?.data ||
          'Unable to create the Agile project.'
      )
    } finally {
      setProjectLoading(false)
    }
  }

  const handleProjectStatusChange = async (status) => {
    if (!project || !isLeader) {
      return
    }

    setProjectLoading(true)
    setError('')
    setSuccess('')

    try {
      const response = await updateProjectStatus(
        project.id,
        status
      )

      setProject(response.data)

      setSuccess(`Project status changed to ${status}.`)
    } catch (requestError) {
      console.error(requestError)

      setError(
        requestError.response?.data ||
          'Unable to update the project status.'
      )
    } finally {
      setProjectLoading(false)
    }
  }

  const handleCreateSprint = async (event) => {
    event.preventDefault()

    if (!project) {
      setError('Create an Agile project before creating a sprint.')
      return
    }

    if (!isLeader) {
      setError('Only the team leader can create a sprint.')
      return
    }

    setSprintLoading(true)
    setError('')
    setSuccess('')

    try {
      const response = await createSprint({
        projectId: project.id,
        name: sprintForm.name,
        goal: sprintForm.goal,
        startDate: sprintForm.startDate,
        endDate: sprintForm.endDate,
      })

      setSprints((previous) =>
        [
          ...previous,
          response.data,
        ].sort(
          (first, second) =>
            first.startDate.localeCompare(second.startDate)
        )
      )

      setSprintForm({
        name: '',
        goal: '',
        startDate: '',
        endDate: '',
      })

      setShowSprintForm(false)

      setSuccess('Sprint created successfully.')
    } catch (requestError) {
      console.error(requestError)

      setError(
        requestError.response?.data ||
          'Unable to create the sprint.'
      )
    } finally {
      setSprintLoading(false)
    }
  }

  const handleSprintStatusChange = async (
    sprintId,
    status
  ) => {
    if (!isLeader) {
      setError('Only the team leader can update sprint status.')
      return
    }

    setSprintLoading(true)
    setError('')
    setSuccess('')

    try {
      const response = await updateSprintStatus(
        sprintId,
        status
      )

      setSprints((previous) =>
        previous.map((sprint) =>
          sprint.id === sprintId
            ? response.data
            : sprint
        )
      )

      setSuccess(`Sprint status changed to ${status}.`)
    } catch (requestError) {
      console.error(requestError)

      setError(
        requestError.response?.data ||
          'Unable to update the sprint status.'
      )
    } finally {
      setSprintLoading(false)
    }
  }

  const handleCreateTask = async (event) => {
    event.preventDefault()

    if (!project) {
      setError('Create an Agile project before creating tasks.')
      return
    }

    if (!isLeader) {
      setError('Only the team leader can create a task.')
      return
    }

    setTaskLoading(true)
    setError('')
    setSuccess('')

    try {
      const response = await createTask({
        projectId: project.id,
        sprintId: taskForm.sprintId || null,
        assignedTo: taskForm.assignedTo || null,
        title: taskForm.title,
        description: taskForm.description,
        priority: taskForm.priority,
        status: 'TODO',
        progress: 0,
      })

      setTasks((previous) => [
        ...previous,
        response.data,
      ])

      setTaskForm({
        sprintId: '',
        assignedTo: '',
        title: '',
        description: '',
        priority: 'MEDIUM',
      })

      setShowTaskForm(false)

      setSuccess('Task created successfully.')
    } catch (requestError) {
      console.error(requestError)

      setError(
        requestError.response?.data ||
          'Unable to create the task.'
      )
    } finally {
      setTaskLoading(false)
    }
  }

  const handleTaskStatusChange = async (
    taskId,
    status
  ) => {
    setTaskLoading(true)
    setError('')
    setSuccess('')

    try {
      const response = await updateTaskStatus(
        taskId,
        status
      )

      setTasks((previous) =>
        previous.map((task) =>
          task.id === taskId
            ? response.data
            : task
        )
      )

      setSuccess(`Task status changed to ${status}.`)

      try {
        const progressResponse = await getMyProjectProgress()
        setProjectProgress(progressResponse.data || null)
      } catch (progressError) {
        console.error(progressError)
      }

      try {
        const workloadResponse = await getMyProjectWorkload()
        setWorkload(workloadResponse.data || [])
      } catch (workloadError) {
        console.error(workloadError)
      }
    } catch (requestError) {
      console.error(requestError)

      setError(
        requestError.response?.data ||
          'Unable to update the task status.'
      )
    } finally {
      setTaskLoading(false)
    }
  }

  const handleViewTaskHistory = async (task) => {
    if (!task?.id) {
      return
    }

    if (selectedHistoryTaskId === task.id) {
      setSelectedHistoryTaskId(null)
      setTaskHistory([])
      return
    }

    setHistoryLoading(true)
    setError('')
    setSuccess('')

    try {
      const response = await getTaskHistory(task.id)

      setSelectedHistoryTaskId(task.id)
      setTaskHistory(response.data || [])
    } catch (requestError) {
      console.error(requestError)

      setError(
        requestError.response?.data ||
          'Unable to load task history.'
      )
    } finally {
      setHistoryLoading(false)
    }
  }

  const resetDocumentForm = () => {
    setDocumentForm({
      title: '',
      content: '',
    })

    setEditingDocumentId(null)
    setShowDocumentForm(false)
  }

  const handleCreateDocument = async (event) => {
    event.preventDefault()

    if (!project) {
      setError(
        'Create an Agile project before creating documents.'
      )
      return
    }

    setDocumentLoading(true)
    setError('')
    setSuccess('')

    try {
      const response = await createProjectDocument({
        projectId: project.id,
        title: documentForm.title,
        content: documentForm.content,
      })

      setDocuments((previous) => [
        response.data,
        ...previous,
      ])

      resetDocumentForm()

      setSuccess('Project document created successfully.')
    } catch (requestError) {
      console.error(requestError)

      setError(
        requestError.response?.data ||
          'Unable to create the project document.'
      )
    } finally {
      setDocumentLoading(false)
    }
  }

  const handleStartDocumentEdit = (document) => {
    setEditingDocumentId(document.id)

    setDocumentForm({
      title: document.title || '',
      content: document.content || '',
    })

    setShowDocumentForm(true)
    setError('')
    setSuccess('')
  }

  const handleUpdateDocument = async (event) => {
    event.preventDefault()

    if (!project || !editingDocumentId) {
      return
    }

    setDocumentLoading(true)
    setError('')
    setSuccess('')

    try {
      const response = await updateProjectDocument(
        editingDocumentId,
        {
          projectId: project.id,
          title: documentForm.title,
          content: documentForm.content,
        }
      )

      setDocuments((previous) =>
        previous
          .map((document) =>
            document.id === editingDocumentId
              ? response.data
              : document
          )
          .sort((first, second) =>
            (second.updatedAt || '').localeCompare(
              first.updatedAt || ''
            )
          )
      )

      resetDocumentForm()

      setSuccess('Project document updated successfully.')
    } catch (requestError) {
      console.error(requestError)

      setError(
        requestError.response?.data ||
          'Unable to update the project document.'
      )
    } finally {
      setDocumentLoading(false)
    }
  }

  const handleDeleteDocument = async (documentId) => {
    const confirmed = window.confirm(
      'Are you sure you want to delete this project document?'
    )

    if (!confirmed) {
      return
    }

    setDocumentLoading(true)
    setError('')
    setSuccess('')

    try {
      await deleteProjectDocument(documentId)

      setDocuments((previous) =>
        previous.filter(
          (document) => document.id !== documentId
        )
      )

      if (editingDocumentId === documentId) {
        resetDocumentForm()
      }

      setSuccess('Project document deleted successfully.')
    } catch (requestError) {
      console.error(requestError)

      setError(
        requestError.response?.data ||
          'Unable to delete the project document.'
      )
    } finally {
      setDocumentLoading(false)
    }
  }

  const handleRefreshProjectProgress = async () => {
    if (!project) {
      return
    }

    setProgressLoading(true)
    setError('')
    setSuccess('')

    try {
      const response = await getMyProjectProgress()
      setProjectProgress(response.data || null)
      setSuccess('Project progress refreshed successfully.')
    } catch (requestError) {
      console.error(requestError)

      setError(
        requestError.response?.data ||
          'Unable to load project progress.'
      )
    } finally {
      setProgressLoading(false)
    }
  }

  const handleRefreshWorkload = async () => {
    if (!project) {
      return
    }

    setWorkloadLoading(true)
    setError('')
    setSuccess('')

    try {
      const response = await getMyProjectWorkload()
      setWorkload(response.data || [])
      setSuccess('Workload refreshed successfully.')
    } catch (requestError) {
      console.error(requestError)

      setError(
        requestError.response?.data ||
          'Unable to load project workload.'
      )
    } finally {
      setWorkloadLoading(false)
    }
  }

  const taskColumns = useMemo(
    () => [
      {
        status: 'TODO',
        title: 'To Do',
      },
      {
        status: 'IN_PROGRESS',
        title: 'In Progress',
      },
      {
        status: 'IN_REVIEW',
        title: 'In Review',
      },
      {
        status: 'DONE',
        title: 'Done',
      },
    ],
    []
  )

  const getTasksForStatus = (status) =>
    tasks.filter((task) => task.status === status)

  const getMemberName = (email) => {
    const member = teamMembers.find(
      (item) => item.email === email
    )

    return member?.fullName || email
  }

  const formatDate = (value) => {
    if (!value) {
      return '—'
    }

    return new Date(`${value}T00:00:00`).toLocaleDateString()
  }

  const formatDateTime = (value) => {
    if (!value) {
      return '—'
    }

    return new Date(value).toLocaleString()
  }

  if (loading) {
    return (
      <div className="project-workspace-page">
        <div className="project-workspace-container">
          <div className="workspace-loading">
            Loading Agile workspace...
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="project-workspace-page">
      <div className="project-workspace-container">

        <div className="workspace-header">
          <div>
            <p className="workspace-eyebrow">
              Agile Project Management
            </p>

            <h1>Project Workspace</h1>

            <p className="workspace-subtitle">
              Manage your approved project, sprints, tasks,
              and collaborative project documents.
            </p>
          </div>

          <Link
            to="/student/dashboard"
            className="workspace-back-link"
          >
            Back to Dashboard
          </Link>
        </div>

        {team && (
          <div className="workspace-team-card">
            <div>
              <span className="workspace-label">
                Team
              </span>

              <strong>
                {team.name || 'Your Team'}
              </strong>
            </div>

            <div>
              <span className="workspace-label">
                Your Role
              </span>

              <strong>
                {isLeader ? 'LEADER' : 'MEMBER'}
              </strong>
            </div>
          </div>
        )}

        {error && (
          <div className="workspace-alert workspace-alert-error">
            {typeof error === 'string'
              ? error
              : 'Something went wrong.'}
          </div>
        )}

        {success && (
          <div className="workspace-alert workspace-alert-success">
            {success}
          </div>
        )}

        {!project && (
          <section className="workspace-card">
            <div className="workspace-card-header">
              <div>
                <h2>Agile Project</h2>

                <p>
                  Your team has not created an Agile project
                  yet.
                </p>
              </div>

              {isLeader && (
                <button
                  type="button"
                  className="workspace-primary-button"
                  onClick={() => {
                    setShowProjectForm(
                      (previous) => !previous
                    )
                    setError('')
                    setSuccess('')
                  }}
                >
                  {showProjectForm
                    ? 'Cancel'
                    : 'Create Project'}
                </button>
              )}
            </div>

            {!isLeader && (
              <div className="workspace-info-box">
                Only the team leader can create the Agile
                project. Once it is created, you can view
                and work with the project here.
              </div>
            )}

            {showProjectForm && isLeader && (
              <form
                className="workspace-form"
                onSubmit={handleCreateProject}
              >
                <div className="workspace-form-group">
                  <label htmlFor="proposalId">
                    Approved Proposal ID
                  </label>

                  <input
                    id="proposalId"
                    name="proposalId"
                    type="text"
                    value={projectForm.proposalId}
                    onChange={handleProjectFormChange}
                    placeholder="Enter approved proposal ID"
                    required
                  />
                </div>

                <div className="workspace-form-group">
                  <label htmlFor="projectName">
                    Project Name
                  </label>

                  <input
                    id="projectName"
                    name="name"
                    type="text"
                    value={projectForm.name}
                    onChange={handleProjectFormChange}
                    placeholder="Enter project name"
                    maxLength={255}
                    required
                  />
                </div>

                <div className="workspace-form-group">
                  <label htmlFor="projectDescription">
                    Description
                  </label>

                  <textarea
                    id="projectDescription"
                    name="description"
                    value={projectForm.description}
                    onChange={handleProjectFormChange}
                    placeholder="Describe the Agile project"
                    rows="5"
                  />
                </div>

                <button
                  type="submit"
                  className="workspace-primary-button"
                  disabled={projectLoading}
                >
                  {projectLoading
                    ? 'Creating...'
                    : 'Create Agile Project'}
                </button>
              </form>
            )}
          </section>
        )}

        {project && (
          <>
            <section className="workspace-card">
              <div className="workspace-card-header">
                <div>
                  <span className="workspace-label">
                    Agile Project
                  </span>

                  <h2>{project.name}</h2>
                </div>

                <span
                  className={`workspace-status workspace-status-${project.status?.toLowerCase()}`}
                >
                  {project.status}
                </span>
              </div>

              {project.description && (
                <p className="workspace-description">
                  {project.description}
                </p>
              )}

              <div className="workspace-details-grid">
                <div>
                  <span className="workspace-label">
                    Project ID
                  </span>

                  <span className="workspace-value">
                    {project.id}
                  </span>
                </div>

                <div>
                  <span className="workspace-label">
                    Proposal ID
                  </span>

                  <span className="workspace-value">
                    {project.proposalId}
                  </span>
                </div>

                <div>
                  <span className="workspace-label">
                    Created
                  </span>

                  <span className="workspace-value">
                    {project.createdAt
                      ? new Date(
                          project.createdAt
                        ).toLocaleString()
                      : '—'}
                  </span>
                </div>

                <div>
                  <span className="workspace-label">
                    Last Updated
                  </span>

                  <span className="workspace-value">
                    {project.updatedAt
                      ? new Date(
                          project.updatedAt
                        ).toLocaleString()
                      : '—'}
                  </span>
                </div>
              </div>

              {isLeader && (
                <div className="workspace-actions">
                  <span className="workspace-label">
                    Project Status
                  </span>

                  <div className="workspace-button-row">
                    {project.status !== 'ACTIVE' && (
                      <button
                        type="button"
                        className="workspace-secondary-button"
                        disabled={projectLoading}
                        onClick={() =>
                          handleProjectStatusChange(
                            'ACTIVE'
                          )
                        }
                      >
                        Set Active
                      </button>
                    )}

                    {project.status !== 'COMPLETED' && (
                      <button
                        type="button"
                        className="workspace-secondary-button"
                        disabled={projectLoading}
                        onClick={() =>
                          handleProjectStatusChange(
                            'COMPLETED'
                          )
                        }
                      >
                        Mark Completed
                      </button>
                    )}
                  </div>
                </div>
              )}
            </section>

            <section className="workspace-card">
              <div className="workspace-card-header">
                <div>
                  <span className="workspace-label">
                    Agile Planning
                  </span>

                  <h2>Sprints</h2>

                  <p>
                    Organize project work into focused
                    development cycles.
                  </p>
                </div>

                {isLeader && (
                  <button
                    type="button"
                    className="workspace-primary-button"
                    onClick={() => {
                      setShowSprintForm(
                        (previous) => !previous
                      )
                      setError('')
                      setSuccess('')
                    }}
                  >
                    {showSprintForm
                      ? 'Cancel'
                      : 'Create Sprint'}
                  </button>
                )}
              </div>

              {showSprintForm && isLeader && (
                <form
                  className="workspace-form"
                  onSubmit={handleCreateSprint}
                >
                  <div className="workspace-form-group">
                    <label htmlFor="sprintName">
                      Sprint Name
                    </label>

                    <input
                      id="sprintName"
                      name="name"
                      type="text"
                      value={sprintForm.name}
                      onChange={handleSprintFormChange}
                      placeholder="Example: Sprint 1"
                      maxLength={255}
                      required
                    />
                  </div>

                  <div className="workspace-form-group">
                    <label htmlFor="sprintGoal">
                      Sprint Goal
                    </label>

                    <textarea
                      id="sprintGoal"
                      name="goal"
                      value={sprintForm.goal}
                      onChange={handleSprintFormChange}
                      placeholder="What should this sprint accomplish?"
                      rows="4"
                    />
                  </div>

                  <div className="workspace-date-grid">
                    <div className="workspace-form-group">
                      <label htmlFor="sprintStartDate">
                        Start Date
                      </label>

                      <input
                        id="sprintStartDate"
                        name="startDate"
                        type="date"
                        value={sprintForm.startDate}
                        onChange={handleSprintFormChange}
                        required
                      />
                    </div>

                    <div className="workspace-form-group">
                      <label htmlFor="sprintEndDate">
                        End Date
                      </label>

                      <input
                        id="sprintEndDate"
                        name="endDate"
                        type="date"
                        value={sprintForm.endDate}
                        onChange={handleSprintFormChange}
                        required
                      />
                    </div>
                  </div>

                  <button
                    type="submit"
                    className="workspace-primary-button"
                    disabled={sprintLoading}
                  >
                    {sprintLoading
                      ? 'Creating...'
                      : 'Create Sprint'}
                  </button>
                </form>
              )}

              {sprints.length === 0 ? (
                <div className="workspace-empty-state">
                  <h3>No sprints yet</h3>

                  <p>
                    {isLeader
                      ? 'Create the first sprint to start planning project work.'
                      : 'Your team has not created any sprints yet.'}
                  </p>
                </div>
              ) : (
                <div className="sprint-list">
                  {sprints.map((sprint) => (
                    <article
                      key={sprint.id}
                      className="sprint-card"
                    >
                      <div className="sprint-card-header">
                        <div>
                          <h3>{sprint.name}</h3>

                          <span
                            className={`workspace-status workspace-status-${sprint.status?.toLowerCase()}`}
                          >
                            {sprint.status}
                          </span>
                        </div>
                      </div>

                      {sprint.goal && (
                        <p className="sprint-goal">
                          {sprint.goal}
                        </p>
                      )}

                      <div className="sprint-details">
                        <div>
                          <span className="workspace-label">
                            Start
                          </span>

                          <span className="workspace-value">
                            {formatDate(
                              sprint.startDate
                            )}
                          </span>
                        </div>

                        <div>
                          <span className="workspace-label">
                            End
                          </span>

                          <span className="workspace-value">
                            {formatDate(
                              sprint.endDate
                            )}
                          </span>
                        </div>
                      </div>

                      {isLeader && (
                        <div className="workspace-button-row">
                          {sprint.status ===
                            'PLANNED' && (
                            <button
                              type="button"
                              className="workspace-secondary-button"
                              disabled={
                                sprintLoading
                              }
                              onClick={() =>
                                handleSprintStatusChange(
                                  sprint.id,
                                  'ACTIVE'
                                )
                              }
                            >
                              Start Sprint
                            </button>
                          )}

                          {sprint.status ===
                            'ACTIVE' && (
                            <button
                              type="button"
                              className="workspace-secondary-button"
                              disabled={
                                sprintLoading
                              }
                              onClick={() =>
                                handleSprintStatusChange(
                                  sprint.id,
                                  'COMPLETED'
                                )
                              }
                            >
                              Complete Sprint
                            </button>
                          )}
                        </div>
                      )}
                    </article>
                  ))}
                </div>
              )}
            </section>

            <section className="workspace-card">
              <div className="workspace-card-header">
                <div>
                  <span className="workspace-label">
                    Agile Execution
                  </span>

                  <h2>Tasks</h2>

                  <p>
                    Track assignments, priorities, progress,
                    and task status across the project.
                  </p>
                </div>

                {isLeader && (
                  <button
                    type="button"
                    className="workspace-primary-button"
                    onClick={() => {
                      setShowTaskForm(
                        (previous) => !previous
                      )
                      setError('')
                      setSuccess('')
                    }}
                  >
                    {showTaskForm
                      ? 'Cancel'
                      : 'Create Task'}
                  </button>
                )}
              </div>

              {!isLeader && (
                <div className="workspace-info-box">
                  You can view all project tasks and update
                  the status or progress of tasks assigned
                  to you.
                </div>
              )}

              {showTaskForm && isLeader && (
                <form
                  className="workspace-form"
                  onSubmit={handleCreateTask}
                >
                  <div className="workspace-form-group">
                    <label htmlFor="taskTitle">
                      Task Title
                    </label>

                    <input
                      id="taskTitle"
                      name="title"
                      type="text"
                      value={taskForm.title}
                      onChange={handleTaskFormChange}
                      placeholder="Example: Implement login API"
                      maxLength={255}
                      required
                    />
                  </div>

                  <div className="workspace-form-group">
                    <label htmlFor="taskDescription">
                      Description
                    </label>

                    <textarea
                      id="taskDescription"
                      name="description"
                      value={taskForm.description}
                      onChange={handleTaskFormChange}
                      placeholder="Describe the task"
                      rows="4"
                    />
                  </div>

                  <div className="workspace-date-grid">
                    <div className="workspace-form-group">
                      <label htmlFor="taskSprint">
                        Sprint
                      </label>

                      <select
                        id="taskSprint"
                        name="sprintId"
                        value={taskForm.sprintId}
                        onChange={handleTaskFormChange}
                      >
                        <option value="">
                          No Sprint
                        </option>

                        {sprints.map((sprint) => (
                          <option
                            key={sprint.id}
                            value={sprint.id}
                          >
                            {sprint.name}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="workspace-form-group">
                      <label htmlFor="taskAssignee">
                        Assign To
                      </label>

                      <select
                        id="taskAssignee"
                        name="assignedTo"
                        value={taskForm.assignedTo}
                        onChange={handleTaskFormChange}
                      >
                        <option value="">
                          Unassigned
                        </option>

                        {teamMembers.map((member) => (
                          <option
                            key={
                              member.userId ||
                              member.id ||
                              member.email
                            }
                            value={
                              member.userId ||
                              member.studentUserId ||
                              member.id
                            }
                          >
                            {member.fullName ||
                              member.email}
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>

                  <div className="workspace-form-group">
                    <label htmlFor="taskPriority">
                      Priority
                    </label>

                    <select
                      id="taskPriority"
                      name="priority"
                      value={taskForm.priority}
                      onChange={handleTaskFormChange}
                    >
                      <option value="LOW">
                        Low
                      </option>

                      <option value="MEDIUM">
                        Medium
                      </option>

                      <option value="HIGH">
                        High
                      </option>

                      <option value="CRITICAL">
                        Critical
                      </option>
                    </select>
                  </div>

                  <button
                    type="submit"
                    className="workspace-primary-button"
                    disabled={taskLoading}
                  >
                    {taskLoading
                      ? 'Creating...'
                      : 'Create Task'}
                  </button>
                </form>
              )}

              {tasks.length === 0 ? (
                <div className="workspace-empty-state">
                  <h3>No tasks yet</h3>

                  <p>
                    {isLeader
                      ? 'Create the first task to begin project execution.'
                      : 'Your team has not created any tasks yet.'}
                  </p>
                </div>
              ) : (
                <div className="task-board">
                  {taskColumns.map((column) => {
                    const columnTasks =
                      getTasksForStatus(
                        column.status
                      )

                    return (
                      <div
                        key={column.status}
                        className="task-column"
                      >
                        <div className="task-column-header">
                          <h3>{column.title}</h3>

                          <span className="task-column-count">
                            {columnTasks.length}
                          </span>
                        </div>

                        <div className="task-column-list">
                          {columnTasks.length === 0 ? (
                            <div className="task-column-empty">
                              No tasks
                            </div>
                          ) : (
                            columnTasks.map((task) => (
                              <article
                                key={task.id}
                                className="task-card"
                              >
                                <div className="task-card-top">
                                  <h4>
                                    {task.title}
                                  </h4>

                                  <span
                                    className={`task-priority task-priority-${task.priority?.toLowerCase()}`}
                                  >
                                    {task.priority}
                                  </span>
                                </div>

                                {task.description && (
                                  <p className="task-description">
                                    {task.description}
                                  </p>
                                )}

                                <div className="task-meta">
                                  <div>
                                    <span className="workspace-label">
                                      Assignee
                                    </span>

                                    <span className="workspace-value">
                                      {task.assignedToName ||
                                        (task.assignedToEmail
                                          ? getMemberName(
                                              task.assignedToEmail
                                            )
                                          : 'Unassigned')}
                                    </span>
                                  </div>

                                  <div>
                                    <span className="workspace-label">
                                      Sprint
                                    </span>

                                    <span className="workspace-value">
                                      {task.sprintId
                                        ? sprints.find(
                                            (sprint) =>
                                              sprint.id ===
                                              task.sprintId
                                          )?.name ||
                                          'Sprint'
                                        : 'No Sprint'}
                                    </span>
                                  </div>
                                </div>

                                <div className="task-progress-section">
                                  <div className="task-progress-header">
                                    <span>
                                      Progress
                                    </span>

                                    <strong>
                                      {task.progress}%
                                    </strong>
                                  </div>

                                  <div className="task-progress-track">
                                    <div
                                      className="task-progress-fill"
                                      style={{
                                        width: `${task.progress}%`,
                                      }}
                                    />
                                  </div>
                                </div>

                                {(isLeader ||
                                  task.assignedToEmail ===
                                    user?.email) && (
                                  <div className="task-actions">
                                    <label>
                                      Status

                                      <select
                                        value={
                                          task.status
                                        }
                                        disabled={
                                          taskLoading
                                        }
                                        onChange={(
                                          event
                                        ) =>
                                          handleTaskStatusChange(
                                            task.id,
                                            event.target
                                              .value
                                          )
                                        }
                                      >
                                        <option value="TODO">
                                          To Do
                                        </option>

                                        <option value="IN_PROGRESS">
                                          In Progress
                                        </option>

                                        <option value="IN_REVIEW">
                                          In Review
                                        </option>

                                        <option value="DONE">
                                          Done
                                        </option>
                                      </select>
                                    </label>
                                  </div>
                                )}

                                <div className="workspace-button-row">
                                  <button
                                    type="button"
                                    className="workspace-secondary-button"
                                    disabled={historyLoading}
                                    onClick={() =>
                                      handleViewTaskHistory(task)
                                    }
                                  >
                                    {selectedHistoryTaskId === task.id
                                      ? 'Hide History'
                                      : 'View History'}
                                  </button>
                                </div>
                              </article>
                            ))
                          )}
                        </div>
                      </div>
                    )
                  })}
                </div>
              )}
            </section>

            <section className="workspace-card">
              <div className="workspace-card-header">
                <div>
                  <span className="workspace-label">
                    Agile Progress
                  </span>

                  <h2>Project Progress</h2>

                  <p>
                    Monitor task completion and overall project
                    progress across the Agile workspace.
                  </p>
                </div>

                <button
                  type="button"
                  className="workspace-secondary-button"
                  disabled={progressLoading}
                  onClick={handleRefreshProjectProgress}
                >
                  {progressLoading
                    ? 'Refreshing...'
                    : 'Refresh Progress'}
                </button>
              </div>

              {!projectProgress ? (
                <div className="workspace-empty-state">
                  <h3>No progress data available</h3>

                  <p>
                    Project progress will appear here once task
                    statistics are available.
                  </p>
                </div>
              ) : (
                <>
                  <div className="workspace-details-grid">
                    <div>
                      <span className="workspace-label">
                        Overall Progress
                      </span>

                      <span className="workspace-value">
                        {projectProgress.overallProgress ?? 0}%
                      </span>
                    </div>

                    <div>
                      <span className="workspace-label">
                        Total Tasks
                      </span>

                      <span className="workspace-value">
                        {projectProgress.totalTasks ?? 0}
                      </span>
                    </div>

                    <div>
                      <span className="workspace-label">
                        Completed
                      </span>

                      <span className="workspace-value">
                        {projectProgress.completedTasks ?? 0}
                      </span>
                    </div>

                    <div>
                      <span className="workspace-label">
                        In Progress
                      </span>

                      <span className="workspace-value">
                        {projectProgress.inProgressTasks ?? 0}
                      </span>
                    </div>

                    <div>
                      <span className="workspace-label">
                        In Review
                      </span>

                      <span className="workspace-value">
                        {projectProgress.inReviewTasks ?? 0}
                      </span>
                    </div>

                    <div>
                      <span className="workspace-label">
                        To Do
                      </span>

                      <span className="workspace-value">
                        {projectProgress.todoTasks ?? 0}
                      </span>
                    </div>
                  </div>

                  <div className="task-progress-section">
                    <div className="task-progress-header">
                      <span>Overall Project Progress</span>

                      <strong>
                        {projectProgress.overallProgress ?? 0}%
                      </strong>
                    </div>

                    <div className="task-progress-track">
                      <div
                        className="task-progress-fill"
                        style={{
                          width: `${Math.min(
                            100,
                            Math.max(
                              0,
                              projectProgress.overallProgress ?? 0
                            )
                          )}%`,
                        }}
                      />
                    </div>
                  </div>
                </>
              )}
            </section>

            <section className="workspace-card">
              <div className="workspace-card-header">
                <div>
                  <span className="workspace-label">
                    Agile Team
                  </span>

                  <h2>Workload</h2>

                  <p>
                    View task distribution and progress across
                    your project team.
                  </p>
                </div>

                <button
                  type="button"
                  className="workspace-secondary-button"
                  disabled={workloadLoading}
                  onClick={handleRefreshWorkload}
                >
                  {workloadLoading
                    ? 'Refreshing...'
                    : 'Refresh Workload'}
                </button>
              </div>

              {workload.length === 0 ? (
                <div className="workspace-empty-state">
                  <h3>No workload data available</h3>

                  <p>
                    Workload information will appear here once
                    project team members and task assignments are
                    available.
                  </p>
                </div>
              ) : (
                <div className="sprint-list">
                  {workload.map((memberWorkload) => (
                    <article
                      key={memberWorkload.userId}
                      className="sprint-card"
                    >
                      <div className="sprint-card-header">
                        <div>
                          <h3>{memberWorkload.fullName}</h3>

                          <span className="workspace-label">
                            Team Member
                          </span>
                        </div>

                        <span className="workspace-status workspace-status-active">
                          {memberWorkload.averageProgress ?? 0}%
                        </span>
                      </div>

                      <div className="workspace-details-grid">
                        <div>
                          <span className="workspace-label">
                            Total Tasks
                          </span>

                          <span className="workspace-value">
                            {memberWorkload.totalTasks ?? 0}
                          </span>
                        </div>

                        <div>
                          <span className="workspace-label">
                            To Do
                          </span>

                          <span className="workspace-value">
                            {memberWorkload.todoTasks ?? 0}
                          </span>
                        </div>

                        <div>
                          <span className="workspace-label">
                            In Progress
                          </span>

                          <span className="workspace-value">
                            {memberWorkload.inProgressTasks ?? 0}
                          </span>
                        </div>

                        <div>
                          <span className="workspace-label">
                            In Review
                          </span>

                          <span className="workspace-value">
                            {memberWorkload.inReviewTasks ?? 0}
                          </span>
                        </div>

                        <div>
                          <span className="workspace-label">
                            Completed
                          </span>

                          <span className="workspace-value">
                            {memberWorkload.completedTasks ?? 0}
                          </span>
                        </div>

                        <div>
                          <span className="workspace-label">
                            Average Progress
                          </span>

                          <span className="workspace-value">
                            {memberWorkload.averageProgress ?? 0}%
                          </span>
                        </div>
                      </div>

                      <div className="task-progress-section">
                        <div className="task-progress-header">
                          <span>Member Progress</span>

                          <strong>
                            {memberWorkload.averageProgress ?? 0}%
                          </strong>
                        </div>

                        <div className="task-progress-track">
                          <div
                            className="task-progress-fill"
                            style={{
                              width: `${Math.min(
                                100,
                                Math.max(
                                  0,
                                  memberWorkload.averageProgress ?? 0
                                )
                              )}%`,
                            }}
                          />
                        </div>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </section>

            {selectedHistoryTaskId && (
              <section className="workspace-card">
                <div className="workspace-card-header">
                  <div>
                    <span className="workspace-label">
                      Collaboration
                    </span>

                    <h2>Task History</h2>

                    <p>
                      View the recorded changes for the selected task.
                    </p>
                  </div>

                  <button
                    type="button"
                    className="workspace-secondary-button"
                    onClick={() => {
                      setSelectedHistoryTaskId(null)
                      setTaskHistory([])
                    }}
                  >
                    Close
                  </button>
                </div>

                {historyLoading ? (
                  <div className="workspace-loading">
                    Loading task history...
                  </div>
                ) : taskHistory.length === 0 ? (
                  <div className="workspace-empty-state">
                    <h3>No history yet</h3>
                    <p>
                      No changes have been recorded for this task yet.
                    </p>
                  </div>
                ) : (
                  <div className="sprint-list">
                    {taskHistory.map((history) => (
                      <article
                        key={history.id}
                        className="sprint-card"
                      >
                        <div className="sprint-card-header">
                          <div>
                            <h3>{history.actionType}</h3>
                            <span className="workspace-label">
                              {history.userName ||
                                history.userEmail ||
                                'Unknown user'}
                            </span>
                          </div>

                          <span className="workspace-label">
                            {formatDateTime(history.createdAt)}
                          </span>
                        </div>

                        <p className="workspace-description">
                          {history.description}
                        </p>

                        {(history.oldValue != null ||
                          history.newValue != null) && (
                          <div className="workspace-details-grid">
                            <div>
                              <span className="workspace-label">
                                Previous Value
                              </span>
                              <span className="workspace-value">
                                {history.oldValue ?? '—'}
                              </span>
                            </div>

                            <div>
                              <span className="workspace-label">
                                New Value
                              </span>
                              <span className="workspace-value">
                                {history.newValue ?? '—'}
                              </span>
                            </div>
                          </div>
                        )}
                      </article>
                    ))}
                  </div>
                )}
              </section>
            )}

            <section className="workspace-card">
              <div className="workspace-card-header">
                <div>
                  <span className="workspace-label">
                    Collaboration
                  </span>

                  <h2>Project Documents</h2>

                  <p>
                    Create and maintain shared project
                    documents for your Agile team.
                  </p>
                </div>

                <button
                  type="button"
                  className="workspace-primary-button"
                  onClick={() => {
                    if (showDocumentForm) {
                      resetDocumentForm()
                    } else {
                      setEditingDocumentId(null)
                      setDocumentForm({
                        title: '',
                        content: '',
                      })
                      setShowDocumentForm(true)
                    }

                    setError('')
                    setSuccess('')
                  }}
                >
                  {showDocumentForm
                    ? 'Cancel'
                    : 'New Document'}
                </button>
              </div>

              {!isLeader && (
                <div className="workspace-info-box">
                  All members of the approved project team can
                  create, view, update, and delete collaborative
                  project documents.
                </div>
              )}

              {showDocumentForm && (
                <form
                  className="workspace-form"
                  onSubmit={
                    editingDocumentId
                      ? handleUpdateDocument
                      : handleCreateDocument
                  }
                >
                  <div className="workspace-form-group">
                    <label htmlFor="documentTitle">
                      Document Title
                    </label>

                    <input
                      id="documentTitle"
                      name="title"
                      type="text"
                      value={documentForm.title}
                      onChange={handleDocumentFormChange}
                      placeholder="Example: Project Requirements"
                      maxLength={255}
                      required
                    />
                  </div>

                  <div className="workspace-form-group">
                    <label htmlFor="documentContent">
                      Document Content
                    </label>

                    <textarea
                      id="documentContent"
                      name="content"
                      value={documentForm.content}
                      onChange={handleDocumentFormChange}
                      placeholder="Write your shared project document here..."
                      rows="12"
                    />
                  </div>

                  <div className="workspace-button-row">
                    <button
                      type="submit"
                      className="workspace-primary-button"
                      disabled={documentLoading}
                    >
                      {documentLoading
                        ? 'Saving...'
                        : editingDocumentId
                          ? 'Update Document'
                          : 'Create Document'}
                    </button>

                    <button
                      type="button"
                      className="workspace-secondary-button"
                      disabled={documentLoading}
                      onClick={resetDocumentForm}
                    >
                      Cancel
                    </button>
                  </div>
                </form>
              )}

              {documents.length === 0 ? (
                <div className="workspace-empty-state">
                  <h3>No project documents yet</h3>

                  <p>
                    Create the first shared document for your
                    project.
                  </p>
                </div>
              ) : (
                <div className="sprint-list">
                  {documents.map((document) => (
                    <article
                      key={document.id}
                      className="sprint-card"
                    >
                      <div className="sprint-card-header">
                        <div>
                          <h3>{document.title}</h3>

                          <span className="workspace-label">
                            Updated{' '}
                            {formatDateTime(
                              document.updatedAt
                            )}
                          </span>
                        </div>
                      </div>

                      {document.content ? (
                        <div className="workspace-description">
                          <p
                            style={{
                              whiteSpace: 'pre-wrap',
                              marginBottom: '1rem',
                            }}
                          >
                            {document.content}
                          </p>
                        </div>
                      ) : (
                        <p className="workspace-description">
                          No content has been added to this
                          document yet.
                        </p>
                      )}

                      <div className="workspace-details-grid">
                        <div>
                          <span className="workspace-label">
                            Created By
                          </span>

                          <span className="workspace-value">
                            {document.createdByName ||
                              document.createdByEmail ||
                              'Unknown'}
                          </span>
                        </div>

                        <div>
                          <span className="workspace-label">
                            Created
                          </span>

                          <span className="workspace-value">
                            {formatDateTime(
                              document.createdAt
                            )}
                          </span>
                        </div>

                        <div>
                          <span className="workspace-label">
                            Last Updated By
                          </span>

                          <span className="workspace-value">
                            {document.updatedByName ||
                              document.updatedByEmail ||
                              'Unknown'}
                          </span>
                        </div>

                        <div>
                          <span className="workspace-label">
                            Last Updated
                          </span>

                          <span className="workspace-value">
                            {formatDateTime(
                              document.updatedAt
                            )}
                          </span>
                        </div>
                      </div>

                      <div className="workspace-button-row">
                        <button
                          type="button"
                          className="workspace-secondary-button"
                          disabled={documentLoading}
                          onClick={() =>
                            handleStartDocumentEdit(
                              document
                            )
                          }
                        >
                          Edit
                        </button>

                        <button
                          type="button"
                          className="workspace-secondary-button"
                          disabled={documentLoading}
                          onClick={() =>
                            handleDeleteDocument(
                              document.id
                            )
                          }
                        >
                          Delete
                        </button>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </section>
          </>
        )}
      </div>
    </div>
  )
}

export default StudentProjectWorkspace