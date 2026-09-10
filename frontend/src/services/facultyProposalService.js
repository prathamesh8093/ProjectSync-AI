import api from './api.js'

export const getFacultyProposals = () => api.get('/faculty/proposals')

export const getFacultyProposalById = (proposalId) =>
  api.get(`/faculty/proposals/${proposalId}`)

export const reviewFacultyProposal = (proposalId, reviewData) =>
  api.post(`/faculty/proposals/${proposalId}/review`, reviewData)