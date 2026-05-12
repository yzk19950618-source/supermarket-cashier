<script setup>
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NAlert, NButton, NImage, NSpace, NTag, NText, useMessage } from 'naive-ui'
import { MODULES, genderOpts, labelOf, money, payTypeOpts, roleOpts, statusOpts } from './management/modules'
import { post, uploadFile } from '@/utils/request'
import { resolveMediaUrl } from '@/utils/mediaUrl'
import { useCashierCacheStore } from '@/stores/cashierCache'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const cashierCache = useCashierCacheStore()

function isSyntheticMemberPhone(p) {
  return typeof p === 'string' && /^X\d{19}$/.test(p)
}

/** 解析旧订单备注中的「键:值」分段（兼容历史数据）；新订单以结构化字段为准 */
function parseCashierRemark(remark) {
  const base = {
    phone: '-',
    repayDate: '-',
    deliveryDate: '-',
    customerLabel: '-',
    address: '-',
  }
  if (!remark || typeof remark !== 'string') return base
  const out = { ...base }
  const pieces = remark
    .split(/[；;]\s*/)
    .map((s) => s.trim())
    .filter(Boolean)
  for (const p of pieces) {
    const m = p.match(/^([^:：]+)[:：]\s*(.*)$/)
    if (!m) continue
    const k = m[1].trim()
    const v = m[2].trim()
    if (k === '电话') out.phone = v || '-'
    else if (k === '还款日期') out.repayDate = v || '-'
    else if (k === '送货日期') out.deliveryDate = v || '-'
    else if (k === '客户') out.customerLabel = v || '-'
    else if (k === '收货地址') out.address = v || '-'
  }
  return out
}

const moduleConfig = computed(() => MODULES[route.meta.moduleKey] || null)

/** 与商品管理一致：首行「标签 + 控件」，次行按钮靠左 */
const visibleToolbarFilters = computed(() => {
  const m = moduleConfig.value
  if (!m?.filters?.length) return []
  return m.filters.filter((f) => !f.hidden)
})

const loading = ref(false)
const rows = ref([])
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const filters = reactive({})

/** 订单/会员：部分筛选在前端对单次拉取结果分页 */
const listClientCache = ref(null)

const options = reactive({
  categoryOptions: [],
  supplierOptions: [],
})

const modalOpen = ref(false)
const editing = ref(null)
const form = reactive({})

const detailOpen = ref(false)
const detail = ref(null)

const rechargeOpen = ref(false)
const rechargeTarget = ref(null)
const rechargeAmount = ref(null)

const orderEditOpen = ref(false)
const orderEditRow = ref(null)
const orderEditDetail = ref(null)
const orderEditAmount = ref(null)
const orderEditRemark = ref('')
/** 附件上传类型（与后端 sale_order_attachment.attachment_type 一致） */
const orderEditAttachmentType = ref(1)
const attachmentTypeOpts = [
  { label: '发票', value: 1 },
  { label: '欠条', value: 2 },
  { label: '送货图片', value: 3 },
]

function attachmentTypeLabel(type) {
  const m = { 1: '发票', 2: '欠条', 3: '送货图片' }
  return m[type] ?? '附件'
}

/** 兼容仅返回 invoiceUrls 的旧接口 */
const orderEditAttachments = computed(() => {
  const d = orderEditDetail.value
  if (!d) return []
  if (Array.isArray(d.attachments) && d.attachments.length) return d.attachments
  return (d.invoiceUrls || []).map((url, i) => ({
    id: `legacy-${i}`,
    attachmentType: 1,
    url,
  }))
})

/** 订单详情抽屉中的附件列表（同上兼容） */
const detailAttachments = computed(() => {
  const d = detail.value
  if (!d) return []
  if (Array.isArray(d.attachments) && d.attachments.length) return d.attachments
  return (d.invoiceUrls || []).map((url, i) => ({
    id: `legacy-${i}`,
    attachmentType: 1,
    url,
  }))
})
const invoiceFileInputRef = ref(null)
const repaymentModalOpen = ref(false)
const repaymentDraft = reactive({ amount: null, payType: 0, remark: '' })

async function removeRepaymentRecord(row, scope) {
  if (!row?.id) {
    message.warning('无法删除该记录')
    return
  }
  if (
    !window.confirm('确认删除该笔还款记录？删除后将重算订单已收金额与状态。')
  ) {
    return
  }
  try {
    await post('/order/repayment/delete', { id: row.id })
    message.success('已删除还款记录')
    if (scope === 'edit') {
      const oid = orderEditRow.value?.id
      if (oid) {
        orderEditDetail.value = await post('/order/detail', { id: oid })
        syncOrderEditDebtFromDetail()
      }
    } else if (scope === 'detail' && detail.value?.id) {
      detail.value = await post('/order/detail', { id: detail.value.id })
    }
    await fetchData()
  } catch (e) {
    message.error(e.message || '删除失败')
  }
}

/** 编辑订单内还款列表列（金额 / 支付方式 / 备注 / 时间 / 操作） */
const repaymentEditColumns = computed(() => [
  { title: '金额', key: 'amount', render: (row) => money(row.amount) },
  {
    title: '支付方式',
    key: 'payType',
    render: (row) => labelOf(payTypeOpts, row.payType),
  },
  { title: '备注', key: 'remark', ellipsis: { tooltip: true } },
  {
    title: '时间',
    key: 'createTime',
    render: (row) => (row.createTime ? String(row.createTime).replace('T', ' ').slice(0, 19) : '-'),
  },
  {
    title: '操作',
    key: '_op',
    width: 80,
    render: (row) =>
      row.id != null
        ? h(
            NButton,
            { size: 'tiny', text: true, type: 'error', onClick: () => removeRepaymentRecord(row, 'edit') },
            { default: () => '删除' },
          )
        : '—',
  },
])

