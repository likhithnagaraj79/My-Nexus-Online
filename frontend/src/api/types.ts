export type Role = 'ADMIN' | 'ORGANISER' | 'CREW' | 'VALIDATOR'

export interface ProblemDetails {
  type?: string
  title: string
  status: number
  detail: string
  instance?: string
  errorCode: string
  fieldErrors?: string[]
  [key: string]: unknown
}

export interface PagedModel<T> {
  content: T[]
  metadata: {
    size: number
    number: number
    totalElements: number
    totalPages: number
  }
}
