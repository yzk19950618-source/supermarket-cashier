# 前端源码（Vue 3 + Vite + Naive UI）

可维护代码在 **`src/`**。不要用 `dist/` 改需求；改完后本地用下面方式联调。

## 本地开发：5173 调 8080 后台

1. 先启动后端，保证 **`http://localhost:8080`** 可访问（如项目根目录 `start-backend.bat` 或 `mvn spring-boot:run`）。
2. 在本目录执行：
   ```bash
   npm install
   npm run dev
   ```
3. 浏览器打开 **`http://localhost:5173`**。  
   - 所有以 **`/api`** 开头的请求会由 Vite 代理到 **`http://localhost:8080`**。  
   - **`/uploads`** 同样代理到 8080（商品图等）。

`src/utils/request.js` 里 `axios` 的 `baseURL` 为 **`/api`**，与上述代理一致。

## 生产构建（可选，给 Spring 打 jar 用）

```bash
npm run build
```

生成 `dist/`，可由后端 `classpath:/static/` 或 `src/main/resources/static` 托管，此时用户访问的是 **8080**，不再需要 5173。

项目根目录 **`sync-frontend-to-backend.ps1`** 可将 `dist/` 同步到后端静态目录（详见脚本说明）。