/** 订单详情抽屉还款表列（含删除） */
const repaymentDetailColumns = computed(() => [
  { title: '还款金额', key: 'amount', render: (r) => money(r.amount) },
  {
    title: '支付方式',
    key: 'payType',
    render: (r) => labelOf(payTypeOpts, r.payType),
  },
  { title: '备注', key: 'remark', ellipsis: true },
  {
    title: '还款时间',
    key: 'createTime',
    render: (r) => (r.createTime ? String(r.createTime).replace('T', ' ').slice(0, 19) : '-'),
  },
  { title: '操作人', key: 'operatorName', ellipsis: true },
  {
    title: '操作',
    key: '_opDel',
    width: 72,
    render: (row) =>
      row.id != null
        ? h(
            NButton,
            { size: 'tiny', text: true, type: 'error', onClick: () => removeRepaymentRecord(row, 'detail') },
            { default: () => '删除' },
          )
        : '—',
  },
])

const repaymentPayTypeOpts = [
  { label: '现金', value: 0 },
  { label: '微信', value: 1 },
  { label: '支付宝', value: 2 },
  { label: '银行卡', value: 4 },
]

const orderDescLabelStyle = {
  width: '50%',
  backgroundColor: '#E0E4E8',
  fontWeight: '600',
  fontSize: '14px',
  color: '#333333',
}
const orderDescContentStyle = { width: '50%', verticalAlign: 'top' }

/** 应收（优惠后） */
function orderDueAmount(d) {
  if (!d) return 0
  return Math.max(0, Number(d.totalAmount || 0) - Number(d.discountAmount ?? 0))
}

/** 剩余欠款 = 应收 − 已还（优先与后端 paidAmount / 还款明细一致） */
function orderRemainDebt(d) {
  if (!d) return 0
  return Math.max(0, orderDueAmount(d) - orderSummaryPaid(d))
}

const orderEditRemainDebt = computed(() => orderRemainDebt(orderEditDetail.value))

/** 详情/编辑顶部三栏：优先用后端 receivableAmount、paidAmount、remainDebt（兼容老接口无字段时前端回算） */
function orderSummaryReceivable(d) {
  if (!d) return 0
  const v = d.receivableAmount
  if (v != null && v !== '') return Number(v)
  return orderDueAmount(d)
}

function orderSummaryPaid(d) {
  if (!d) return 0
  const v = d.paidAmount
  if (v != null && v !== '') return Number(v)
  if (Array.isArray(d.repayments) && d.repayments.length) {
    const sum = d.repayments.reduce((s, r) => s + Number(r.amount ?? 0), 0)
    return Number(sum.toFixed(2))
  }
  return Number(d.realAmount ?? 0)
}

function orderSummaryRemain(d) {
  if (!d) return 0
  const v = d.remainDebt
  if (v != null && v !== '') return Number(v)
  return orderRemainDebt(d)
}

/** 编辑弹窗顶部数字框：展示/编辑「当前欠款」；保存时提交 remainDebt，后端反算优惠金额并同步累计已收 */
function syncOrderEditDebtFromDetail() {
  const d = orderEditDetail.value
  if (!d) {
    orderEditAmount.value = null
    return
  }
  orderEditAmount.value = Number(orderSummaryRemain(d).toFixed(2))
}

/** 将输入的欠款夹在 [0, 应收] 内，供提交 remainDebt */
function clampRemainDebtForSave(detail, debtInput) {
  const R = orderSummaryReceivable(detail)
  let D = Number(debtInput)
  if (!Number.isFinite(D)) D = 0
  D = Math.max(0, Math.min(R, D))
  return Number(D.toFixed(2))
}

const orderEditReceivableCap = computed(() =>
  Number(orderSummaryReceivable(orderEditDetail.value).toFixed(2)),
)

function openRepaymentModal() {
  const d = orderEditDetail.value
  const remain = orderRemainDebt(d)
  if (!(remain > 0)) return
  repaymentDraft.payType = 0
  repaymentDraft.remark = ''
  repaymentDraft.amount = remain
  repaymentModalOpen.value = true
}

function resetFilters() {
  listClientCache.value = null
  Object.keys(filters).forEach((k) => delete filters[k])
  if (moduleConfig.value?.filters) {
    for (const f of moduleConfig.value.filters) {
      if (f.hidden) filters[f.key] = null
      else filters[f.key] = f.type === 'select' ? null : ''
    }
  }
}

function applyOrderMemberIdFromRoute() {
  if (route.meta.moduleKey !== 'orders') return
  const mid = route.query.memberId
  if (mid === undefined || mid === null || mid === '') {
    filters.memberId = null
    return
  }
  const n = Number(mid)
  filters.memberId = Number.isFinite(n) ? n : null
}

function onToolbarReset() {
  resetFilters()
  if (route.name === 'orders' && route.query.memberId != null && String(route.query.memberId) !== '') {
    router.replace({ name: 'orders', query: {} })
    return
  }
  fetchData()
}

function fieldOptions(f) {
  if (f.optionsKey) return options[f.optionsKey] || []
  return f.options || []
}

function applyClientPagination() {
  if (!listClientCache.value?.records) return
  const records = listClientCache.value.records
  page.total = records.length
  const start = (page.pageNum - 1) * page.pageSize
  rows.value = records.slice(start, start + page.pageSize)
}

