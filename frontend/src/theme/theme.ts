import { createTheme } from '@mui/material/styles'
import type {} from '@mui/x-data-grid/themeAugmentation'

export const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#1B3A6B',
      light: '#3D5A8A',
      dark: '#102544',
      contrastText: '#FFFFFF',
    },
    secondary: {
      main: '#2E8B8B',
      contrastText: '#FFFFFF',
    },
    background: {
      default: '#F4F6F8',
      paper: '#FFFFFF',
    },
    success: {
      main: '#2E7D32',
      light: '#E8F5E9',
    },
    warning: {
      main: '#ED6C02',
    },
    error: {
      main: '#C62828',
    },
  },
  typography: {
    fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
    h4: { fontWeight: 600 },
    h5: { fontWeight: 600 },
    h6: { fontWeight: 600 },
  },
  shape: {
    borderRadius: 8,
  },
  spacing: 8,
  components: {
    MuiAppBar: {
      styleOverrides: {
        root: {
          backgroundColor: '#1B3A6B',
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
        },
      },
    },
    MuiPaper: {
      defaultProps: {
        elevation: 1,
      },
    },
    MuiDataGrid: {
      defaultProps: {
        density: 'compact',
      },
    },
  },
})
