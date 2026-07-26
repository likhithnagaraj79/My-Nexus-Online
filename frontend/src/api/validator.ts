import { apiClient } from './client'
import type { EventDayDto } from './admin'

export const listEventDays = (eventId: string) =>
  apiClient.get<EventDayDto[]>('/api/validator/event-days', { params: { eventId } }).then((r) => r.data)

export interface ScanRequest {
  eventDayId: string
  qrPayload: string
}

export interface ScanResponse {
  checkInScanId: string
  exhibitorPersonId: string
  personName: string
  companyName: string
  alreadyCheckedInToday: boolean
  scannedAt: string
}

export const submitScan = (body: ScanRequest) =>
  apiClient.post<ScanResponse>('/api/validator/scans', body).then((r) => r.data)
