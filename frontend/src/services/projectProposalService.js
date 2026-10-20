import api from './api.js'

// Create a new proposal
export const createProposal = (proposalData) =>
    api.post('/proposals', proposalData)

// Get current student's team proposal
export const getMyProposal = () =>
    api.get('/proposals/my-proposal')

// Get proposal by ID
export const getProposalById = (proposalId) =>
    api.get(`/proposals/${proposalId}`)

// Update a draft proposal
export const updateProposal = (proposalId, proposalData) =>
    api.put(`/proposals/${proposalId}`, proposalData)

// Submit a draft proposal
export const submitProposal = (proposalId) =>
    api.post(`/proposals/${proposalId}/submit`)

// Generate AI proposal draft from idea and tech stack
export const generateAiProposalDraft = (draftRequest) =>
    api.post('/proposals/ai-draft', draftRequest)

// Generate Jira-style Sprint & Story Point Breakdown
export const getSprintBreakdown = (proposalId) =>
    api.get(`/proposals/${proposalId}/sprint-breakdown`)
