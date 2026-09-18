import {
  Button, Checkbox, Dialog, DialogActions, DialogContent, DialogTitle,
  FormControlLabel, FormGroup, Typography,
} from '@mui/material'
import { useState } from 'react'
import type { UserSummary } from '../api/types'
import { ErrorAlert } from './ErrorAlert'

interface Props {
  candidates: UserSummary[]
  selected: UserSummary[]
  onClose: () => void
  onSave: (userIds: number[]) => Promise<void>
}

/** Dialog zur Pflege der Mitgliederliste, nur eingehängt solange er offen ist. */
export function MembersDialog({ candidates, selected, onClose, onSave }: Props) {
  const [checked, setChecked] = useState<number[]>(() => selected.map((user) => user.id))
  const [error, setError] = useState<unknown>(null)
  const [saving, setSaving] = useState(false)

  const toggle = (id: number) => {
    setChecked((current) =>
      current.includes(id) ? current.filter((entry) => entry !== id) : [...current, id])
  }

  const handleSave = async () => {
    setSaving(true)
    setError(null)
    try {
      await onSave(checked)
      onClose()
    } catch (caught) {
      setError(caught)
      setSaving(false)
    }
  }

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle>Mitglieder verwalten</DialogTitle>
      <DialogContent>
        <ErrorAlert error={error} />
        {candidates.length === 0 ? (
          <Typography variant="body2" color="text.secondary">
            Es stehen keine weiteren Benutzer zur Auswahl.
          </Typography>
        ) : (
          <FormGroup>
            {candidates.map((user) => (
              <FormControlLabel key={user.id}
                control={<Checkbox checked={checked.includes(user.id)} onChange={() => toggle(user.id)} />}
                label={user.displayName} />
            ))}
          </FormGroup>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Abbrechen</Button>
        <Button onClick={handleSave} variant="contained" disabled={saving}>Speichern</Button>
      </DialogActions>
    </Dialog>
  )
}
