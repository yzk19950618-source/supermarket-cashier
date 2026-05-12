# 离线前端契约（锁定版本）

**参照目录**：`D:\Users\PC\dist-offline\supermarket-cashier\frontend\dist`  
**主入口**：`index-Bu6cdN_h.js`（路由与 chunk 映射）  
**收银**：`assets/CashierView-B0Yk3w8G.js`  
**管理**：`assets/ManagementView-CeTRe6_f.js`

## 收银结算 `POST /api/order/settle`

离线 payload（JSON）字段：

| 字段 | 说明 |
|------|------|
| `customerName` | 客户姓名 |
| `customerPhone` | 电话 |
| `customerAddress` | 完整拼接地址文案 |
| `customerGender` | 0/1/2 |
| `customerRemark` | 本次新增客户备注（写入会员历史） |
| `repayDate` | `yyyy-MM-dd` 或 null |
| `deliveryDate` | `yyyy-MM-dd` 或 null |
| `manualRealAmount` | 应收金额；若表单留空则前端传 **null**（后端按商品合计计算） |
| `remark` | 订单备注 |
| `items` | `[{ goodsId, quantity }]` |

离线成功提示：**未支付订单** → 后端应落 **`status = 2`（未支付）** 与欠款字段。

## 区域

- `POST /api/region/all`：返回 **扁平** 数组，元素含 `level`(1省2市3县)、`code`、`parentCode`、`name`（及可选 `label`/`value` 别名）
- `POST /api/region/towns`：`{ province, city, county }` → `[{ label, value }]`
- `POST /api/region/villages`：`{ province, city, county, town }` → `[{ label, value }]`
- `POST /api/region/geocode/township-neighborhood`：`{ address, city? }` → `{ province, city, district, township?, neighborhood? }`

## 会员

- `POST /api/member/getByPhone`：`{ phone }` → 会员对象（含 `name`,`phone`,`address`,`remark`,`gender`…）

## 订单（管理端，节选）

- `POST /api/order/page`：筛选含 `customerPhone`、`status`（0退款 1已支付 2未支付 等，与实现对齐）
- `POST /api/order/detail`：`{ id }` → 详情含明细、**还款记录**、**附件列表**
- `POST /api/order/edit`：编辑订单，请求体为 `OrderUpdateDTO`（仅路径名与离线一致，字段按 DTO 解析）
- `POST /api/order/refund`
- `POST /api/order/business-config` → `{ evidenceRequired: boolean }` 等
- `POST /api/order/repayment/add|update|delete`
- `POST /api/order/attachment/upload`（multipart）
- `POST /api/order/attachment/delete`

## 商品

- `POST /api/goods/uploadImage`：multipart `file` → 返回可写入 `image` 的 URL

## 地址维护

- `POST /api/region/page|add|update|delete`：管理端 CRUD
