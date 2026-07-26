import { Navigate, Outlet } from 'react-router-dom'
import type { Role } from '../api/types'
import { useAuthStore } from '../store/authStore'

interface RequireAuthProps {
  allow: Role[]
}

export default function RequireAuth({ allow }: RequireAuthProps) {
  const { accessToken, role, mustChangePassword } = useAuthStore()

  if (!accessToken) {
    return <Navigate to="/login" replace />
  }
  if (mustChangePassword) {
    return <Navigate to="/change-password" replace />
  }
  if (!role || !allow.includes(role)) {
    return <Navigate to="/403" replace />
  }
  return <Outlet />
}
