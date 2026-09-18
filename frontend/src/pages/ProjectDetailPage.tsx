import {
  Box, Button, Chip, MenuItem, Paper, Stack, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, TextField, Typography,
} from '@mui/material'
import { useCallback, useEffect, useState } from 'react'
import { Link as RouterLink, useParams } from 'react-router'
import {
  changeProjectStatus, fetchMemberCandidates, fetchProject, setProjectMembers, updateProject,
} from '../api/projects'
import { changeTaskStatus, createTask, updateTask } from '../api/tasks'
import type { TaskInput } from '../api/tasks'
import type { ProjectDetail, TaskResponse, TaskStatus, UserSummary } from '../api/types'
import { projectStatusLabels, taskStatusLabels } from '../api/types'
import { ErrorAlert } from '../components/ErrorAlert'
import { MembersDialog } from '../components/MembersDialog'
import { ProgressBar } from '../components/ProgressBar'
import { ProjectDialog } from '../components/ProjectDialog'
import { TaskDialog } from '../components/TaskDialog'

const statusOrder: TaskStatus[] = ['OPEN', 'IN_PROGRESS', 'DONE']

function formatDate(value: string | null): string {
  return value ? new Date(value).toLocaleDateString('de-DE') : ''
}

/** Projektdetail mit Mitgliedern, Fortschritt und Aufgabenliste. */
export function ProjectDetailPage() {
  const { id } = useParams()
  const projectId = Number(id)
  const [project, setProject] = useState<ProjectDetail | null>(null)
  const [candidates, setCandidates] = useState<UserSummary[]>([])
  const [error, setError] = useState<unknown>(null)
  const [projectDialog, setProjectDialog] = useState(false)
  const [membersDialog, setMembersDialog] = useState(false)
  const [taskDialog, setTaskDialog] = useState<{ open: boolean; task: TaskResponse | null }>({ open: false, task: null })

  const load = useCallback(async () => {
    try {
      setProject(await fetchProject(projectId))
      setError(null)
    } catch (caught) {
      setError(caught)
    }
  }, [projectId])

  useEffect(() => {
    // Das Laden der Daten ist genau der Fall, für den ein Effekt gedacht ist. Der Zustand wird
    // erst nach der Antwort gesetzt, die Regel erkennt das bei einer asynchronen Funktion nicht.
    // oxlint-disable-next-line react/set-state-in-effect
    void load()
  }, [load])

  const openMembersDialog = async () => {
    try {
      setCandidates(await fetchMemberCandidates(projectId))
      setMembersDialog(true)
    } catch (caught) {
      setError(caught)
    }
  }

  const handleStatusChange = async (task: TaskResponse, status: TaskStatus) => {
    try {
      await changeTaskStatus(task.id, status)
      await load()
    } catch (caught) {
      setError(caught)
    }
  }

  const handleTaskSave = async (input: TaskInput) => {
    if (taskDialog.task) {
      await updateTask(taskDialog.task.id, input)
    } else {
      await createTask(projectId, input)
    }
    await load()
  }

  const handleArchive = async () => {
    if (!project) {
      return
    }
    try {
      setProject(await changeProjectStatus(projectId, project.status === 'ACTIVE' ? 'ARCHIVED' : 'ACTIVE'))
    } catch (caught) {
      setError(caught)
    }
  }

  if (!project) {
    return (
      <Box>
        <ErrorAlert error={error} />
        <Button component={RouterLink} to="/projects">Zurück zur Übersicht</Button>
      </Box>
    )
  }

  const assignable = [project.manager, ...project.members]

  return (
    <Box>
      <Button component={RouterLink} to="/projects" sx={{ mb: 1 }}>Zurück zur Übersicht</Button>
      <ErrorAlert error={error} />

      <Paper sx={{ p: 3, mb: 3 }}>
        <Stack direction="row" spacing={2} sx={{ mb: 1, alignItems: 'center', flexWrap: 'wrap' }}>
          <Typography variant="h5" sx={{ flexGrow: 1 }}>{project.name}</Typography>
          <Chip size="small" label={projectStatusLabels[project.status]}
            color={project.status === 'ACTIVE' ? 'primary' : 'default'}
            variant={project.status === 'ACTIVE' ? 'filled' : 'outlined'} />
          {project.canManage && project.status === 'ACTIVE' && (
            <Button size="small" onClick={() => setProjectDialog(true)}>Bearbeiten</Button>
          )}
          {project.canManage && (
            <Button size="small" onClick={handleArchive}>
              {project.status === 'ACTIVE' ? 'Archivieren' : 'Reaktivieren'}
            </Button>
          )}
        </Stack>
        {project.description && (
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>{project.description}</Typography>
        )}
        <Box sx={{ mb: 2, maxWidth: 420 }}>
          <ProgressBar percent={project.progressPercent} counts={project.counts} />
        </Box>
        <Typography variant="body2">Leitung: {project.manager.displayName}</Typography>
        <Stack direction="row" spacing={1} sx={{ mt: 1, alignItems: 'center', flexWrap: 'wrap' }}>
          <Typography variant="body2">
            Mitglieder: {project.members.length > 0
              ? project.members.map((member) => member.displayName).join(', ')
              : 'keine'}
          </Typography>
          {project.canManage && project.status === 'ACTIVE' && (
            <Button size="small" onClick={openMembersDialog}>Mitglieder verwalten</Button>
          )}
        </Stack>
      </Paper>

      <Stack direction="row" spacing={2} sx={{ mb: 1, alignItems: 'center' }}>
        <Typography variant="h6" sx={{ flexGrow: 1 }}>Aufgaben</Typography>
        {project.canEditTasks && (
          <Button variant="contained" onClick={() => setTaskDialog({ open: true, task: null })}>
            Neue Aufgabe
          </Button>
        )}
      </Stack>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Titel</TableCell>
              <TableCell>Zugewiesen an</TableCell>
              <TableCell>Fällig am</TableCell>
              <TableCell sx={{ width: 190 }}>Status</TableCell>
              <TableCell align="right">Aktion</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {project.tasks.map((task) => (
              <TableRow key={task.id} hover>
                <TableCell>{task.title}</TableCell>
                <TableCell>{task.assignee?.displayName ?? ''}</TableCell>
                <TableCell>{formatDate(task.dueDate)}</TableCell>
                <TableCell>
                  {project.canEditTasks ? (
                    <TextField select size="small" fullWidth value={task.status}
                      onChange={(event) => handleStatusChange(task, event.target.value as TaskStatus)}>
                      {statusOrder.map((status) => (
                        <MenuItem key={status} value={status}>{taskStatusLabels[status]}</MenuItem>
                      ))}
                    </TextField>
                  ) : (
                    <Typography variant="body2">{taskStatusLabels[task.status]}</Typography>
                  )}
                </TableCell>
                <TableCell align="right">
                  {project.canEditTasks && (
                    <Button size="small" onClick={() => setTaskDialog({ open: true, task })}>Bearbeiten</Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
            {project.tasks.length === 0 && (
              <TableRow>
                <TableCell colSpan={5}>
                  <Typography variant="body2" color="text.secondary" sx={{ py: 2 }}>
                    Dieses Projekt hat noch keine Aufgaben.
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {projectDialog && (
        <ProjectDialog project={project} onClose={() => setProjectDialog(false)}
          onSave={async (name, description) => {
            setProject(await updateProject(projectId, name, description))
          }} />
      )}
      {membersDialog && (
        <MembersDialog candidates={candidates} selected={project.members}
          onClose={() => setMembersDialog(false)}
          onSave={async (userIds) => {
            setProject(await setProjectMembers(projectId, userIds))
          }} />
      )}
      {taskDialog.open && (
        <TaskDialog task={taskDialog.task} assignableUsers={assignable}
          onClose={() => setTaskDialog({ open: false, task: null })} onSave={handleTaskSave} />
      )}
    </Box>
  )
}
