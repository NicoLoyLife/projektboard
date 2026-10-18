import { createTheme } from '@mui/material'
import { deDE } from '@mui/material/locale'
import { alpha } from '@mui/material/styles'
import type { Shadows } from '@mui/material/styles'
import type { TaskStatus } from './api/types'

// Kühle Neutraltöne mit leichter Petrol-Tönung und ein Akzent für Aktion, Auswahl, Fokus und Fortschritt.
// Kontraste auf Weiß: Tinte 16:1, gedämpfte Tinte 6,8:1, Akzent 6,6:1, Rahmen der Eingabefelder 3,7:1.
const tone = {
  ground: '#f1f5f6',
  groundDeep: '#e8eff1',
  layer: '#f7fafa',
  line: '#d6dee0',
  rule: '#e1e8e9',
  chipLine: '#c2cdcf',
  control: '#7a878a',
  controlHover: '#3c4b4e',
  ink: '#172426',
  inkMuted: '#505e60',
  inkDisabled: '#647174',
  accent: '#006775',
  accentDark: '#00515e',
  accentActive: '#00434e',
  accentTrack: '#c1e0e6',
  selection: '#b6e0e9',
  error: '#b6322d',
  errorDark: '#89211e',
  errorLight: '#d7695f',
  errorTint: '#ffedeb',
  errorLine: '#f4c8c3',
}

// Flache Flächen: Schatten nur für schwebende Ebenen, Menüs ab Stufe 8, Dialoge ab Stufe 16.
const menuShadow = `0 6px 16px -4px ${alpha(tone.ink, 0.16)}, 0 1px 3px ${alpha(tone.ink, 0.08)}`
const dialogShadow = `0 16px 40px -12px ${alpha(tone.ink, 0.28)}, 0 2px 6px ${alpha(tone.ink, 0.08)}`
const shadows = Array.from({ length: 25 }, (_, level) =>
  level < 8 ? 'none' : level < 16 ? menuShadow : dialogShadow) as Shadows

// Teilt den Fortschrittsbalken mit 2-px-Fugen in zehn gleiche Abschnitte.
const tenths = 'repeating-linear-gradient(to right, transparent 0 1px, #000 1px calc(10% - 1px), transparent calc(10% - 1px) 10%)'

// Der Aufgabenstatus erscheint als Abschnitt derselben Skala: offen leer, in Bearbeitung halb, erledigt voll.
const statusFill: Record<TaskStatus, string> = {
  OPEN: tone.accentTrack,
  IN_PROGRESS: `linear-gradient(to right, ${tone.accent} 50%, ${tone.accentTrack} 50%)`,
  DONE: tone.accent,
}
const statusMark = (fill: string) => ({
  content: '""',
  display: 'inline-block',
  width: 10,
  height: 10,
  marginRight: 8,
  borderRadius: 2,
  background: fill,
  boxShadow: `inset 0 0 0 1px ${alpha(tone.accent, 0.35)}`,
  verticalAlign: '-1px',
})
// Gilt für jedes Auswahlfeld in einer Tabelle und jeden Menüeintrag mit einem dieser Werte. Die Auswahl
// hält ihren Wert im versteckten Eingabefeld, die Menüeinträge tragen ihn als data-value.
const selectStatusMarks = Object.fromEntries(Object.entries(statusFill).map(([status, fill]) =>
  [`.MuiTableCell-body &:has(> input.MuiSelect-nativeInput[value="${status}"]) .MuiSelect-select::before`, statusMark(fill)]))
const menuStatusMarks = Object.fromEntries(Object.entries(statusFill).map(([status, fill]) =>
  [`&[data-value="${status}"]::before`, statusMark(fill)]))

