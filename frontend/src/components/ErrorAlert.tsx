import { Alert } from '@mui/material'
import { ApiError } from '../api/client'

/** Zeigt eine Fehlermeldung der API. Feldfehler werden zusätzlich aufgelistet. */
export function ErrorAlert({ error }: { error: unknown }) {
  if (!error) {
    return null
  }
  if (error instanceof ApiError && error.errors.length > 0) {
    return (
      <Alert severity="error" sx={{ mb: 2 }}>
        {error.errors.map((entry) => `${entry.field}: ${entry.message}`).join(', ')}
      </Alert>
    )
  }
  const message = error instanceof Error ? error.message : 'Ein unerwarteter Fehler ist aufgetreten'
  return <Alert severity="error" sx={{ mb: 2 }}>{message}</Alert>
}
