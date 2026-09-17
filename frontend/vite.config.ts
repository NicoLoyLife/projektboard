import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Aufrufe der API gehen in der Entwicklung an das Spring-Boot-Backend
      '/api': 'http://localhost:8080',
    },
  },
})
