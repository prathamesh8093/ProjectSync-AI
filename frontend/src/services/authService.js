import api from './api.js'

export const loginUser = (email, password) => {
  return api.post('/auth/login', { email, password })
}

export const registerUser = (fullName, email, password, role) => {
  return api.post('/auth/register', { fullName, email, password, role })
}