import { apiClient } from './client'

export interface LinkInfo {
  linkId: string
  eventName: string
  eventStartDate: string
  eventEndDate: string
}

export interface CompanySuggestion {
  id: string
  name: string
}

export interface PersonInput {
  name: string
  designation: string
}

export interface SubmissionRequest {
  companyName: string
  people: PersonInput[]
  recaptchaToken: string
}

export interface SubmissionResponse {
  submissionId: string
  companyId: string
  personCount: number
}

export const getLinkInfo = (linkId: string) =>
  apiClient.get<LinkInfo>(`/api/public/links/${linkId}`).then((r) => r.data)

export const searchCompanies = (query: string) =>
  apiClient.get<CompanySuggestion[]>('/api/public/companies', { params: { query } }).then((r) => r.data)

export const submitRegistration = (linkId: string, body: SubmissionRequest) =>
  apiClient.post<SubmissionResponse>(`/api/public/links/${linkId}/submissions`, body).then((r) => r.data)
