import { apiClient } from './client'
import type { LabourPassSummary } from './organiser'

export type LabourPassType = 'VENDOR' | 'EXHIBITOR' | 'FABRICATOR_LABOUR'

export interface CreateLabourPassRequest {
  passType: LabourPassType
  passCount: number
  phoneNumber: string
  stallNumber?: string
}

export interface ExhibitorPassSummary {
  id: string
  name: string
  designation: string
  companyId: string
  companyName: string
  printed: boolean
  printedAt: string | null
  issued: boolean
  issuedAt: string | null
}

/** Exactly one of personIds or companyId must be set. */
export interface PrintRequest {
  personIds?: string[]
  companyId?: string
}

export interface IssueRequest {
  phoneNumber: string
}

export const createLabourPass = (body: CreateLabourPassRequest) =>
  apiClient.post<void>('/api/crew/labour-passes', body).then((r) => r.data)

export const listLabourPasses = (eventId: string) =>
  apiClient
    .get<LabourPassSummary[]>('/api/crew/labour-passes', { params: { eventId } })
    .then((r) => r.data)

export interface ListExhibitorPassesParams {
  companyId?: string
  printed?: boolean
  issued?: boolean
  q?: string
}

export const listExhibitorPasses = (params: ListExhibitorPassesParams) =>
  apiClient.get<ExhibitorPassSummary[]>('/api/crew/exhibitor-passes', { params }).then((r) => r.data)

export const printExhibitorPasses = (body: PrintRequest) =>
  apiClient.post<ExhibitorPassSummary[]>('/api/crew/exhibitor-passes/print', body).then((r) => r.data)

export const issueExhibitorPass = (id: string, body: IssueRequest) =>
  apiClient.patch<ExhibitorPassSummary>(`/api/crew/exhibitor-passes/${id}/issue`, body).then((r) => r.data)

export const exhibitorPassQrCodeUrl = (id: string) => `/api/crew/exhibitor-passes/${id}/qr-code`
