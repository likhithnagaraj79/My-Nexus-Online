import { apiClient } from './client'
import type { EventDayDto, EventDto } from './admin'

export const getActiveEvent = () => apiClient.get<EventDto>('/api/common/events/active').then((r) => r.data)

export const getEventDays = (eventId: string) =>
  apiClient.get<EventDayDto[]>(`/api/common/events/${eventId}/days`).then((r) => r.data)
