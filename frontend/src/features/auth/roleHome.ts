import type { Role } from '../../api/types'

export const ROLE_HOME: Record<Role, string> = {
  ADMIN: '/admin/actors',
  ORGANISER: '/organiser/dashboard',
  CREW: '/crew/exhibitor-passes',
  VALIDATOR: '/validator/scan',
}
