# 关于 `dist-offline` 目录

路径 `D:\Users\PC\dist-offline\supermarket-cashier` 下通常只有 **`frontend/dist`**（打包后的 JS/CSS/HTML），**不包含** `.vue` 源码。界面与字段顺序可参考 chunk（例如 `CashierView-*.js`）里的字符串与接口调用。

当前仓库 **`cashier-frontend/src`** 即为可维护源码；收银台页面按离线 dist 的布局还原，但 HTTP 调用必须与 **`cashier-backend` 现有 Controller** 一致（见 `FRONTEND-API-INDEX.md`）。
