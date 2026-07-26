import AnalyticsIcon from '@mui/icons-material/Analytics'
import BackupIcon from '@mui/icons-material/Backup'
import BadgeIcon from '@mui/icons-material/Badge'
import DashboardIcon from '@mui/icons-material/Dashboard'
import EventIcon from '@mui/icons-material/Event'
import FileDownloadIcon from '@mui/icons-material/FileDownload'
import HistoryIcon from '@mui/icons-material/History'
import LinkIcon from '@mui/icons-material/Link'
import PeopleIcon from '@mui/icons-material/People'
import QrCode2Icon from '@mui/icons-material/QrCode2'
import QrCodeScannerIcon from '@mui/icons-material/QrCodeScanner'
import type { SvgIconComponent } from '@mui/icons-material'
import type { Role } from '../api/types'

export interface NavItem {
  label: string
  path: string
  icon: SvgIconComponent
}

export const NAV_ITEMS: Record<Role, NavItem[]> = {
  ADMIN: [
    { label: 'Actors', path: '/admin/actors', icon: PeopleIcon },
    { label: 'Events', path: '/admin/events', icon: EventIcon },
    { label: 'Labour Passes', path: '/admin/labour-passes', icon: BadgeIcon },
    { label: 'Audit Logs', path: '/admin/audit-logs', icon: HistoryIcon },
    { label: 'Backup', path: '/admin/backup', icon: BackupIcon },
  ],
  ORGANISER: [
    { label: 'Dashboard', path: '/organiser/dashboard', icon: DashboardIcon },
    { label: 'Analytics', path: '/organiser/analytics', icon: AnalyticsIcon },
    { label: 'Labour Passes', path: '/organiser/labour-passes', icon: BadgeIcon },
    { label: 'Check-ins', path: '/organiser/check-ins', icon: FileDownloadIcon },
    { label: 'Registration Links', path: '/organiser/links', icon: LinkIcon },
  ],
  CREW: [
    { label: 'Labour Passes', path: '/crew/labour-passes', icon: BadgeIcon },
    { label: 'Exhibitor Passes', path: '/crew/exhibitor-passes', icon: QrCode2Icon },
  ],
  VALIDATOR: [{ label: 'Scan', path: '/validator/scan', icon: QrCodeScannerIcon }],
}
