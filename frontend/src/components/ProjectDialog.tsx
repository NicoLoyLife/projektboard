import { Button, Dialog, DialogActions, DialogContent, DialogTitle, TextField } from '@mui/material'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError } from '../api/client'
import type { ProjectDetail } from '../api/types'
import { ErrorAlert } from './ErrorAlert'

interface Props {
  project?: ProjectDetail | null
  onClose: () => void
  onSave: (name: string, description: string) => Promise<void>
}

/**
 * Dialog zum Anlegen und Bearbeiten eines Projekts. Er wird nur eingehängt, solange er offen ist,
 * damit die Felder bei jedem Öffnen frisch aus dem Projekt gefüllt werden.
 */
export function ProjectDialog({ project, onClose, onSave }: Props) {
  const [name, setName] = useState(project?.name ?? '')
  const [description, setDescription] = useState(project?.description ?? '')
  const [error, setError] = useState<unknown>(null)
  const [saving, setSaving] = useState(false)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setSaving(true)
    setError(null)
    try {
      await onSave(name, description)
      onClose()
    } catch (caught) {
      setError(caught)
      setSaving(false)
    }
  }

  const fieldError = error instanceof ApiError ? error.fieldMessage('name') : undefined

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="sm">
      <form onSubmit={handleSubmit}>
        <DialogTitle>{project ? 'Projekt bearbeiten' : 'Projekt anlegen'}</DialogTitle>
        <DialogContent>
          {!fieldError && <ErrorAlert error={error} />}
          <TextField label="Name" value={name} onChange={(event) => setName(event.target.value)}
            fullWidth margin="normal" required autoFocus
            error={Boolean(fieldError)} helperText={fieldError} />
          <TextField label="Beschreibung" value={description}
            onChange={(event) => setDescription(event.target.value)}
            fullWidth margin="normal" multiline minRows={3} />
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Abbrechen</Button>
          <Button type="submit" variant="contained" disabled={saving}>Speichern</Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}
