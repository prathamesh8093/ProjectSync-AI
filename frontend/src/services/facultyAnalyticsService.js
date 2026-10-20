import api from './api.js'

export const getMonitoredProjects = () => api.get('/faculty/analytics/projects')

export const getProjectAnalytics = (projectId, sprintId) =>
  api.get(`/faculty/analytics/projects/${projectId}/analytics`, { params: sprintId ? { sprintId } : {} })

export const analyzeSprint = (projectId, sprintId) =>
  api.post(`/faculty/analytics/projects/${projectId}/sprints/${sprintId}/analyze`)

export const getEvaluationReport = (projectId) =>
  api.get(`/faculty/analytics/projects/${projectId}/export-report`, { responseType: 'text' })

export const getRubricEvaluation = (projectId) =>
  api.get(`/faculty/analytics/projects/${projectId}/rubric-evaluation`)

export const getCodeInspection = (projectId) =>
  api.get(`/faculty/analytics/projects/${projectId}/code-inspection`)

export const getDelayRiskPrediction = (projectId) =>
  api.get(`/faculty/analytics/projects/${projectId}/delay-risk-prediction`)

