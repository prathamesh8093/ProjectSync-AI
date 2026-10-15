import api from './api.js'

export const createTaskComment = (commentData) =>
  api.post('/task-comments', commentData)

export const getTaskComments = (taskId) =>
  api.get(`/task-comments/task/${taskId}`)

export const getTaskCommentById = (commentId) =>
  api.get(`/task-comments/${commentId}`)

export const updateTaskComment = (commentId, commentData) =>
  api.put(`/task-comments/${commentId}`, commentData)

export const deleteTaskComment = (commentId) =>
  api.delete(`/task-comments/${commentId}`)