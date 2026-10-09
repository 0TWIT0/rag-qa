import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// 这个工程有两种跑法，配置分别对应：
//
// 【开发】server.proxy
//   前端在 5173，后端在 8080。浏览器直接请求 8080 会撞上跨域（CORS），
//   所以把 /api 开头的请求代理到后端——浏览器看来始终同源，
//   后端一行 CORS 配置都不用加。
//
// 【打包】build.outDir
//   构建产物直接写进 Spring Boot 的静态资源目录。这样 mvnw package 打出的
//   jar 自带前端界面，一条 java -jar 就能跑起完整应用（见 README「打包与部署」）。
//   此时前后端同源，前端里的 /api 相对路径直接生效，不再需要代理。
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    // 目录在工程根目录之外，Vite 默认不会清空它，必须显式声明 emptyOutDir。
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  },
})
