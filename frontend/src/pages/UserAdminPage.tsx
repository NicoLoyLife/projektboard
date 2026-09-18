import {
  Box, Button, Chip, Paper, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Typography,
} from '@mui/material'
import { useCallback, useEffect, useState } from 'react'
import { createUser, fetchUsers, updateUser } from '../api/users'
import type { UserResponse } from '../api/types'
import { roleLabels } from '../api/types'
import { ErrorAlert } from '../components/ErrorAlert'
import { UserDialog } from '../components/UserDialog'
import type { UserFormValues } from '../components/UserDialog'

/** Benutzerverwaltung, nur für die Administration erreichbar. */
export function UserAdminPage() {
  const [users, setUsers] = useState<UserResponse[]>([])
  const [error, setError] = useState<unknown>(null)
  const [dialog, setDialog] = useState<{ open: boolean; user: UserResponse | null }>({ open: false, user: null })

  const load = useCallback(async () => {
    try {
      setUsers(await fetchUsers())
      setError(null)
    } catch (caught) {
      setError(caught)
    }
  }, [])

  useEffect(() => {
    // Das Laden der Daten ist genau der Fall, für den ein Effekt gedacht ist. Der Zustand wird
    // erst nach der Antwort gesetzt, die Regel erkennt das bei einer asynchronen Funktion nicht.
    // oxlint-disable-next-line react/set-state-in-effect
    void load()
  }, [load])

  const handleSave = async (values: UserFormValues) => {
    if (dialog.user) {
      await updateUser(dialog.user.id, {
        displayName: values.displayName,
        role: values.role,
        active: values.active,
        password: values.password ? values.password : null,
      })
    } else {
      await createUser({
        username: values.username.trim(),
        password: values.password,
        displayName: values.displayName,
        role: values.role,
      })
    }
    await load()
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 2, gap: 2 }}>
        <Typography variant="h5" sx={{ flexGrow: 1 }}>Benutzer</Typography>
        <Button variant="contained" onClick={() => setDialog({ open: true, user: null })}>
          Neuer Benutzer
        </Button>
      </Box>

      <ErrorAlert error={error} />

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Benutzername</TableCell>
              <TableCell>Anzeigename</TableCell>
              <TableCell>Rolle</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Aktion</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {users.map((user) => (
              <TableRow key={user.id} hover>
                <TableCell>{user.username}</TableCell>
                <TableCell>{user.displayName}</TableCell>
                <TableCell>{roleLabels[user.role]}</TableCell>
                <TableCell>
                  <Chip size="small" label={user.active ? 'aktiv' : 'deaktiviert'}
                    color={user.active ? 'primary' : 'default'}
                    variant={user.active ? 'filled' : 'outlined'} />
                </TableCell>
                <TableCell align="right">
                  <Button size="small" onClick={() => setDialog({ open: true, user })}>Bearbeiten</Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
        Benutzer werden deaktiviert statt gelöscht, damit ihre Zuordnungen in Projekten und Aufgaben erhalten bleiben.
      </Typography>

      {dialog.open && (
        <UserDialog user={dialog.user} onClose={() => setDialog({ open: false, user: null })} onSave={handleSave} />
      )}
    </Box>
  )
}
