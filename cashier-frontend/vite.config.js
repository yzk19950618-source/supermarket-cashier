import { defineConfig } from 'vite'

// Serves existing ./dist (production build). /api proxied to Spring Boot on 8080.
export default defineConfig({
  preview: {
    host: true,
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
