import { Navigate, Route, Routes } from 'react-router'
import { RequireAuth } from './auth/RequireAuth'
import { AppShell } from './components/AppShell'
import { LoginPage } from './pages/LoginPage'
import { ProjectDetailPage } from './pages/ProjectDetailPage'
import { ProjectListPage } from './pages/ProjectListPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<AppShell />}>
          <Route path="/projects" element={<ProjectListPage />} />
          <Route path="/projects/:id" element={<ProjectDetailPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/projects" replace />} />
    </Routes>
  )
}
