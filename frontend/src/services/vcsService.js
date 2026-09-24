import api from './api.js'

export const vcsService = {
  // Repositories
  listRepositories: async () => {
    const res = await api.get('/vcs/repositories')
    return res.data
  },

  createRepository: async (data) => {
    const res = await api.post('/vcs/repositories', data)
    return res.data
  },

  registerRepository: async (data) => {
    const res = await api.post('/vcs/repositories/register', data)
    return res.data
  },

  getRepository: async (id) => {
    const res = await api.get(`/vcs/repositories/${id}`)
    return res.data
  },

  updateRepository: async (id, data) => {
    const res = await api.put(`/vcs/repositories/${id}`, data)
    return res.data
  },

  deleteRepository: async (id, deleteFiles = false) => {
    const res = await api.delete(`/vcs/repositories/${id}?deleteFiles=${deleteFiles}`)
    return res.data
  },

  // Status & Staging
  getStatus: async (id) => {
    const res = await api.get(`/vcs/repositories/${id}/status`)
    return res.data
  },

  stageFiles: async (id, filePaths) => {
    const res = await api.post(`/vcs/repositories/${id}/stage`, { filePaths })
    return res.data
  },

  stageAll: async (id) => {
    const res = await api.post(`/vcs/repositories/${id}/stage-all`)
    return res.data
  },

  unstageFiles: async (id, filePaths) => {
    const res = await api.post(`/vcs/repositories/${id}/unstage`, { filePaths })
    return res.data
  },

  unstageAll: async (id) => {
    const res = await api.post(`/vcs/repositories/${id}/unstage-all`)
    return res.data
  },

  discardChanges: async (id, filePaths) => {
    const res = await api.post(`/vcs/repositories/${id}/discard`, { filePaths })
    return res.data
  },

  // Commits & History
  createCommit: async (id, { message, authorName, authorEmail }) => {
    const res = await api.post(`/vcs/repositories/${id}/commit`, { message, authorName, authorEmail })
    return res.data
  },

  getCommitHistory: async (id, branch = '', limit = 50) => {
    const params = new URLSearchParams()
    if (branch) params.append('branch', branch)
    if (limit) params.append('limit', limit)
    const res = await api.get(`/vcs/repositories/${id}/commits?${params.toString()}`)
    return res.data
  },

  getCommitDetails: async (id, sha) => {
    const res = await api.get(`/vcs/repositories/${id}/commits/${sha}`)
    return res.data
  },

  // Branches
  listBranches: async (id) => {
    const res = await api.get(`/vcs/repositories/${id}/branches`)
    return res.data
  },

  createBranch: async (id, branchName, startPoint = null) => {
    const res = await api.post(`/vcs/repositories/${id}/branches`, { branchName, startPoint })
    return res.data
  },

  checkoutBranch: async (id, name) => {
    const res = await api.post(`/vcs/repositories/${id}/branches/checkout?name=${encodeURIComponent(name)}`)
    return res.data
  },

  deleteBranch: async (id, name, force = false) => {
    const res = await api.delete(`/vcs/repositories/${id}/branches/${encodeURIComponent(name)}?force=${force}`)
    return res.data
  },

  // Diff
  getDiff: async (id, path = '', staged = false) => {
    const params = new URLSearchParams()
    if (path) params.append('path', path)
    params.append('staged', staged)
    const res = await api.get(`/vcs/repositories/${id}/diff?${params.toString()}`)
    return res.data
  },

  // Remotes & GitHub Sync
  getRemotes: async (id) => {
    const res = await api.get(`/vcs/repositories/${id}/remotes`)
    return res.data
  },

  setRemote: async (id, data) => {
    const res = await api.post(`/vcs/repositories/${id}/remotes`, data)
    return res.data
  },

  fetchRemote: async (id, data) => {
    const res = await api.post(`/vcs/repositories/${id}/fetch`, data)
    return res.data
  },

  pullRemote: async (id, data) => {
    const res = await api.post(`/vcs/repositories/${id}/pull`, data)
    return res.data
  },

  pushRemote: async (id, data) => {
    const res = await api.post(`/vcs/repositories/${id}/push`, data)
    return res.data
  },
}

export default vcsService
