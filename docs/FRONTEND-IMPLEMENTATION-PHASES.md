# 前端实施阶段（可按优先级拆分）

当前仓库已具备完整链路时的推荐拆分方式：

1. **API 与契约**：维护 `src/api/backend.js` 与 `docs/FRONTEND-API-INDEX.md`，后端变更时先改索引再改页面。
2. **收银台**：结算 `SettleDTO`、区域级联、会员查询、`/file/upload`、条码与会员卡快捷入口（`CashierView`）。
3. **后台 CRUD**：`ManagementView` + `management/modules.js` 覆盖商品/分类/会员/订单/进货/供应商/用户。
4. **数据展示**：`DashboardView` 统计与 `RegionDataView` 只读区域树。
5. **体验**：对齐离线 dist 的样式与文案（`assets`、间距、标题），不改后端。

若需增量交付，可按 2 → 3 → 5 顺序发版；1 与 4 可并行文档化。
