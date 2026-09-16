import api from './api.js'

export const createProjectDocument = (documentData) =>
  api.post('/project-documents', documentData)

export const getMyProjectDocuments = () =>
  api.get('/project-documents/my-project')

export const getProjectDocumentById = (documentId) =>
  api.get(`/project-documents/${documentId}`)

export const updateProjectDocument = (documentId, documentData) =>
  api.put(`/project-documents/${documentId}`, documentData)

export const deleteProjectDocument = (documentId) =>
  api.delete(`/project-documents/${documentId}`)