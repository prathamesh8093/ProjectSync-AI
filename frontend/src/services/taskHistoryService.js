import api from './api.js'

export const getTaskHistory = (taskId) =>
  api.get(`/task-history/task/${taskId}`)

export const getTaskHistoryById = (historyId) =>
  api.get(`/task-history/${historyId}`)