import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

/** 开发时浏览器访问 5173，接口与上传静态资源走本地 Spring Boot :8080 */
const proxy = {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
  },
  '/uploads': {
    target: 'http://localhost:8080',
    changeOrigin: true,
  },
}

export default defineConfig({
  /** 与 Spring Boot `classpath:/static/` 根路径一致，部署在 http://localhost:8080/ */
  base: '/',
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    strictPort: true,
    host: true,
    proxy,
  },
  preview: {
    port: 5173,
    strictPort: true,
    host: true,
    proxy,
  },
})