export const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: tone.accent, dark: tone.accentDark, contrastText: '#ffffff' },
    // Wird von keiner Komponente genutzt und ersetzt nur das Lila des Standardthemes
    secondary: { main: tone.controlHover, contrastText: '#ffffff' },
    error: { main: tone.error, dark: tone.errorDark, light: tone.errorLight, contrastText: '#ffffff' },
    text: { primary: tone.ink, secondary: tone.inkMuted, disabled: tone.inkDisabled },
    divider: tone.line,
    background: { default: tone.ground, paper: '#ffffff' },
    grey: {
      50: tone.ground, 100: tone.groundDeep, 200: '#dce5e6', 300: '#ccd7d8', 400: '#aebabc',
      500: '#7c898b', 600: '#5d6c6f', 700: '#425052', 800: '#283538', 900: tone.ink,
    },
    action: {
      hover: alpha(tone.accent, 0.06),
      hoverOpacity: 0.06,
      selected: alpha(tone.accent, 0.1),
      selectedOpacity: 0.1,
      disabled: alpha(tone.ink, 0.38),
      disabledBackground: alpha(tone.ink, 0.08),
    },
  },
  typography: {
    fontFamily: '"Source Sans 3 Variable", "Source Sans 3", "Segoe UI", system-ui, sans-serif',
    h5: { fontSize: '1.5rem', fontWeight: 600, lineHeight: 1.25, letterSpacing: '-0.01em' },
    h6: { fontSize: '1.125rem', fontWeight: 600, lineHeight: 1.35, letterSpacing: '-0.005em' },
    body1: { fontSize: '1rem', lineHeight: 1.5 },
    body2: { fontSize: '0.9375rem', lineHeight: 1.45 },
    button: { fontSize: '0.9375rem', fontWeight: 600, lineHeight: 1.6, letterSpacing: 0, textTransform: 'none' },
    caption: { fontSize: '0.8125rem', lineHeight: 1.4 },
  },
  shape: { borderRadius: 6 },
  shadows,
  mixins: { toolbar: { minHeight: 56 } },
  focusVisible: true,
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        html: { colorScheme: 'light', accentColor: tone.accent },
        body: { fontVariantNumeric: 'tabular-nums' },
        '::selection': { backgroundColor: tone.selection, color: tone.ink },
        'input, textarea': { caretColor: tone.accent },
        // Ein leeres Datumsfeld zeigt nur das Format und soll nicht wie ein eingetragener Wert wirken
        'input[type="date"][value=""]': { color: tone.inkMuted },
        // Links außerhalb der MUI-Schaltflächen, etwa der Produktname, bekommen denselben Fokusring
        'a:not(.MuiButtonBase-root):focus-visible': { outline: `2px solid ${tone.accent}`, outlineOffset: 2, borderRadius: 2 },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: { border: `1px solid ${tone.line}` },
        rounded: { borderRadius: 8 },
      },
    },
    MuiAppBar: {
      // Variante default, weil MUI um den Fokusring in farbigen Leisten einen Ring in der Grundfarbe legt
      defaultProps: { elevation: 0, color: 'default' },
      styleOverrides: {
        root: {
          '--AppBar-background': '#ffffff',
          '--AppBar-color': tone.ink,
          border: 0,
          borderBottom: `1px solid ${tone.line}`,
          '& .MuiTypography-h6': { fontWeight: 700, letterSpacing: '-0.01em' },
          '& .MuiTypography-body2': { color: tone.inkMuted },
          '& .MuiButton-root': { whiteSpace: 'nowrap', flexShrink: 0 },
          // Abmelden ist die einzige Schaltfläche, die Navigation besteht aus Links
          '& button.MuiButton-root': { fontWeight: 500, color: tone.inkMuted, '&:hover': { color: tone.ink } },
        },
      },
    },
    MuiToolbar: {
      styleOverrides: {
        root: ({ theme }) => ({
          // Die Leiste fluchtet mit dem Seiteninhalt
          width: '100%',
          maxWidth: theme.breakpoints.values.lg,
          marginInline: 'auto',
          // Auf schmalen Bildschirmen bricht sie um, damit Abmelden erreichbar bleibt
          [theme.breakpoints.down('sm')]: { flexWrap: 'wrap', paddingTop: 8, paddingBottom: 8 },
        }),
      },
    },
    MuiStack: {
      // Abstand über gap, damit umbrechende Zeilen links bündig bleiben
      defaultProps: { useFlexGap: true },
    },
    MuiButtonBase: {
      defaultProps: { disableRipple: true },
    },
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: {
          padding: '6px 14px',
          variants: [
            { props: { variant: 'text' }, style: { padding: '6px 8px' } },
            { props: { size: 'small' }, style: { padding: '3px 8px', fontSize: '0.875rem' } },
            { props: { variant: 'contained', color: 'primary' }, style: { '&:active': { backgroundColor: tone.accentActive } } },
            { props: { variant: 'text', color: 'primary' }, style: { '&:active': { backgroundColor: alpha(tone.accent, 0.14) } } },
          ],
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        // Status als rechteckiges Etikett. Der Normalfall „aktiv“ bleibt neutral,
        // archivierte und deaktivierte Einträge heben sich grau und gestrichelt ab.
        root: {
          borderRadius: 4,
          fontWeight: 600,
          letterSpacing: '0.01em',
          variants: [
            {
              props: { variant: 'filled', color: 'primary' },
              style: { backgroundColor: '#ffffff', color: tone.ink, boxShadow: `inset 0 0 0 1px ${tone.chipLine}` },
            },
            {
              props: { variant: 'outlined', color: 'default' },
              style: { backgroundColor: tone.groundDeep, color: tone.inkMuted, borderStyle: 'dashed', borderColor: tone.control },
            },
          ],
        },
        sizeSmall: { height: 22 },
      },
    },
    MuiLinearProgress: {
      styleOverrides: {
        root: {
          variants: [
            {
              props: { variant: 'determinate' },
              style: {
                // Doppelte Klasse, weil die Komponente den Radius über sx setzt
                '&&': { borderRadius: 2 },
                maskImage: tenths,
              },
            },
          ],
        },
        colorPrimary: { backgroundColor: tone.accentTrack },
      },
    },
    MuiTableContainer: {
      styleOverrides: {
        // Auf schmalen Bildschirmen zeigt ein Schatten am rechten Rand, dass die Tabelle weiterläuft
        root: ({ theme }) => ({
          [theme.breakpoints.down('sm')]: {
            backgroundImage: `linear-gradient(to left, #ffffff 16px, ${alpha('#ffffff', 0)}), `
              + `radial-gradient(farthest-side at 100% 50%, ${alpha(tone.ink, 0.18)}, ${alpha(tone.ink, 0)})`,
            backgroundPosition: 'right center, right center',
            backgroundSize: '40px 100%, 14px 100%',
            backgroundRepeat: 'no-repeat',
            backgroundAttachment: 'local, scroll',
          },
        }),
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: ({ theme }) => ({
          padding: '10px 16px',
          borderBottom: `1px solid ${tone.rule}`,
          [theme.breakpoints.down('sm')]: {
            padding: '8px 10px',
            // Die erste Spalte bleibt beim seitlichen Wischen stehen
            '&:first-of-type': {
              position: 'sticky',
              left: 0,
              zIndex: 1,
              backgroundColor: '#ffffff',
              boxShadow: `inset -1px 0 0 ${tone.line}`,
            },
            // Die Zeile für leere Listen überspannt alle Spalten und bleibt normal
            '&[colspan]': { position: 'static', boxShadow: 'none' },
            '.MuiTableRow-hover:hover > &:first-of-type': {
              backgroundImage: `linear-gradient(${theme.palette.action.hover}, ${theme.palette.action.hover})`,
            },
          },
        }),
        head: ({ theme }) => ({
          backgroundColor: tone.layer,
          color: tone.inkMuted,
          fontSize: '0.8125rem',
          fontWeight: 600,
          lineHeight: 1.4,
          letterSpacing: '0.01em',
          paddingTop: 9,
          paddingBottom: 9,
          borderBottomColor: tone.line,
          [theme.breakpoints.down('sm')]: {
            '&:first-of-type': { backgroundColor: tone.layer, zIndex: 2 },
          },
        }),
        body: {
          color: tone.ink,
          '& .MuiInputBase-sizeSmall .MuiSelect-select': { paddingTop: 5, paddingBottom: 5 },
        },
      },
    },
    MuiTableRow: {
      styleOverrides: {
        root: { '&:last-of-type > .MuiTableCell-body': { borderBottom: 0 } },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          backgroundColor: '#ffffff',
          '&:hover:not(.Mui-focused):not(.Mui-error):not(.Mui-disabled) .MuiOutlinedInput-notchedOutline': {
            borderColor: tone.controlHover,
          },
          '&.Mui-disabled': { backgroundColor: tone.layer },
          ...selectStatusMarks,
        },
        notchedOutline: { borderColor: tone.control },
      },
    },
    MuiMenuItem: {
      styleOverrides: {
        root: { ...menuStatusMarks },
      },
    },
    MuiFormHelperText: {
      styleOverrides: {
        root: { marginTop: 4 },
      },
    },
    MuiSwitch: {
      styleOverrides: {
        switchBase: {
          '&.Mui-checked': { color: '#ffffff' },
          '&.Mui-checked + .MuiSwitch-track': { backgroundColor: tone.accent, opacity: 1 },
        },
        track: { backgroundColor: tone.control, opacity: 1 },
        thumb: { boxShadow: `0 1px 2px ${alpha(tone.ink, 0.3)}` },
      },
    },
    MuiAlert: {
      styleOverrides: {
        root: {
          borderRadius: 6,
          variants: [
            {
              props: { variant: 'standard', severity: 'error' },
              style: {
                backgroundColor: tone.errorTint,
                color: tone.errorDark,
                borderColor: tone.errorLine,
                '& .MuiAlert-icon': { color: tone.error },
              },
            },
          ],
        },
      },
    },
    MuiDialog: {
      styleOverrides: {
        root: { '& .MuiBackdrop-root': { backgroundColor: alpha(tone.ink, 0.42) } },
        paper: { borderRadius: 10 },
      },
    },
    MuiDialogTitle: {
      styleOverrides: {
        root: { padding: '20px 24px 6px' },
      },
    },
    MuiDialogActions: {
      styleOverrides: {
        root: { padding: '8px 24px 20px' },
      },
    },
  },
}, deDE)