function buildColumns() {
  const m = moduleConfig.value
  if (!m?.columns) return []
  const cols = m.columns.map((c) => {
    const col = {
      title: c.title,
      key: c.key,
      ...(c.width != null ? { width: c.width } : {}),
      ...(c.minWidth != null ? { minWidth: c.minWidth } : {}),
      ...(c.maxWidth != null ? { maxWidth: c.maxWidth } : {}),
      ...(c.align != null ? { align: c.align } : {}),
      ...(c.fixed != null ? { fixed: c.fixed } : {}),
      ...(c.ellipsis !== undefined ? { ellipsis: c.ellipsis } : {}),
      render(row) {
        const v = row[c.key]
        if (c.empty && (v == null || v === '')) return c.empty
        if (c.format === 'money') return money(v)
        if (c.format === 'gender') return labelOf(genderOpts, v)
        if (c.format === 'role') return labelOf(roleOpts, v)
        if (c.format === 'payType') return labelOf(payTypeOpts, v)
        if (c.format === 'statusTag') {
          return h(NTag, { type: v ? 'success' : 'error' }, { default: () => labelOf(statusOpts, v) })
        }
        if (c.format === 'orderCustomerName') {
          const name = row.customerName || row.memberName
          return name == null || name === '' ? '-' : String(name)
        }
        if (c.format === 'orderDate') {
          return formatDetailDate(row.orderDate ?? row.createTime)
        }
        if (c.format === 'orderDebt') {
          const debt =
            row.remainDebt != null && row.remainDebt !== '' ? Number(row.remainDebt) : orderRemainDebt(row)
          return h(
            'span',
            { style: debt > 0.009 ? 'color:#e6a23c;font-weight:600' : 'color:#67c23a' },
            money(debt),
          )
        }
        if (c.format === 'orderUiStatus') {
          if (row.status === 0) return h(NTag, { type: 'error' }, { default: () => '已退款' })
          if (row.status === 1) return h(NTag, { type: 'success' }, { default: () => '已支付' })
          const remain = orderRemainDebt(row)
          if (remain <= 0.009) return h(NTag, { type: 'success' }, { default: () => '已支付' })
          return h(NTag, { type: 'warning', bordered: false }, { default: () => '待回款' })
        }
        if (c.format === 'memberPhone') {
          const pv = row[c.key]
          if (pv == null || pv === '') return '-'
          if (isSyntheticMemberPhone(String(pv))) return '—'
          return String(pv)
        }
        if (c.format === 'memberTotalDebt') {
          const amt = Number(row.totalDebt ?? row[c.key] ?? 0)
          return h(
            'span',
            {
              role: 'button',
              tabIndex: 0,
              style:
                'display:inline-block;color:#e6a23c;font-weight:600;cursor:pointer;white-space:nowrap;',
              onClick: () => {
                if (row?.id != null) {
                  router.push({ name: 'orders', query: { memberId: String(row.id) } })
                }
              },
            },
            money(amt),
          )
        }
        if (c.format === 'placeholderDash') return '-'
        if (c.format === 'image' && v) {
          return h('img', {
            src: resolveMediaUrl(v),
            style: 'width:40px;height:40px;object-fit:cover;border-radius:4px;border:1px solid #ebeef5;',
          })
        }
        if (c.format === 'image') return '-'
        return v ?? '-'
      },
    }
    return col
  })
  cols.push({
    title: '操作',
    key: 'actions',
    width: m.actionColumnWidth ?? 300,
    render(row) {
      const btns = []
      if (m.extraActions?.includes('editOrder')) {
        btns.push(
          h(
            NButton,
            { size: 'small', text: true, type: 'primary', onClick: () => openEdit(row) },
            { default: () => '编辑' },
          ),
        )
      }
      if (m.updateEndpoint) {
        btns.push(
          h(
            NButton,
            { size: 'small', text: true, type: 'primary', onClick: () => openEdit(row) },
            { default: () => '编辑' },
          ),
        )
      }
      if (m.detailEndpoint && m.extraActions?.includes('detail')) {
        btns.push(
          h(
            NButton,
            { size: 'small', text: true, onClick: () => openDetail(row) },
            { default: () => '详情' },
          ),
        )
      }
      if (m.deleteEndpoint) {
        btns.push(
          h(
            NButton,
            { size: 'small', text: true, type: 'error', onClick: () => onDelete(row) },
            { default: () => '删除' },
          ),
        )
      }
      if (m.statusEndpoint) {
        btns.push(
          h(
            NButton,
            {
              size: 'small',
              text: true,
              type: row.status ? 'default' : 'success',
              onClick: () => toggleStatus(row),
            },
            { default: () => (row.status ? '禁用' : '启用') },
          ),
        )
      }
      if (m.extraActions?.includes('refund') && row.status === 1) {
        btns.push(
          h(
            NButton,
            { size: 'small', text: true, type: 'warning', onClick: () => onRefund(row) },
            { default: () => '退款' },
          ),
        )
      }
      if (m.extraActions?.includes('resetPwd')) {
        btns.push(
          h(
            NButton,
            { size: 'small', text: true, type: 'warning', onClick: () => onResetPwd(row) },
            { default: () => '重置密码' },
          ),
        )
      }
      if (m.extraActions?.includes('recharge')) {
        btns.push(
          h(
            NButton,
            { size: 'small', text: true, type: 'success', onClick: () => openRecharge(row) },
            { default: () => '充值' },
          ),
        )
      }
      return h(NSpace, { size: 8 }, { default: () => btns })
    },
  })
  return cols
}

const tableColumns = computed(() => buildColumns())

