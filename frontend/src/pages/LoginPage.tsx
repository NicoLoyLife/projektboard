import { Alert, Box, Button, Paper, TextField, Typography } from '@mui/material'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'

/** Anmeldeseite. Nach Erfolg geht es zur ursprünglich aufgerufenen Seite oder zur Projektübersicht. */
export function LoginPage() {
  const { user, loading, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (!loading && user) {
    return <Navigate to="/projects" replace />
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login(username.trim(), password)
      const from = (location.state as { from?: string } | null)?.from
      navigate(from && from !== '/login' ? from : '/projects', { replace: true })
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) {
        setError('Benutzername oder Passwort ist falsch.')
      } else {
        setError('Die Anmeldung ist fehlgeschlagen. Bitte später erneut versuchen.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', bgcolor: 'grey.100', px: 2 }}>
      <Paper component="form" onSubmit={handleSubmit} sx={{ p: 4, width: '100%', maxWidth: 400 }}>
        <Typography variant="h5" gutterBottom>Projektboard</Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>Anmeldung</Typography>
        <TextField label="Benutzername" value={username} onChange={(event) => setUsername(event.target.value)}
          fullWidth margin="normal" autoComplete="username" autoFocus required />
        <TextField label="Passwort" type="password" value={password}
          onChange={(event) => setPassword(event.target.value)}
          fullWidth margin="normal" autoComplete="current-password" required />
        {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}
        <Button type="submit" variant="contained" fullWidth sx={{ mt: 3 }} disabled={submitting}>
          {submitting ? 'Anmeldung läuft' : 'Anmelden'}
        </Button>
      </Paper>
    </Box>
  )
}
