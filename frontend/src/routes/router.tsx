import { createBrowserRouter, Navigate } from 'react-router-dom'
import AppShell from '../layouts/AppShell'
import AuthLayout from '../layouts/AuthLayout'
import PublicLayout from '../layouts/PublicLayout'
import ChangePasswordPage from '../features/auth/ChangePasswordPage'
import LoginPage from '../features/auth/LoginPage'
import TotpPage from '../features/auth/TotpPage'
import ActorsPage from '../features/admin/ActorsPage'
import EventsPage from '../features/admin/EventsPage'
import AuditLogsPage from '../features/admin/AuditLogsPage'
import BackupPage from '../features/admin/BackupPage'
import ForbiddenPage from '../features/shared/ForbiddenPage'
import NotFoundPage from '../features/shared/NotFoundPage'
import LabourPassesTable from '../features/shared/LabourPassesTable'
import DashboardPage from '../features/organiser/DashboardPage'
import AnalyticsPage from '../features/organiser/AnalyticsPage'
import CheckInExportPage from '../features/organiser/CheckInExportPage'
import LinksPage from '../features/organiser/LinksPage'
import LabourPassesPage from '../features/crew/LabourPassesPage'
import ExhibitorPassesPage from '../features/crew/ExhibitorPassesPage'
import ScanPage from '../features/validator/ScanPage'
import RegistrationPage from '../features/public/RegistrationPage'
import RequireAuth from './RequireAuth'

export const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/login" replace /> },
  {
    element: <PublicLayout />,
    children: [{ path: '/register/:linkId', element: <RegistrationPage /> }],
  },
  {
    element: <AuthLayout />,
    children: [
      { path: '/login', element: <LoginPage /> },
      { path: '/login/totp', element: <TotpPage /> },
      { path: '/change-password', element: <ChangePasswordPage /> },
    ],
  },
  { path: '/403', element: <ForbiddenPage /> },
  {
    element: <RequireAuth allow={['ADMIN']} />,
    children: [
      {
        element: <AppShell />,
        children: [
          { path: '/admin', element: <Navigate to="/admin/actors" replace /> },
          { path: '/admin/actors', element: <ActorsPage /> },
          { path: '/admin/events', element: <EventsPage /> },
          { path: '/admin/labour-passes', element: <LabourPassesTable /> },
          { path: '/admin/audit-logs', element: <AuditLogsPage /> },
          { path: '/admin/backup', element: <BackupPage /> },
        ],
      },
    ],
  },
  {
    element: <RequireAuth allow={['ORGANISER']} />,
    children: [
      {
        element: <AppShell />,
        children: [
          { path: '/organiser', element: <Navigate to="/organiser/dashboard" replace /> },
          { path: '/organiser/dashboard', element: <DashboardPage /> },
          { path: '/organiser/analytics', element: <AnalyticsPage /> },
          { path: '/organiser/labour-passes', element: <LabourPassesTable /> },
          { path: '/organiser/check-ins', element: <CheckInExportPage /> },
          { path: '/organiser/links', element: <LinksPage /> },
        ],
      },
    ],
  },
  {
    element: <RequireAuth allow={['CREW']} />,
    children: [
      {
        element: <AppShell />,
        children: [
          { path: '/crew', element: <Navigate to="/crew/exhibitor-passes" replace /> },
          { path: '/crew/labour-passes', element: <LabourPassesPage /> },
          { path: '/crew/exhibitor-passes', element: <ExhibitorPassesPage /> },
        ],
      },
    ],
  },
  {
    element: <RequireAuth allow={['VALIDATOR']} />,
    children: [
      {
        element: <AppShell />,
        children: [
          { path: '/validator', element: <Navigate to="/validator/scan" replace /> },
          { path: '/validator/scan', element: <ScanPage /> },
        ],
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
])
