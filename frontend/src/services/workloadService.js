import api from './api.js'

export const getMyProjectWorkload = () =>
  api.get('/workload/my-project')