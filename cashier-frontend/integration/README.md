# 收银台地址、上传与订单详情图片预览（接入说明）

将本目录下 `.vue` 示例合并到你的 Vue3 + Naive UI 工程（需已配置 `/api` 代理到后端、请求头带 `Authorization`）。

## 依赖

- `vue` ^3.4
- `naive-ui` ^2.x
- 项目内已有 axios 封装时，把示例里的 `fetch` 换成你的 `request.post`。

## 接口约定（与当前后端一致）

| 能力 | 方法 | 路径 |
|------|------|------|
| 省市区整树 | GET 或 POST | `/api/region/all` |
| 省市区子节点 | GET 或 POST | `/api/region/children?parentId=`（空为省根） |
| 上传图片 | POST `multipart/form-data` | `/api/file/upload`，字段名 `file` |
| 结算 | POST JSON | `/api/order/settle`，可增加 `receiverAddress`、`receiverRegionCodes`、`attachmentUrls` |
| 订单详情 | POST JSON | `/api/order/detail`，响应 `data.attachmentUrls` 为字符串数组 |

## 文件说明

- `CashierRegionAndUpload.example.vue`：级联地址 + `n-upload` 上传后收集 `url` 列表，结算时一并提交；缩略图旁用 `n-image` 开启预览。
- `OrderDetailImagePreview.example.vue`：订单详情里用 `n-image-group` 做多图大图预览。

复制到例如 `src/components/` 后去掉文件名中的 `.example`，在收银页、订单详情页中引入即可。
