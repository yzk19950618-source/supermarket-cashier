# 收银台区域选择与备注拼接（接入说明）

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
| 上传图片 | POST `multipart/form-data` | `/api/file/upload`，字段名 `file`（与其它业务自用 URL，不再写入订单） |
| 结算 | POST JSON | `/api/order/settle`，主体为 `memberCardNo`、`payType`、`items`、`remark`（地址与区划写入 `remark` 分段） |
| 订单详情 | POST JSON | `/api/order/detail`；收货信息等见 `data.remark`。**金额字段**：`receivableAmount`（应收合计，优惠后）、`paidAmount`（与 `realAmount` 相同，累计已还）、`remainDebt`（剩余欠款）。老前端请勿再用 `totalAmount - realAmount` 当剩余欠款（存在 `discountAmount` 时与真实欠款不一致）；请改用 `remainDebt` 或自行用 `receivableAmount - paidAmount`。列表 `/order/page` 每条记录同样包含上述三字段。 |

## 文件说明

- `CashierRegionAndUpload.example.vue`：省市区 + 详细地址，输出 `settleRemarkSegment` 供拼入结算 `remark`。
- `OrderDetailImagePreview.example.vue`：通用多图预览组件（传入 URL 字符串数组即可）。

复制到例如 `src/components/` 后去掉文件名中的 `.example`，在收银页中引入即可。
