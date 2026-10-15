import api from './api.js'

export const uploadProjectFile = (file) => {
  const formData = new FormData()

  formData.append('file', file)

  return api.post('/project-files', formData)
}

export const getMyProjectFiles = () =>
  api.get('/project-files/my-project')

export const getProjectFileById = (fileId) =>
  api.get(`/project-files/${fileId}`)

export const downloadProjectFile = (fileId) =>
  api.get(`/project-files/${fileId}/download`, {
    responseType: 'blob',
  })

export const deleteProjectFile = (fileId) =>
  api.delete(`/project-files/${fileId}`)