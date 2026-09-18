import { createContext, useContext } from 'react'
import type { Me } from '../api/types'

export interface AuthState {
  user: Me | null
  /** Solange die Sitzung beim Start geprüft wird. */
  loading: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthState | null>(null)

export function useAuth(): AuthState {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth kann nur innerhalb von AuthProvider verwendet werden')
  }
  return context
}
