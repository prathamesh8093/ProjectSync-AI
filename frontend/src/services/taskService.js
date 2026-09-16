import api from './api.js'

export const createTask = (taskData) =>
  api.post('/tasks', taskData)

export const getMyProjectTasks = () =>
  api.get('/tasks/my-project')

export const getTasksBySprint = (sprintId) =>
  api.get(`/tasks/sprint/${sprintId}`)

export const getTaskById = (taskId) =>
  api.get(`/tasks/${taskId}`)

export const updateTask = (taskId, taskData) =>
  api.put(`/tasks/${taskId}`, taskData)

export const updateTaskStatus = (taskId, status) =>
  api.put(`/tasks/${taskId}/status`, null, {
    params: { status },
  })

export const updateTaskProgress = (taskId, progress) =>
  api.put(`/tasks/${taskId}/progress`, null, {
    params: { progress },
  })