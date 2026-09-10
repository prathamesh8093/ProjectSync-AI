import api from './api.js'

export const getStudentProfile = () => api.get('/student-profile')

export const updateStudentProfile = (data) => api.put('/student-profile', data)

export const getFacultyProfile = () => api.get('/faculty-profile')

export const updateFacultyProfile = (data) => api.put('/faculty-profile', data)