import api from './api.js'

export const searchStudents = (query) => api.get(`/users/search?query=${encodeURIComponent(query)}`)

