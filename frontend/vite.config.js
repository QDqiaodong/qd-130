import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: Number(process.env.FRONTEND_PORT || 8230),
    host: '127.0.0.1',
    strictPort: true,
    proxy: {
      '/api': {
        target: `http://localhost:${process.env.BACKEND_PORT || 8330}`,
        changeOrigin: true
      }
    }
  },
  preview: {
    host: '127.0.0.1',
    port: Number(process.env.FRONTEND_PORT || 8230),
    strictPort: true
  }
})
