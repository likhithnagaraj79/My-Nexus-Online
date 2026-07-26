import { apiClient } from './client'
import type { PagedModel, Role } from './types'

export interface ActorSummary {
  id: string
  username: string
  email: string | null
  role: Role
  active: boolean
  accountLocked: boolean
  createdAt: string
}

export interface CreateOrganiserRequest {
  username: string
  email: string
  temporaryPassword: string
}

export interface CreateCrewOrValidatorRequest {
  username: string
  temporaryPassword: string
  aadharNumber: string
  phoneNumber: string
}

export interface CreatedActorResponse {
  userId: string
  totpQrPngBase64: string | null
}

export interface EventDto {
  id: string
  name: string
  startDate: string
  endDate: string
  active: boolean
}

export interface CreateEventRequest {
  name: string
  startDate: string
  endDate: string
}

export interface EventDayDto {
  id: string
  dayNumber: number
  date: string
}

export interface CreateEventDayRequest {
  dayNumber: number
  date: string
}

export type AuditEventType =
  | 'LOGIN_SUCCESS'
  | 'LOGIN_FAILURE'
  | 'LOGOUT'
  | 'ACCOUNT_LOCKED'
  | 'ACCOUNT_UNLOCKED'

export interface AuditLogEntry {
  id: string
  userId: string | null
  usernameAttempted: string | null
  eventType: AuditEventType
  ipAddress: string | null
  userAgent: string | null
  occurredAt: string
}

export interface ListActorsParams {
  role?: Role
  active?: boolean
  page?: number
  size?: number
}

export const listActors = (params: ListActorsParams) =>
  apiClient.get<PagedModel<ActorSummary>>('/api/admin/actors', { params }).then((r) => r.data)

export const createOrganiser = (body: CreateOrganiserRequest) =>
  apiClient.post<CreatedActorResponse>('/api/admin/actors/organisers', body).then((r) => r.data)

export const createCrew = (body: CreateCrewOrValidatorRequest) =>
  apiClient.post<CreatedActorResponse>('/api/admin/actors/crew', body).then((r) => r.data)

export const createValidator = (body: CreateCrewOrValidatorRequest) =>
  apiClient.post<CreatedActorResponse>('/api/admin/actors/validators', body).then((r) => r.data)

export const unlockActor = (id: string) => apiClient.post<void>(`/api/admin/actors/${id}/unlock`).then((r) => r.data)

export const activateActor = (id: string) =>
  apiClient.patch<void>(`/api/admin/actors/${id}/activate`).then((r) => r.data)

export const deactivateActor = (id: string) =>
  apiClient.patch<void>(`/api/admin/actors/${id}/deactivate`).then((r) => r.data)

export const regenerateTotpQr = (id: string) =>
  apiClient.post<{ totpQrPngBase64: string }>(`/api/admin/actors/${id}/totp-qr/regenerate`).then((r) => r.data)

export const createEvent = (body: CreateEventRequest) =>
  apiClient.post<EventDto>('/api/admin/events', body).then((r) => r.data)

export const listEvents = () => apiClient.get<EventDto[]>('/api/admin/events').then((r) => r.data)

export const activateEvent = (id: string) =>
  apiClient.post<EventDto>(`/api/admin/events/${id}/activate`).then((r) => r.data)

export const deactivateEvent = (id: string) =>
  apiClient.post<EventDto>(`/api/admin/events/${id}/deactivate`).then((r) => r.data)

export const createEventDay = (eventId: string, body: CreateEventDayRequest) =>
  apiClient.post<EventDayDto>(`/api/admin/events/${eventId}/days`, body).then((r) => r.data)

export const listEventDays = (eventId: string) =>
  apiClient.get<EventDayDto[]>(`/api/admin/events/${eventId}/days`).then((r) => r.data)

export interface ListAuditLogsParams {
  userId?: string
  eventType?: AuditEventType
  from?: string
  to?: string
  page?: number
  size?: number
}

export const listAuditLogs = (params: ListAuditLogsParams) =>
  apiClient.get<PagedModel<AuditLogEntry>>('/api/admin/audit-logs', { params }).then((r) => r.data)

export const exportBackup = () =>
  apiClient.get('/api/admin/backup/export', { responseType: 'blob' }).then((r) => r.data as Blob)

export const restoreBackup = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('confirm', 'true')
  return apiClient
    .post<void>('/api/admin/backup/restore', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then((r) => r.data)
}
