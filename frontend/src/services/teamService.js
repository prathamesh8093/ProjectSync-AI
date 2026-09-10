import api from './api.js'

export const createTeam = (teamName) => api.post('/teams', { teamName })

export const getMyTeam = () => api.get('/teams/my-team')

export const addTeamMember = (teamId, studentUserId) =>
  api.post(`/teams/${teamId}/members`, { studentUserId })

export const getTeamMembers = (teamId) => api.get(`/teams/${teamId}/members`)