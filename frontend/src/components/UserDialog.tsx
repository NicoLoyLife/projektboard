import {
  Button, Checkbox, Dialog, DialogActions, DialogContent, DialogTitle,
  FormControlLabel, MenuItem, TextField,
} from '@mui/material'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError } from '../api/client'
import type { Role, UserResponse } from '../api/types'
import { roleLabels } from '../api/types'
import { ErrorAlert } from './ErrorAlert'

export interface UserFormValues {
  username: string
  password: string
  displayName: string
  role: Role
  active: boolean
}

interface Props {
  user?: UserResponse | null
  onClose: () => void
  onSave: (values: UserFormValues) => Promise<void>
}

const roles: Role[] = ['ADMIN', 'PROJECT_MANAGER', 'EMPLOYEE']

/**
 * Dialog zum Anlegen und Bearbeiten eines Benutzers. Beim Bearbeiten bleibt der Benutzername
 * unverändert, ein Passwort wird nur gesetzt, wenn eines eingegeben wurde.
 */
export function UserDialog({ user, onClose, onSave }: Props) {
  const [username, setUsername] = useState(user?.username ?? '')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState(user?.displayName ?? '')
  const [role, setRole] = useState<Role>(user?.role ?? 'EMPLOYEE')
  const [active, setActive] = useState(user?.active ?? true)
  const [error, setError] = useState<unknown>(null)
  const [saving, setSaving] = useState(false)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setSaving(true)
    setError(null)
    try {
      await onSave({ username, password, displayName, role, active })
      onClose()
    } catch (caught) {
      setError(caught)
      setSaving(false)
    }
  }

  const fieldError = (field: string) =>
    error instanceof ApiError ? error.fieldMessage(field) : undefined
  const hasFieldErrors = error instanceof ApiError && error.errors.length > 0

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="sm">
      <form onSubmit={handleSubmit}>
        <DialogTitle>{user ? 'Benutzer bearbeiten' : 'Benutzer anlegen'}</DialogTitle>
        <DialogContent>
          {!hasFieldErrors && <ErrorAlert error={error} />}
          <TextField label="Benutzername" value={username}
            onChange={(event) => setUsername(event.target.value)}
            fullWidth margin="normal" required autoFocus={!user} disabled={Boolean(user)}
            error={Boolean(fieldError('username'))}
            helperText={fieldError('username') ?? (user ? 'Der Benutzername kann nicht geändert werden.' : '')} />
          <TextField label="Anzeigename" value={displayName}
            onChange={(event) => setDisplayName(event.target.value)}
            fullWidth margin="normal" required autoFocus={Boolean(user)}
            error={Boolean(fieldError('displayName'))} helperText={fieldError('displayName')} />
          <TextField label={user ? 'Neues Passwort' : 'Passwort'} type="password" value={password}
            onChange={(event) => setPassword(event.target.value)}
            fullWidth margin="normal" required={!user} autoComplete="new-password"
            error={Boolean(fieldError('password'))}
            helperText={fieldError('password') ?? (user ? 'Leer lassen, um das Passwort beizubehalten.' : 'Mindestens 8 Zeichen.')} />
          <TextField label="Rolle" value={role} select fullWidth margin="normal"
            onChange={(event) => setRole(event.target.value as Role)}
            error={Boolean(fieldError('role'))} helperText={fieldError('role')}>
            {roles.map((entry) => (
              <MenuItem key={entry} value={entry}>{roleLabels[entry]}</MenuItem>
            ))}
          </TextField>
          {user && (
            <FormControlLabel sx={{ mt: 1 }}
              control={<Checkbox checked={active} onChange={(event) => setActive(event.target.checked)} />}
              label="Aktiv" />
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Abbrechen</Button>
          <Button type="submit" variant="contained" disabled={saving}>Speichern</Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}