async function loadOptions() {
  try {
    const cats = await post('/category/list')
    options.categoryOptions = (cats || []).map((t) => ({ label: t.name, value: t.id }))
  } catch {
    options.categoryOptions = []
  }
  try {
    const sups = await post('/supplier/list')
    options.supplierOptions = (sups || []).map((t) => ({ label: t.name, value: t.id }))
  } catch {
    options.supplierOptions = []
  }
}

async function fetchData() {
  const m = moduleConfig.value
  if (!m || m.placeholder) return
  loading.value = true
  try {
    listClientCache.value = null
    const payload = { ...filters }
    for (const f of m.filters || []) {
      if (f.clientOnly) delete payload[f.key]
    }
    if (!m.noPagination) {
      payload.pageNum = page.pageNum
      payload.pageSize = page.pageSize
    }

    if (
      m.orderClientFilter &&
      !filters.memberId &&
      (filters.customerName?.trim() || filters.customerPhone?.trim())
    ) {
      payload.pageNum = 1
      payload.pageSize = 500
    }
    if ('status' in payload && (payload.status === null || payload.status === undefined)) {
      delete payload.status
    }
    if ('memberId' in payload && (payload.memberId == null || payload.memberId === '')) {
      delete payload.memberId
    }

    const data = await post(m.pageEndpoint, payload)
    if (m.noPagination) {
      let list = Array.isArray(data) ? data : []
      const name = filters.name?.trim()
      if (name) list = list.filter((x) => (x.name || '').includes(name))
      rows.value = list
      page.total = list.length
      return
    }

    let records = data.records || []

    if (route.meta.moduleKey === 'orders' && m.orderClientFilter && !filters.memberId) {
      const cn = filters.customerName?.trim()
      const cp = filters.customerPhone?.trim()
      if (cn || cp) {
        if (cn) {
          records = records.filter((r) =>
            `${r.memberName || ''}${r.customerName || ''}`.includes(cn),
          )
        }
        if (cp) {
          records = records.filter((r) => (r.remark || '').includes(cp))
        }
        listClientCache.value = { records }
        page.total = records.length
        page.pageNum = 1
        applyClientPagination()
        return
      }
    }

    rows.value = records
    page.total = data.total || 0
  } catch (e) {
    message.error(e.message || '数据加载失败')
  } finally {
    loading.value = false
  }
}

function onPageUpdate(p) {
  page.pageNum = p
  if (listClientCache.value) applyClientPagination()
  else fetchData()
}

function onPageSizeUpdate(ps) {
  page.pageSize = ps
  page.pageNum = 1
  if (listClientCache.value) applyClientPagination()
  else fetchData()
}

function openCreate() {
  editing.value = null
  Object.assign(form, moduleConfig.value.createForm ? moduleConfig.value.createForm() : {})
  modalOpen.value = true
}

async function openEdit(row) {
  if (route.meta.moduleKey === 'orders' && moduleConfig.value.extraActions?.includes('editOrder')) {
    orderEditRow.value = row
    try {
      orderEditDetail.value = await post('/order/detail', { id: row.id })
      syncOrderEditDebtFromDetail()
      orderEditRemark.value = orderEditDetail.value.remark || ''
      orderEditAttachmentType.value = 1
      repaymentDraft.amount = null
      repaymentDraft.payType = 0
      repaymentDraft.remark = ''
      orderEditOpen.value = true
    } catch (e) {
      message.error(e.message || '加载订单失败')
    }
    return
  }
  editing.value = row
  Object.assign(form, { ...row })
  modalOpen.value = true
}

function closeOrderEdit() {
  orderEditOpen.value = false
  orderEditDetail.value = null
}

async function saveOrderEdit() {
  const row = orderEditRow.value
  if (!row?.id) return
  const d = orderEditDetail.value
  if (!d) return
  try {
    await post('/order/edit', {
      id: row.id,
      remainDebt: clampRemainDebtForSave(d, orderEditAmount.value),
      remark: orderEditRemark.value?.trim() || undefined,
    })
    message.success('保存成功')
    orderEditOpen.value = false
    orderEditDetail.value = null
    await fetchData()
  } catch (e) {
    message.error(e.message || '保存失败')
  }
}

async function submitRepayment() {
  const row = orderEditRow.value
  const amt = Number(repaymentDraft.amount)
  if (!row?.id || !(amt > 0)) {
    message.warning('请输入大于 0 的还款金额')
    return
  }
  try {
    await post('/order/repayment/add', {
      orderId: row.id,
      amount: amt,
      payType: repaymentDraft.payType,
      remark: repaymentDraft.remark?.trim() || undefined,
    })
    message.success('还款已记录')
    repaymentModalOpen.value = false
    orderEditDetail.value = await post('/order/detail', { id: row.id })
    syncOrderEditDebtFromDetail()
    await fetchData()
  } catch (e) {
    message.error(e.message || '提交失败')
  }
}

function triggerOrderAttachmentPick() {
  invoiceFileInputRef.value?.click()
}

async function onInvoiceUploadEdit(ev) {
  const f = ev.target?.files?.[0]
  const row = orderEditRow.value
  if (!f || !row?.id) return
  try {
    const res = await uploadFile('/file/upload', f)
    const url = res?.url || res?.path
    if (!url) return
    await post('/order/attachment/add', {
      orderId: row.id,
      url,
      attachmentType: orderEditAttachmentType.value ?? 1,
    })
    message.success('附件已关联')
    orderEditDetail.value = await post('/order/detail', { id: row.id })
    syncOrderEditDebtFromDetail()
    await fetchData()
  } catch (e) {
    message.error(e.message || '上传失败')
  } finally {
    ev.target.value = ''
  }
}

function canDeleteOrderAttachment(att) {
  if (att?.id == null) return false
  if (String(att.id).startsWith('legacy')) return false
  const n = typeof att.id === 'number' ? att.id : Number(att.id)
  return Number.isFinite(n) && n > 0
}

