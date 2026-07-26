import LogoutIcon from '@mui/icons-material/Logout'
import MenuIcon from '@mui/icons-material/Menu'
import {
  AppBar,
  Box,
  Chip,
  Container,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
} from '@mui/material'
import { useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { logout as logoutRequest } from '../api/auth'
import type { Role } from '../api/types'
import { useAuthStore } from '../store/authStore'
import { NAV_ITEMS } from './navItems'

const DRAWER_WIDTH = 240

export default function AppShell() {
  const navigate = useNavigate()
  const { role, username, refreshToken, logout } = useAuthStore()
  const items = NAV_ITEMS[role as Role] ?? []
  const [mobileOpen, setMobileOpen] = useState(false)

  const handleLogout = async () => {
    if (refreshToken) {
      try {
        await logoutRequest(refreshToken)
      } catch {
        // Ignore — clear local session regardless of whether the server call succeeded.
      }
    }
    logout()
    navigate('/login', { replace: true })
  }

  const navList = (
    <List>
      {items.map((item) => (
        <ListItemButton
          key={item.path}
          component={NavLink}
          to={item.path}
          onClick={() => setMobileOpen(false)}
          sx={{
            '&.active': {
              bgcolor: 'action.selected',
              borderRight: '3px solid',
              borderColor: 'primary.main',
            },
          }}
        >
          <ListItemIcon>
            <item.icon fontSize="small" />
          </ListItemIcon>
          <ListItemText primary={item.label} />
        </ListItemButton>
      ))}
    </List>
  )

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <AppBar position="fixed" sx={{ zIndex: (t) => t.zIndex.drawer + 1 }}>
        <Toolbar>
          <IconButton
            color="inherit"
            aria-label="open navigation"
            onClick={() => setMobileOpen(true)}
            sx={{ mr: 2, display: { sm: 'none' } }}
          >
            <MenuIcon />
          </IconButton>
          <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
            Exhibitor Registration System
          </Typography>
          <Chip
            label={`${username} · ${role}`}
            size="small"
            sx={{ bgcolor: 'rgba(255,255,255,0.15)', color: 'white', mr: 2, display: { xs: 'none', sm: 'flex' } }}
          />
          <IconButton color="inherit" onClick={handleLogout} aria-label="logout">
            <LogoutIcon />
          </IconButton>
        </Toolbar>
      </AppBar>

      <Drawer
        variant="temporary"
        open={mobileOpen}
        onClose={() => setMobileOpen(false)}
        ModalProps={{ keepMounted: true }}
        sx={{
          display: { xs: 'block', sm: 'none' },
          [`& .MuiDrawer-paper`]: { width: DRAWER_WIDTH, boxSizing: 'border-box' },
        }}
      >
        <Toolbar />
        <Divider />
        {navList}
      </Drawer>

      <Drawer
        variant="permanent"
        sx={{
          width: DRAWER_WIDTH,
          flexShrink: 0,
          display: { xs: 'none', sm: 'block' },
          [`& .MuiDrawer-paper`]: { width: DRAWER_WIDTH, boxSizing: 'border-box' },
        }}
      >
        <Toolbar />
        <Divider />
        {navList}
      </Drawer>

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          bgcolor: 'background.default',
          minHeight: '100vh',
          width: { sm: `calc(100% - ${DRAWER_WIDTH}px)` },
        }}
      >
        <Toolbar />
        <Container maxWidth="xl" sx={{ py: 4 }}>
          <Outlet />
        </Container>
      </Box>
    </Box>
  )
}
