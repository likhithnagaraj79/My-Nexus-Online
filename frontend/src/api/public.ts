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

export interface DelegateLinkInfo {
  linkId: string
  eventName: string
  eventStartDate: string
  eventEndDate: string
}

export interface DelegateSubmissionRequest {
  name: string
  companyName: string
  designation: string
  mobileNumber: string
  email: string
  recaptchaToken: string
}

export interface DelegateSubmissionResponse {
  delegateId: string
}

export const getDelegateLinkInfo = (linkId: string) =>
  apiClient.get<DelegateLinkInfo>(`/api/public/delegate-links/${linkId}`).then((r) => r.data)

export const submitDelegateRegistration = (linkId: string, body: DelegateSubmissionRequest) =>
  apiClient
    .post<DelegateSubmissionResponse>(`/api/public/delegate-links/${linkId}/submissions`, body)
    .then((r) => r.data)