async function removeOrderAttachment(att, scope) {
  if (!canDeleteOrderAttachment(att)) {
    message.warning('该附件无法删除，请刷新页面后重试')
    return
  }
  if (!window.confirm('确认删除该附件？')) return
  const id = typeof att.id === 'number' ? att.id : Number(att.id)
  try {
    await post('/order/attachment/delete', { id })
    message.success('已删除')
    if (scope === 'edit') {
      const row = orderEditRow.value
      if (row?.id) {
        orderEditDetail.value = await post('/order/detail', { id: row.id })
        syncOrderEditDebtFromDetail()
      }
    } else if (scope === 'detail' && detail.value?.id) {
      detail.value = await post('/order/detail', { id: detail.value.id })
    }
    await fetchData()
  } catch (e) {
    message.error(e.message || '删除失败')
  }
}

function formatDetailDate(v) {
  if (v == null || v === '') return '-'
  const s = String(v)
  if (/^\d{4}-\d{2}-\d{2}/.test(s)) return s.slice(0, 10)
  if (/^\d{4}-\d{2}-\d{2}T/.test(s)) return s.slice(0, 10)
  return s
}

async function saveForm() {
  const m = moduleConfig.value
  try {
    const url = editing.value ? m.updateEndpoint : m.addEndpoint
    const body = editing.value ? { ...form, id: editing.value.id } : { ...form }
    if (route.meta.moduleKey === 'users' && !editing.value && !body.password) {
      message.warning('新增用户请填写密码')
      return
    }
    await post(url, body)
    message.success(editing.value ? '更新成功' : '新增成功')
    modalOpen.value = false
    await fetchData()
    if (route.meta.moduleKey === 'goods' || route.meta.moduleKey === 'categories') {
      void cashierCache.refreshGoodsOnShelf()
    }
  } catch (e) {
    message.error(e.message || '保存失败')
  }
}

async function onDelete(row) {
  const m = moduleConfig.value
  if (!window.confirm(`确认删除「${row.name || row.orderNo || row.username || row.purchaseNo || '当前记录'}」吗？`))
    return
  try {
    await post(m.deleteEndpoint, { id: row.id })
    message.success('删除成功')
    await fetchData()
    if (route.meta.moduleKey === 'goods') void cashierCache.refreshGoodsOnShelf()
  } catch (e) {
    message.error(e.message || '删除失败')
  }
}

async function toggleStatus(row) {
  const m = moduleConfig.value
  try {
    await post(m.statusEndpoint, { id: row.id, status: row.status ? 0 : 1 })
    message.success('状态已更新')
    await fetchData()
    if (route.meta.moduleKey === 'goods') void cashierCache.refreshGoodsOnShelf()
  } catch (e) {
    message.error(e.message || '状态更新失败')
  }
}

async function openDetail(row) {
  const m = moduleConfig.value
  try {
    detail.value = await post(m.detailEndpoint, { id: row.id })
    detailOpen.value = true
  } catch (e) {
    message.error(e.message || '详情加载失败')
  }
}

const detailRemarkParsed = computed(() => parseCashierRemark(detail.value?.remark))

async function onRefund(row) {
  if (!window.confirm(`确认退款订单 ${row.orderNo} 吗？`)) return
  try {
    await post('/order/refund', { id: row.id })
    message.success('退款成功')
    await fetchData()
  } catch (e) {
    message.error(e.message || '退款失败')
  }
}

async function onResetPwd(row) {
  if (!window.confirm(`确认将 ${row.username} 的密码重置为 123456 吗？`)) return
  try {
    await post('/user/resetPwd', { id: row.id })
    message.success('密码已重置为 123456')
  } catch (e) {
    message.error(e.message || '重置失败')
  }
}

function openRecharge(row) {
  rechargeTarget.value = row
  rechargeAmount.value = null
  rechargeOpen.value = true
}

async function submitRecharge() {
  const row = rechargeTarget.value
  const amt = Number(rechargeAmount.value)
  if (!row?.id || !(amt > 0)) {
    message.warning('请输入大于 0 的充值金额')
    return
  }
  try {
    await post('/member/recharge', { id: row.id, amount: amt })
    message.success('充值成功')
    rechargeOpen.value = false
    await fetchData()
  } catch (e) {
    message.error(e.message || '充值失败')
  }
}

async function onImageField(ev, key) {
  const f = ev.target?.files?.[0]
  if (!f) return
  try {
    const res = await uploadFile('/file/upload', f)
    const url = res?.url || res?.path
    if (url) {
      form[key] = url
      message.success('图片上传成功')
    }
  } catch (e) {
    message.error(e.message || '上传失败')
  } finally {
    ev.target.value = ''
  }
}

watch(
  () => ({ mk: route.meta.moduleKey, mid: route.query.memberId }),
  async (cur, prev) => {
    const mkChanged = cur.mk !== prev?.mk
    const midChanged = cur.mid !== prev?.mid
    if (mkChanged) {
      resetFilters()
      applyOrderMemberIdFromRoute()
      page.pageNum = 1
      await loadOptions()
      await fetchData()
    } else if (cur.mk === 'orders' && midChanged) {
      applyOrderMemberIdFromRoute()
      page.pageNum = 1
      await fetchData()
    }
  },
  { immediate: true },
)

onMounted(loadOptions)
</script>

