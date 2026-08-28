import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
    extensions: ['.ts', '.js', '.vue', '.json'],
  },
  server: {
    port: 5177,
    proxy: {
      '/race-api': {
                target: 'http://localhost:8084',
        changeOrigin: true,
      },
    },
  },
})
