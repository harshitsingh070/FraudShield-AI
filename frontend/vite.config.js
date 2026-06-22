import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0', // Expose to all network interfaces for Docker
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://fraudshield-backend:8080',
        changeOrigin: true
      }
    }
  }
})
