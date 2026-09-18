import { Box, LinearProgress, Typography } from '@mui/material'
import type { TaskCounts } from '../api/types'

/** Fortschrittsbalken mit Prozentwert und Anzahl der erledigten Aufgaben. */
export function ProgressBar({ percent, counts }: { percent: number; counts?: TaskCounts }) {
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, minWidth: 180 }}>
      <LinearProgress variant="determinate" value={percent}
        sx={{ flexGrow: 1, height: 8, borderRadius: 1 }}
        aria-label={`Fortschritt ${percent} Prozent`} />
      <Typography variant="body2" sx={{ minWidth: 40, textAlign: 'right' }}>{percent} %</Typography>
      {counts && (
        <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'nowrap' }}>
          {counts.done} von {counts.total}
        </Typography>
      )}
    </Box>
  )
}
