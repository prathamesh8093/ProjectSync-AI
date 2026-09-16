import api from './api.js'

export const getMyProjectProgress = () =>
  api.get('/projects/my-project/progress')