import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
// API proxy target is configurable via VITE_API_TARGET env var:
//   - Docker (default): http://fraudshield-backend:8080
//   - Local dev (no Docker): set VITE_API_TARGET=http://localhost:8080
export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0', // Expose to all network interfaces for Docker
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_API_TARGET || 'http://fraudshield-backend:8080',
        changeOrigin: true
      }
    }
  }
})
