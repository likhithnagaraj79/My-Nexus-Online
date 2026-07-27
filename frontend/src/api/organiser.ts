import { apiClient } from './client'

export interface DashboardStats {
  printedBadgeCount: number
  issuedBadgeCount: number
  submissionCount: number
  exhibitorPersonCount: number
  checkInCount: number
}

export interface AnalyticsStats {
  checkInCount: number
  vendorPassCount: number
  exhibitorPassCount: number
  fabricatorLabourPassCount: number
  printedExhibitorBadgeCount: number
}

export interface LabourPassSummary {
  id: string
  passType: 'VENDOR' | 'EXHIBITOR' | 'FABRICATOR_LABOUR'
  passCount: number
  phoneNumber: string
  stallNumber: string | null
  issuedByUsername: string
  createdAt: string
}

export interface CreateLinkRequest {
  expiresAt?: string
}

export interface LinkResponse {
  id: string
  publicUrl: string
  expiresAt: string | null
  active: boolean
  createdAt: string
}

export const getDashboard = () => apiClient.get<DashboardStats>('/api/organiser/dashboard').then((r) => r.data)

export const getAnalytics = () => apiClient.get<AnalyticsStats>('/api/organiser/analytics').then((r) => r.data)

export const listLabourPasses = () =>
  apiClient.get<LabourPassSummary[]>('/api/organiser/labour-passes').then((r) => r.data)

export const exportCheckIns = (eventDayId: string) =>
  apiClient
    .get('/api/organiser/check-ins/export', { params: { eventDayId }, responseType: 'blob' })
    .then((r) => r.data as Blob)

export const createLink = (body: CreateLinkRequest) =>
  apiClient.post<LinkResponse>('/api/organiser/links', body).then((r) => r.data)

export const listLinks = () => apiClient.get<LinkResponse[]>('/api/organiser/links').then((r) => r.data)

export const deactivateLink = (id: string) =>
  apiClient.patch<LinkResponse>(`/api/organiser/links/${id}/deactivate`).then((r) => r.data)

export interface CreateDelegateLinkRequest {
  expiresAt?: string
}

export interface DelegateLinkResponse {
  id: string
  publicUrl: string
  expiresAt: string | null
  active: boolean
  createdAt: string
}

export const createDelegateLink = (body: CreateDelegateLinkRequest) =>
  apiClient.post<DelegateLinkResponse>('/api/organiser/delegate-links', body).then((r) => r.data)

export const listDelegateLinks = () =>
  apiClient.get<DelegateLinkResponse[]>('/api/organiser/delegate-links').then((r) => r.data)

export const deactivateDelegateLink = (id: string) =>
  apiClient.patch<DelegateLinkResponse>(`/api/organiser/delegate-links/${id}/deactivate`).then((r) => r.data)
