import type { ErrorResponse, FieldError } from './types'

/** Fehler der API mit Status, Fehlerkennung und optionalen Feldfehlern. */
export class ApiError extends Error {
  readonly status: number
  readonly error: string
  readonly errors: FieldError[]

  constructor(status: number, body: ErrorResponse | undefined) {
    super(body?.message ?? 'Die Anfrage ist fehlgeschlagen')
    this.status = status
    this.error = body?.error ?? 'unknown'
    this.errors = body?.errors ?? []
  }

  /** Meldung zu einem Feld, falls das Backend eine geliefert hat. */
  fieldMessage(field: string): string | undefined {
    return this.errors.find((entry) => entry.field === field)?.message
  }
}

let unauthorizedHandler: (() => void) | null = null

/** Wird vom Sitzungszustand gesetzt, damit ein 401 aus jedem Aufruf zur Abmeldung führt. */
export function setUnauthorizedHandler(handler: (() => void) | null) {
  unauthorizedHandler = handler
}

function readCookie(name: string): string | null {
  const prefix = name + '='
  const entry = document.cookie.split('; ').find((part) => part.startsWith(prefix))
  return entry ? decodeURIComponent(entry.substring(prefix.length)) : null
}

/** Holt ein CSRF-Token vom Backend. Nötig beim Start sowie nach Anmeldung und Abmeldung. */
export async function refreshCsrfToken(): Promise<void> {
  await fetch('/api/auth/csrf', { credentials: 'same-origin' })
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: unknown
  /** Ein 401 bei dieser Anfrage soll nicht zur Abmeldung führen, etwa bei der Anmeldung selbst. */
  ignoreUnauthorized?: boolean
}

/** Führt einen JSON-Aufruf gegen die API aus und wandelt Fehler in ApiError um. */
export async function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const method = options.method ?? 'GET'
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (method !== 'GET') {
    if (!readCookie('XSRF-TOKEN')) {
      await refreshCsrfToken()
    }
    const token = readCookie('XSRF-TOKEN')
    if (token) {
      headers['X-XSRF-TOKEN'] = token
    }
  }
  const response = await fetch(path, {
    method,
    headers,
    credentials: 'same-origin',
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  })
  const text = await response.text()
  const data = text ? (JSON.parse(text) as unknown) : undefined
  if (!response.ok) {
    if (response.status === 401 && !options.ignoreUnauthorized) {
      unauthorizedHandler?.()
    }
    throw new ApiError(response.status, data as ErrorResponse | undefined)
  }
  return data as T
}
