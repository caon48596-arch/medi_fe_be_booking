import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/user': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/doctor': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/doctor-slots': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/appointment': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/medical-records': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
