import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 本地开发时将同源 API 请求转发到 Spring Boot，避免页面直连后端产生跨域错误。
const LOCAL_API_PROXY_TARGET = 'http://localhost:8080'

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: LOCAL_API_PROXY_TARGET,
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('proxyReq', (request) => {
            // 浏览器到 Vite 已是同源请求，转发至后端时不携带开发端 Origin，避免触发生产 CORS 白名单。
            request.removeHeader('origin')
          })
        },
      },
    },
  },
})
