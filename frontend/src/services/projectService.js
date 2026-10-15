import api from './api.js'

// Create a new Agile project from an approved proposal
export const createProject = (projectData) =>
  api.post('/projects', projectData)

// Get the current student's team Agile project
export const getMyProject = () =>
  api.get('/projects/my-project')

// Get an Agile project by ID
export const getProjectById = (projectId) =>
  api.get(`/projects/${projectId}`)

// Update an Agile project
export const updateProject = (projectId, projectData) =>
  api.put(`/projects/${projectId}`, projectData)

// Update Agile project status
export const updateProjectStatus = (projectId, status) =>
  api.put(`/projects/${projectId}/status`, null, {
    params: { status },
  })