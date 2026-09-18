import {
  Box, Button, Chip, FormControlLabel, Paper, Switch, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, Typography,
} from '@mui/material'
import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import { createProject, fetchProjects } from '../api/projects'
import type { ProjectSummary } from '../api/types'
import { projectStatusLabels } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { ErrorAlert } from '../components/ErrorAlert'
import { ProgressBar } from '../components/ProgressBar'
import { ProjectDialog } from '../components/ProjectDialog'

/** Übersicht der sichtbaren Projekte mit Fortschritt. */
export function ProjectListPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [projects, setProjects] = useState<ProjectSummary[]>([])
  const [showArchived, setShowArchived] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const [loading, setLoading] = useState(true)
  const [dialogOpen, setDialogOpen] = useState(false)

  const load = useCallback(async () => {
    try {
      setProjects(await fetchProjects())
      setError(null)
    } catch (caught) {
      setError(caught)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    // Das Laden der Daten ist genau der Fall, für den ein Effekt gedacht ist. Der Zustand wird
    // erst nach der Antwort gesetzt, die Regel erkennt das bei einer asynchronen Funktion nicht.
    // oxlint-disable-next-line react/set-state-in-effect
    void load()
  }, [load])

  const handleCreate = async (name: string, description: string) => {
    const created = await createProject(name, description)
    navigate(`/projects/${created.id}`)
  }

  const visible = projects.filter((project) => showArchived || project.status === 'ACTIVE')

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 2, gap: 2, flexWrap: 'wrap' }}>
        <Typography variant="h5" sx={{ flexGrow: 1 }}>Projekte</Typography>
        <FormControlLabel
          control={<Switch checked={showArchived} onChange={(event) => setShowArchived(event.target.checked)} />}
          label="Archivierte anzeigen" />
        {user?.role === 'PROJECT_MANAGER' && (
          <Button variant="contained" onClick={() => setDialogOpen(true)}>Neues Projekt</Button>
        )}
      </Box>

      <ErrorAlert error={error} />

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Name</TableCell>
              <TableCell>Leitung</TableCell>
              <TableCell align="right">Aufgaben</TableCell>
              <TableCell>Fortschritt</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Aktion</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {visible.map((project) => (
              <TableRow key={project.id} hover>
                <TableCell>{project.name}</TableCell>
                <TableCell>{project.manager.displayName}</TableCell>
                <TableCell align="right">{project.counts.total}</TableCell>
                <TableCell sx={{ width: 260 }}>
                  <ProgressBar percent={project.progressPercent} counts={project.counts} />
                </TableCell>
                <TableCell>
                  <Chip size="small" label={projectStatusLabels[project.status]}
                    color={project.status === 'ACTIVE' ? 'primary' : 'default'}
                    variant={project.status === 'ACTIVE' ? 'filled' : 'outlined'} />
                </TableCell>
                <TableCell align="right">
                  <Button size="small" onClick={() => navigate(`/projects/${project.id}`)}>Öffnen</Button>
                </TableCell>
              </TableRow>
            ))}
            {!loading && visible.length === 0 && (
              <TableRow>
                <TableCell colSpan={6}>
                  <Typography variant="body2" color="text.secondary" sx={{ py: 2 }}>
                    Es sind keine Projekte vorhanden, für die Sie berechtigt sind.
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {dialogOpen && <ProjectDialog onClose={() => setDialogOpen(false)} onSave={handleCreate} />}
    </Box>
  )
}
