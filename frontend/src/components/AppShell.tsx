import { AppBar, Box, Button, Container, Toolbar, Typography } from '@mui/material'
import { Link as RouterLink, Outlet, useLocation, useNavigate } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { roleLabels } from '../api/types'

/** Navigationsleiste mit Projekten, Benutzern (nur Administration) und Abmeldung. */
export function AppShell() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const handleLogout = async () => {
    await logout()
    navigate('/login', { replace: true })
  }

  const isActive = (prefix: string) => location.pathname.startsWith(prefix)

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'grey.50' }}>
      <AppBar position="static">
        <Toolbar sx={{ gap: 1 }}>
          <Typography variant="h6" component={RouterLink} to="/projects"
            sx={{ color: 'inherit', textDecoration: 'none', mr: 2 }}>
            Projektboard
          </Typography>
          <Button color="inherit" component={RouterLink} to="/projects"
            sx={{ fontWeight: isActive('/projects') ? 700 : 400 }}>
            Projekte
          </Button>
          {user?.role === 'ADMIN' && (
            <Button color="inherit" component={RouterLink} to="/users"
              sx={{ fontWeight: isActive('/users') ? 700 : 400 }}>
              Benutzer
            </Button>
          )}
          <Box sx={{ flexGrow: 1 }} />
          {user && (
            <Typography variant="body2" sx={{ mr: 1 }}>
              {user.displayName} ({roleLabels[user.role]})
            </Typography>
          )}
          <Button color="inherit" onClick={handleLogout}>Abmelden</Button>
        </Toolbar>
      </AppBar>
      <Container maxWidth="lg" sx={{ py: 3 }}>
        <Outlet />
      </Container>
    </Box>
  )
}
