import api from './api.js'

export const getFacultyProjects = () =>
  api.get('/faculty/projects')

export const getFacultyProjectById = (projectId) =>
  api.get(`/faculty/projects/${projectId}`)