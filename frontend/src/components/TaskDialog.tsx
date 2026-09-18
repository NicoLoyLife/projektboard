import { Button, Dialog, DialogActions, DialogContent, DialogTitle, MenuItem, TextField } from '@mui/material'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError } from '../api/client'
import type { TaskInput } from '../api/tasks'
import type { TaskResponse, UserSummary } from '../api/types'
import { taskStatusLabels } from '../api/types'
import { ErrorAlert } from './ErrorAlert'

interface Props {
  task?: TaskResponse | null
  assignableUsers: UserSummary[]
  onClose: () => void
  onSave: (input: TaskInput) => Promise<void>
}

/** Dialog zum Anlegen und Bearbeiten einer Aufgabe, nur eingehängt solange er offen ist. */
export function TaskDialog({ task, assignableUsers, onClose, onSave }: Props) {
  const [title, setTitle] = useState(task?.title ?? '')
  const [description, setDescription] = useState(task?.description ?? '')
  const [assigneeId, setAssigneeId] = useState(task?.assignee ? String(task.assignee.id) : '')
  const [dueDate, setDueDate] = useState(task?.dueDate ?? '')
  const [error, setError] = useState<unknown>(null)
  const [saving, setSaving] = useState(false)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setSaving(true)
    setError(null)
    try {
      await onSave({
        title,
        description,
        assigneeId: assigneeId ? Number(assigneeId) : null,
        dueDate: dueDate || null,
      })
      onClose()
    } catch (caught) {
      setError(caught)
      setSaving(false)
    }
  }

  const titleError = error instanceof ApiError ? error.fieldMessage('title') : undefined
  const assigneeError = error instanceof ApiError ? error.fieldMessage('assigneeId') : undefined

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="sm">
      <form onSubmit={handleSubmit}>
        <DialogTitle>{task ? 'Aufgabe bearbeiten' : 'Aufgabe anlegen'}</DialogTitle>
        <DialogContent>
          {!titleError && !assigneeError && <ErrorAlert error={error} />}
          <TextField label="Titel" value={title} onChange={(event) => setTitle(event.target.value)}
            fullWidth margin="normal" required autoFocus
            error={Boolean(titleError)} helperText={titleError} />
          <TextField label="Beschreibung" value={description}
            onChange={(event) => setDescription(event.target.value)}
            fullWidth margin="normal" multiline minRows={2} />
          <TextField label="Zugewiesen an" value={assigneeId} select fullWidth margin="normal"
            onChange={(event) => setAssigneeId(event.target.value)}
            error={Boolean(assigneeError)} helperText={assigneeError}>
            <MenuItem value="">Niemand</MenuItem>
            {assignableUsers.map((user) => (
              <MenuItem key={user.id} value={String(user.id)}>{user.displayName}</MenuItem>
            ))}
          </TextField>
          <TextField label="Fällig am" type="date" value={dueDate}
            onChange={(event) => setDueDate(event.target.value)}
            fullWidth margin="normal" slotProps={{ inputLabel: { shrink: true } }} />
          {task && (
            <TextField label="Status" value={taskStatusLabels[task.status]} fullWidth margin="normal"
              disabled helperText="Der Status wird in der Aufgabenliste geändert." />
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
