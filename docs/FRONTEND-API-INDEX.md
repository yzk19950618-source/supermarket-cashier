# 后端接口索引（与当前 Java Controller 一致）

前端 `axios` 的 `baseURL` 为 **`/api`**。开发时须在 **`cashier-frontend` 根目录**运行 `npm run dev`，由 Vite 将 `/api` **代理到** `http://localhost:8080`（见 `vite.config.js`）。若只打开静态 `dist` 且未做反向代理，所有接口会失败（含省市区）。

> 除下列 **登录** 外，其余接口均需携带 `Authorization: Bearer <token>`。

---

## 认证 `/api/auth`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/login` | 登录（匿名） |
| POST | `/auth/logout` | 退出 |
| POST | `/auth/info` | 当前用户 |
| POST | `/auth/updatePwd` | 修改密码 |

---

## 区域 `/api/region`（收银台省市区、地址数据页）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/region/all` | 完整树（与 POST 等价） |
| POST | `/region/all` | 完整树（`consumes: ALL`） |
| GET | `/region/children?parentId=` | 子节点；**无/空 parentId 时返回省级根列表**（与 all 根一致） |
| POST | `/region/children?parentId=` | 同上（query 传参） |

前端收银台：**优先** `GET /region/children` 三级联动；失败时回退 `POST /region/all` 取省列表。

---

## 文件 `/api/file`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/file/upload` | `multipart/form-data`，字段名 **`file`** |

---

## 商品 `/api/goods`

| POST | `/goods/page` |
| POST | `/goods/add` |
| POST | `/goods/update` |
| POST | `/goods/delete` |
| POST | `/goods/updateStatus` |
| POST | `/goods/stockWarning` |

---

## 分类 `/api/category`

| POST | `/category/list` |
| POST | `/category/add` |
| POST | `/category/update` |
| POST | `/category/delete` |

---

## 会员 `/api/member`

| POST | `/member/page` |
| POST | `/member/getByCardNo` |
| POST | `/member/add` |
| POST | `/member/update` |
| POST | `/member/delete` |
| POST | `/member/recharge` |

---

## 订单 `/api/order`

| POST | `/order/settle` |
| POST | `/order/page` |
| POST | `/order/detail` |
| POST | `/order/edit` |
| POST | `/order/repayment/add` |
| POST | `/order/repayment/delete` |
| POST | `/order/attachment/add` |
| POST | `/order/attachment/delete` |
| POST | `/order/refund` |
| POST | `/order/today` |

---

## 统计 `/api/statistics`

| POST | `/statistics/dashboard` |
| POST | `/statistics/repaymentReminder` |
| POST | `/statistics/salesTrend` |
| POST | `/statistics/salesRanking` |
| POST | `/statistics/categoryPie` |
| POST | `/statistics/cashierRanking` |

---

## 用户 `/api/user`

| POST | `/user/page` |
| POST | `/user/add` |
| POST | `/user/update` |
| POST | `/user/delete` |
| POST | `/user/resetPwd` |
| POST | `/user/updateStatus` |

---

## 供应商 `/api/supplier`

| POST | `/supplier/page` |
| POST | `/supplier/list` |
| POST | `/supplier/add` |
| POST | `/supplier/update` |
| POST | `/supplier/delete` |

---

## 进货 `/api/purchase`

| POST | `/purchase/page` |
| POST | `/purchase/add` |
| POST | `/purchase/delete` |

---

封装聚合：`cashier-frontend/src/api/backend.js`。离线 UI 参考目录仅作样式参考，路径以本文为准。
