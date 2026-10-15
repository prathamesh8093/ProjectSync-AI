import api from './api.js'

// Create a new sprint for a project
export const createSprint = (sprintData) =>
  api.post('/sprints', sprintData)

// Get all sprints for the current student's project
export const getMyProjectSprints = () =>
  api.get('/sprints/my-project')

// Get a sprint by ID
export const getSprintById = (sprintId) =>
  api.get(`/sprints/${sprintId}`)

// Update a sprint
export const updateSprint = (sprintId, sprintData) =>
  api.put(`/sprints/${sprintId}`, sprintData)

// Update sprint status
export const updateSprintStatus = (sprintId, status) =>
  api.put(`/sprints/${sprintId}/status`, null, {
    params: { status },
  })