<template>
  <n-space v-if="moduleConfig" vertical size="large">
    <n-card class="page-card" :bordered="false">
      <n-alert
        v-if="route.meta.moduleKey === 'orders' && filters.memberId"
        type="info"
        style="margin-bottom: 12px"
        title="客户订单"
      >
        当前仅显示所选客户的订单。
      </n-alert>
      <n-alert
        v-else-if="route.meta.moduleKey === 'orders' && (filters.customerName || filters.customerPhone)"
        type="warning"
        style="margin-bottom: 12px"
        title="提示"
      >
        客户姓名/电话筛选在前端对单次拉取的数据过滤（最多 500 条）；生产环境建议后端扩展查询条件。
      </n-alert>

      <div v-if="visibleToolbarFilters.length" class="toolbar toolbar--labeled-two-row">
        <div class="toolbar-filters-row">
          <div v-for="ff in visibleToolbarFilters" :key="ff.key" class="toolbar-labeled-field">
            <span class="toolbar-field-label">{{ ff.label }}</span>
            <n-input
              v-if="ff.type === 'input'"
              v-model:value="filters[ff.key]"
              class="toolbar-field-control"
              :placeholder="`请输入${ff.label}`"
            />
            <n-select
              v-else-if="ff.type === 'select'"
              v-model:value="filters[ff.key]"
              class="toolbar-field-control"
              clearable
              :options="fieldOptions(ff)"
              :placeholder="`请选择${ff.label}`"
            />
          </div>
        </div>
        <div class="toolbar-actions-row">
          <n-space>
            <n-button type="primary" @click="fetchData">查询</n-button>
            <n-button @click="onToolbarReset">重置</n-button>
            <n-button v-if="moduleConfig.addEndpoint" type="success" @click="openCreate">新增</n-button>
          </n-space>
        </div>
      </div>
      <div v-else class="toolbar toolbar--labeled-two-row">
        <div class="toolbar-actions-row">
          <n-space>
            <n-button type="primary" @click="fetchData">查询</n-button>
            <n-button @click="onToolbarReset">重置</n-button>
            <n-button v-if="moduleConfig.addEndpoint" type="success" @click="openCreate">新增</n-button>
          </n-space>
        </div>
      </div>

      <n-data-table :columns="tableColumns" :data="rows" :loading="loading" size="small" striped />

      <div v-if="!moduleConfig.noPagination" class="table-footer">
        <n-pagination
          v-model:page="page.pageNum"
          v-model:page-size="page.pageSize"
          :page-count="Math.max(1, Math.ceil(page.total / page.pageSize))"
          show-size-picker
          :page-sizes="[10, 20, 50]"
          @update:page="onPageUpdate"
          @update:page-size="onPageSizeUpdate"
        />
      </div>
    </n-card>

    <n-modal
      v-model:show="modalOpen"
      preset="card"
      :title="(editing ? '编辑' : '新增') + moduleConfig.title"
      :style="{ width: route.meta.moduleKey === 'goods' ? '760px' : '720px' }"
      :bordered="false"
      :mask-closable="false"
    >
      <template #action>
        <n-space justify="end">
          <n-button @click="modalOpen = false">取消</n-button>
          <n-button type="primary" @click="saveForm">保存</n-button>
        </n-space>
      </template>
      <n-form :model="form" label-placement="top">
        <n-grid :cols="2" :x-gap="16">
          <n-gi
            v-for="ff in moduleConfig.formFields || []"
            :key="ff.key"
            :span="ff.gridSpan ?? (moduleConfig.formGridFieldSpan ?? 2)"
          >
            <n-form-item :label="ff.label">
              <n-input
                v-if="ff.type === 'input'"
                v-model:value="form[ff.key]"
                :placeholder="`请输入${ff.label}`"
                :maxlength="ff.maxlength"
                :show-count="ff.maxlength != null"
              />
              <n-input
                v-else-if="ff.type === 'textarea'"
                v-model:value="form[ff.key]"
                type="textarea"
                :placeholder="`请输入${ff.label}`"
                :autosize="{ minRows: ff.rows ?? 5, maxRows: ff.maxRows ?? 16 }"
                :maxlength="ff.maxlength"
                :show-count="ff.maxlength != null"
              />
              <n-input-number
                v-else-if="ff.type === 'number'"
                v-model:value="form[ff.key]"
                :min="0"
                :show-button="ff.showStepper === true"
                :step="ff.step"
                :precision="ff.precision"
                style="width: 100%"
              />
              <n-select
                v-else-if="ff.type === 'select'"
                v-model:value="form[ff.key]"
                :options="fieldOptions(ff)"
                :placeholder="`请选择${ff.label}`"
              />
              <div v-else-if="ff.type === 'file'" style="width: 100%">
                <input type="file" accept="image/*" @change="(e) => onImageField(e, ff.key)" />
                <img
                  v-if="form[ff.key]"
                  :src="resolveMediaUrl(form[ff.key])"
                  alt=""
                  style="margin-top: 8px; width: 80px; height: 80px; object-fit: cover; border-radius: 4px"
                />
              </div>
            </n-form-item>
          </n-gi>
        </n-grid>
      </n-form>
    </n-modal>

    <n-modal
      v-model:show="orderEditOpen"
      preset="card"
      title="编辑订单"
      style="width: 800px"
      :bordered="false"
      :mask-closable="false"
      @close="closeOrderEdit"
    >
      <template #action>
        <n-space justify="end">
          <n-button @click="closeOrderEdit">取消</n-button>
          <n-button type="primary" @click="saveOrderEdit">保存</n-button>
        </n-space>
      </template>
      <n-space vertical size="large">
        <n-grid :cols="2" :x-gap="16">
          <n-gi>
            <n-form-item label="当前欠款">
              <n-space vertical :size="6" style="width: 100%">
                <n-input-number
                  v-model:value="orderEditAmount"
                  :min="0"
                  :max="orderEditReceivableCap"
                  :precision="2"
                  :show-button="true"
                  style="width: 100%"
                />
                <n-text depth="3" style="font-size: 12px">
                  应收(优惠后) {{ money(orderEditReceivableCap) }}；累计已收
                  {{ money(orderSummaryPaid(orderEditDetail)) }}。保存时提交当前欠款，服务端按订单总额与还款合计反算优惠金额。
                </n-text>
              </n-space>
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="操作备注">
              <n-input
                v-model:value="orderEditRemark"
                type="textarea"
                placeholder="请输入操作备注"
                :rows="5"
              />
            </n-form-item>
          </n-gi>
        </n-grid>
        <n-alert type="info" title="商品明细已锁定">
          为避免金额计算异常，订单编辑中已禁用商品修改；如需调整商品，请先退款后重新下单。
        </n-alert>
        <n-card title="附件管理" size="small" embedded>
          <n-space vertical size="medium">
            <n-space align="center" :size="12" style="flex-wrap: wrap">
              <n-select
                v-model:value="orderEditAttachmentType"
                :options="attachmentTypeOpts"
                placeholder="附件类型"
                style="width: 200px"
              />
              <n-button type="success" @click="triggerOrderAttachmentPick">选择图片上传</n-button>
              <input
                ref="invoiceFileInputRef"
                type="file"
                accept="image/*"
                style="display: none"
                @change="onInvoiceUploadEdit"
              />
            </n-space>
            <n-space v-if="orderEditAttachments.length" :size="16" style="flex-wrap: wrap">
              <div
                v-for="(a, i) in orderEditAttachments"
                :key="a.id ?? `${a.url}-${i}`"
                class="order-edit-att-chip"
              >
                <n-space align="center" :size="8">
                  <n-tag size="small" type="info">{{ attachmentTypeLabel(a.attachmentType) }}</n-tag>
                  <n-button
                    v-if="canDeleteOrderAttachment(a)"
                    text
                    type="error"
                    size="tiny"
                    @click="removeOrderAttachment(a, 'edit')"
                  >
                    删除
                  </n-button>
                </n-space>
                <n-image
                  width="96"
                  height="96"
                  object-fit="cover"
                  :src="resolveMediaUrl(a.url)"
                />
              </div>
            </n-space>
            <n-text v-else depth="3">暂无附件</n-text>
          </n-space>
        </n-card>
        <n-card title="分批还款管理" size="small" embedded>
          <n-space justify="space-between" style="width: 100%; margin-bottom: 8px">
            <n-text>本单还可还：￥{{ orderEditRemainDebt.toFixed(2) }}</n-text>
            <n-button
              type="success"
              size="small"
              :disabled="orderEditRemainDebt <= 0"
              @click="openRepaymentModal"
            >
              新增还款
            </n-button>
          </n-space>
          <n-data-table
            size="small"
            :pagination="false"
            :scroll-x="520"
            max-height="280"
            striped
            :columns="repaymentEditColumns"
            :data="orderEditDetail?.repayments || []"
          />
        </n-card>
      </n-space>
    </n-modal>

    <n-modal
      v-model:show="repaymentModalOpen"
      preset="card"
      title="新增还款"
      style="width: 440px"
      :bordered="false"
      :mask-closable="false"
    >
      <template #action>
        <n-space justify="end">
          <n-button @click="repaymentModalOpen = false">取消</n-button>
          <n-button type="primary" @click="submitRepayment">确认还款</n-button>
        </n-space>
      </template>
      <n-space vertical size="large">
        <div>
          <div style="margin-bottom: 6px; font-weight: 600">还款金额</div>
          <n-input-number
            v-model:value="repaymentDraft.amount"
            :min="0.01"
            :max="Math.max(0.01, orderEditRemainDebt)"
            :step="0.01"
            placeholder="金额"
            style="width: 100%"
          />
        </div>
        <div>
          <div style="margin-bottom: 6px; font-weight: 600">支付方式</div>
          <n-select v-model:value="repaymentDraft.payType" :options="repaymentPayTypeOpts" style="width: 100%" />
        </div>
        <div>
          <div style="margin-bottom: 6px; font-weight: 600">备注</div>
          <n-input v-model:value="repaymentDraft.remark" type="textarea" placeholder="选填" :rows="2" />
        </div>
      </n-space>
    </n-modal>

    <n-modal
      v-model:show="rechargeOpen"
      preset="card"
      title="会员充值"
      style="width: 420px"
      :bordered="false"
      :mask-closable="false"
    >
      <template #action>
        <n-space justify="end">
          <n-button @click="rechargeOpen = false">取消</n-button>
          <n-button type="primary" @click="submitRecharge">确认充值</n-button>
        </n-space>
      </template>
      <n-space vertical>
        <n-text v-if="rechargeTarget">客户：{{ rechargeTarget.name }}（卡号 {{ rechargeTarget.cardNo }}）</n-text>
        <n-input-number
          v-model:value="rechargeAmount"
          :min="0.01"
          :step="0.01"
          placeholder="充值金额"
          style="width: 100%"
        />
      </n-space>
    </n-modal>

    <n-drawer v-model:show="detailOpen" :width="880" class="order-detail-drawer" :mask-closable="true">
      <n-drawer-content title="订单详情" closable>
        <template v-if="detail">
          <div class="order-debt-summary-row order-debt-summary-row--drawer">
            <div class="order-debt-summary-cell order-debt-summary-cell--due">
              <div class="order-debt-summary-label">应收合计</div>
              <div class="order-debt-summary-value">￥{{ orderSummaryReceivable(detail).toFixed(2) }}</div>
            </div>
            <div class="order-debt-summary-cell order-debt-summary-cell--paid">
              <div class="order-debt-summary-label">已还</div>
              <div class="order-debt-summary-value">￥{{ orderSummaryPaid(detail).toFixed(2) }}</div>
            </div>
            <div class="order-debt-summary-cell order-debt-summary-cell--remain">
              <div class="order-debt-summary-label">剩余欠款</div>
              <div class="order-debt-summary-value">￥{{ orderSummaryRemain(detail).toFixed(2) }}</div>
            </div>
          </div>

          <n-card class="page-card order-detail-base-card" :bordered="false" title="订单基础信息">
            <n-descriptions
              bordered
              size="small"
              :column="2"
              class="order-detail-descriptions"
              :label-style="orderDescLabelStyle"
              :content-style="orderDescContentStyle"
            >
              <n-descriptions-item label="订单号">{{ detail.orderNo }}</n-descriptions-item>
              <n-descriptions-item label="客户">{{
                detail.memberName || detail.customerName || detailRemarkParsed.customerLabel || '-'
              }}</n-descriptions-item>
              <n-descriptions-item label="电话">{{
                detail.customerPhone || detailRemarkParsed.phone
              }}</n-descriptions-item>
              <n-descriptions-item label="地址">{{
                detail.customerAddress || detailRemarkParsed.address
              }}</n-descriptions-item>
              <n-descriptions-item label="订单日期">{{ formatDetailDate(detail.orderDate || detail.createTime) }}</n-descriptions-item>
              <n-descriptions-item label="还款日期">{{ formatDetailDate(detail.repayDate) }}</n-descriptions-item>
              <n-descriptions-item label="送货日期">{{ formatDetailDate(detail.deliveryDate) }}</n-descriptions-item>
              <n-descriptions-item label="支付方式">{{ labelOf(payTypeOpts, detail.payType) }}</n-descriptions-item>
              <n-descriptions-item label="订单总额">{{ money(detail.totalAmount) }}</n-descriptions-item>
              <n-descriptions-item label="优惠金额">{{ money(detail.discountAmount) }}</n-descriptions-item>
              <n-descriptions-item label="累计实收（已还）">{{ money(orderSummaryPaid(detail)) }}</n-descriptions-item>
              <n-descriptions-item label="备注">{{ detail.remark || '-' }}</n-descriptions-item>
            </n-descriptions>
          </n-card>

          <n-card class="page-card order-detail-section-card" :bordered="false" title="附件">
            <n-space v-if="detailAttachments.length" :size="16" style="flex-wrap: wrap">
              <div
                v-for="(a, i) in detailAttachments"
                :key="a.id ?? `${a.url}-${i}`"
                class="order-edit-att-chip"
              >
                <n-space align="center" :size="8">
                  <n-tag size="small" type="info">{{ attachmentTypeLabel(a.attachmentType) }}</n-tag>
                  <n-button
                    v-if="canDeleteOrderAttachment(a)"
                    text
                    type="error"
                    size="tiny"
                    @click="removeOrderAttachment(a, 'detail')"
                  >
                    删除
                  </n-button>
                </n-space>
                <n-image
                  width="120"
                  height="120"
                  object-fit="cover"
                  :src="resolveMediaUrl(a.url)"
                />
              </div>
            </n-space>
            <n-text v-else depth="3">暂无附件</n-text>
          </n-card>

          <n-card class="page-card order-detail-section-card" :bordered="false" title="还款记录">
            <n-data-table
              v-if="detail.repayments?.length"
              size="small"
              :pagination="false"
              striped
              :columns="repaymentDetailColumns"
              :data="detail.repayments"
            />
            <n-empty v-else description="暂无还款记录" size="small" />
          </n-card>

          <n-card class="page-card order-detail-section-card" :bordered="false" title="商品明细">
            <n-data-table
              class="order-items-table"
              size="small"
              :pagination="false"
              striped
              :columns="[
                { title: '商品名称', key: 'goodsName' },
                {
                  title: '品类',
                  key: 'categoryName',
                  render: (r) => r.categoryName || '-',
                },
                { title: '单价', key: 'sellingPrice', render: (r) => money(r.sellingPrice) },
                { title: '数量', key: 'quantity' },
                { title: '小计', key: 'subtotal', render: (r) => money(r.subtotal) },
              ]"
              :data="detail.items || []"
            />
          </n-card>
        </template>
      </n-drawer-content>
    </n-drawer>
  </n-space>
</template>

<style scoped>
.order-debt-summary-row {
  display: flex;
  gap: 12px;
  width: 100%;
  margin-bottom: 16px;
}
.order-debt-summary-row--drawer {
  flex-wrap: nowrap;
}
.order-debt-summary-cell {
  flex: 1;
  min-width: 0;
  padding: 14px 16px;
  border-radius: 8px;
  border: 1px solid #dce3ea;
  background: linear-gradient(135deg, #f8fafc 0%, #eef2f6 100%);
}
.order-debt-summary-cell--due .order-debt-summary-value {
  color: #1a5fb4;
}
.order-debt-summary-cell--paid .order-debt-summary-value {
  color: #0d7a52;
}
.order-debt-summary-cell--remain .order-debt-summary-value {
  color: #c45c00;
}
.order-debt-summary-label {
  font-size: 13px;
  font-weight: 600;
  color: #555;
  margin-bottom: 6px;
}
.order-debt-summary-value {
  font-size: 20px;
  font-weight: 700;
  line-height: 1.2;
}
.order-detail-descriptions :deep(.n-descriptions-table-wrapper) {
  border-radius: 6px;
  overflow: hidden;
}
.order-edit-att-chip {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}
</style>
