<script>
/** keep-alive include 按组件名匹配 */
export default { name: 'CashierView' }
</script>
<script setup>
/**
 * 对齐离线包与交付截图：省/市/区、镇/街道、村/社区均为浏览器原生 <select>；日期为 <input type="date">。
 * 地区与「地址管理」同源：`POST /region/all` 扁平化后省市区联动；镇/村按区县、镇编码懒加载 `GET /region/children`。
 * 结算请求：结构化 JSON（客户、地址、日期、区划编码、备注等独立字段）；remark 仅承载自由文本备注。
 */
import { computed, nextTick, onActivated, onMounted, reactive, ref, watch } from 'vue'
import { useMessage, NImage, NSwitch } from 'naive-ui'
import { get, post } from '@/utils/request'
import { resolveMediaUrl } from '@/utils/mediaUrl'
import { useCashierCacheStore } from '@/stores/cashierCache'
import {
  receiverCodesFromNames,
  namesFromRegionCodes,
  findDistrictCode,
  matchPcdPrefixFromAddress,
} from '@/utils/regionData'

const DRAFT_KEY = 'cashier_draft_v1'
const message = useMessage()

/** 赠送数量最多三位小数；购买数量、买满件数为整数 */
const GIFT_QTY_DECIMALS = 3
function roundGiftQty(n) {
  const x = Number(n)
  if (!Number.isFinite(x)) return 0
  const p = 10 ** GIFT_QTY_DECIMALS
  return Math.round(x * p) / p
}

/** 购买数量、买满件数：正整数袋 */
function intBagQty(n) {
  const x = Math.round(Number(n))
  if (!Number.isFinite(x)) return 1
  return Math.max(1, x)
}

function isPositiveIntBag(n) {
  const x = Math.round(Number(n))
  return Number.isFinite(x) && x >= 1
}

function isPositiveGiftQty(n) {
  return n != null && Number.isFinite(Number(n)) && Number(n) > 0
}

function pad2(n) {
  return String(n).padStart(2, '0')
}

/** 收银历史备注追加用：yyyy-MM-dd HH:mm:ss */
function formatLocalDateTime(d) {
  const x = d instanceof Date ? d : new Date(d)
  return `${x.getFullYear()}-${pad2(x.getMonth() + 1)}-${pad2(x.getDate())} ${pad2(x.getHours())}:${pad2(x.getMinutes())}:${pad2(x.getSeconds())}`
}

const cashierCache = useCashierCacheStore()

const loading = ref(false)
const keyword = ref('')
const goodsList = ref([])
const cart = reactive([])

/** 地区目录来自全局缓存（启动预热 / 地址管理同步） */
const flatRegions = computed(() => cashierCache.regionFlat)
const regionY = computed(() => cashierCache.regionY)
const selProvinceName = ref('')
const selCityName = ref('')
const selDistrictName = ref('')
const townOptions = ref([])
const villageOptions = ref([])
const selTownCode = ref('')
const selVillageCode = ref('')
let districtChildrenDebounce = null
const regionLoadError = ref('')
const regionsLoading = ref(false)
const syncingRegions = ref(false)
const memberDialogRef = ref(null)

const form = reactive({
  customerName: '',
  customerPhone: '',
  customerGender: 0,
  /** 镇/村名称由 <select> 与 /region/children 同步，用于拼接收货地址全文 */
  customerTown: '',
  customerVillage: '',
  customerDetailAddress: '',
  customerRemarkNew: '',
  orderRemark: '',
  repayDate: '',
  deliveryDate: '',
  manualRealAmount: null,
})

const memberHistoryRemark = ref('')
const selectedMember = ref(null)

const memberLoading = ref(false)
const memberQuery = reactive({ phone: '', name: '' })
const memberRows = ref([])

const genderOptions = [
  { label: '未知', value: 0 },
  { label: '男', value: 1 },
  { label: '女', value: 2 },
]

const provinceNameList = computed(() => Object.keys(regionY.value || {}))

const cityNameList = computed(() => {
  const p = selProvinceName.value
  if (!p || !regionY.value[p]) return []
  return Object.keys(regionY.value[p])
})

const districtNameList = computed(() => {
  const y = regionY.value
  const p = selProvinceName.value
  const c = selCityName.value
  if (!y[p] || !y[p][c]) return []
  return (y[p][c] || []).map((d) => d.name)
})

async function loadRegionCatalog() {
  regionLoadError.value = ''
  regionsLoading.value = true
  try {
    if (!cashierCache.regionFlat.length) {
      await cashierCache.fetchRegions()
    }
    regionLoadError.value =
      cashierCache.regionsError ||
      (cashierCache.regionFlat.length
        ? ''
        : '区域数据为空（请确认已登录、后端已启动，且 npm run dev 代理 /api）')
    ensureDefaultRegionSelection()
    await nextTick()
    void refreshTownVillageFromDistrict()
  } catch (e) {
    regionLoadError.value =
      cashierCache.regionsError ||
      e.message ||
      '区域数据加载失败（请确认已登录、后端已启动，且 npm run dev 代理 /api）'
  } finally {
    regionsLoading.value = false
  }
}

/** 默认地区：河南省周口市扶沟县（数据中不存在时回退第一项） */
function pickDefaultProvince(pk) {
  if (pk.includes('河南省')) return '河南省'
  if (pk.includes('北京市')) return '北京市'
  return pk[0]
}

function pickDefaultCity(ck) {
  if (ck.includes('周口市')) return '周口市'
  const prefer =
    ck.find((x) => x.includes('市辖区') || x === '市辖区') || ck.find((x) => x.endsWith('市')) || ck[0] || ''
  return prefer
}

function pickDefaultDistrict(ds) {
  if (ds.includes('扶沟县')) return '扶沟县'
  return ds[0] || ''
}

function ensureDefaultRegionSelection() {
  const y = regionY.value
  const pk = Object.keys(y)
  if (!pk.length) {
    selProvinceName.value = ''
    selCityName.value = ''
    selDistrictName.value = ''
    return
  }
  if (!selProvinceName.value || !pk.includes(selProvinceName.value)) {
    selProvinceName.value = pickDefaultProvince(pk)
  }
  const ck = Object.keys(y[selProvinceName.value] || {})
  if (!selCityName.value || !ck.includes(selCityName.value)) {
    selCityName.value = pickDefaultCity(ck)
  }
  const ds = (y[selProvinceName.value]?.[selCityName.value] || []).map((d) => d.name)
  if (!selDistrictName.value || !ds.includes(selDistrictName.value)) {
    selDistrictName.value = pickDefaultDistrict(ds)
  }
}

watch(selProvinceName, () => {
  if (syncingRegions.value) return
  const y = regionY.value
  const ck = Object.keys(y[selProvinceName.value] || {})
  selCityName.value = pickDefaultCity(ck)
  const ds = (y[selProvinceName.value]?.[selCityName.value] || []).map((d) => d.name)
  selDistrictName.value = pickDefaultDistrict(ds)
})

watch(selCityName, () => {
  if (syncingRegions.value) return
  const y = regionY.value
  const ds = (y[selProvinceName.value]?.[selCityName.value] || []).map((d) => d.name)
  selDistrictName.value = pickDefaultDistrict(ds)
})

/**
 * 区县编码：优先用省→市→区嵌套里自带的 code（与 DB parent_code 一致），
 * 避免仅用名称在扁平表里查找失败导致镇/村接口 parentId 为空。
 */
const districtCode = computed(() => {
  const y = regionY.value
  const pname = selProvinceName.value
  const cname = selCityName.value
  const dname = selDistrictName.value
  const rows = y[pname]?.[cname]
  if (Array.isArray(rows) && dname) {
    const hit = rows.find((d) => d.name === dname)
    if (hit?.code) return String(hit.code).trim()
  }
  return findDistrictCode(flatRegions.value, pname, cname, dname)
})

function normalizeChildrenList(raw) {
  if (Array.isArray(raw)) return raw
  if (raw && Array.isArray(raw.records)) return raw.records
  if (raw && Array.isArray(raw.list)) return raw.list
  return []
}

function mapChildOpts(raw) {
  return normalizeChildrenList(raw)
    .map((x) => {
      const v = x.value ?? x.code ?? x.id ?? x.regionCode
      const value = v != null && v !== '' ? String(v).trim() : ''
      const label = String(x.label ?? x.name ?? x.regionName ?? x.text ?? '').trim() || value
      return { value, label }
    })
    .filter((o) => o.value)
}

async function refreshTownVillageFromDistrict() {
  const code = districtCode.value
  selTownCode.value = ''
  selVillageCode.value = ''
  townOptions.value = []
  villageOptions.value = []
  form.customerTown = ''
  form.customerVillage = ''
  if (!code) return
  try {
    const raw = await get('/region/children', { parentId: code })
    /** 避免并发请求：仅应用仍对应当前区县的结果（取代 seq，防止慢请求被误丢弃） */
    if (districtCode.value !== code) return
    townOptions.value = mapChildOpts(raw)
  } catch {
    if (districtCode.value !== code) return
    townOptions.value = []
  }
}

async function refreshVillagesFromTown() {
  const town = selTownCode.value
  selVillageCode.value = ''
  form.customerVillage = ''
  villageOptions.value = []
  if (!town) return
  try {
    const raw = await get('/region/children', { parentId: town })
    if (selTownCode.value !== town) return
    villageOptions.value = mapChildOpts(raw)
  } catch {
    if (selTownCode.value !== town) return
    villageOptions.value = []
  }
}

function syncTownLabelFromSelection() {
  const o = townOptions.value.find((t) => t.value === selTownCode.value)
  form.customerTown = o?.label || ''
}

function syncVillageLabelFromSelection() {
  const o = villageOptions.value.find((t) => t.value === selVillageCode.value)
  form.customerVillage = o?.label || ''
}

watch(districtCode, () => {
  if (districtChildrenDebounce) clearTimeout(districtChildrenDebounce)
  districtChildrenDebounce = setTimeout(() => {
    districtChildrenDebounce = null
    void refreshTownVillageFromDistrict()
  }, 100)
})

watch(selTownCode, () => {
  syncTownLabelFromSelection()
  void refreshVillagesFromTown()
})

watch(selVillageCode, () => {
  syncVillageLabelFromSelection()
})

const receiverRegionCodes = computed(() => {
  const base = receiverCodesFromNames(
    flatRegions.value,
    selProvinceName.value,
    selCityName.value,
    selDistrictName.value,
  )
  const parts = base ? base.split(',').filter(Boolean) : []
  if (selTownCode.value) parts.push(selTownCode.value)
  if (selVillageCode.value) parts.push(selVillageCode.value)
  return parts.join(',')
})

const regionPrefixText = computed(
  () => `${selProvinceName.value || ''}${selCityName.value || ''}${selDistrictName.value || ''}`,
)

function openMemberSearch() {
  nextTick(() => memberDialogRef.value?.showModal())
}

function closeMemberSearch() {
  memberDialogRef.value?.close()
}

const groupedGoods = computed(() => {
  const m = new Map()
  for (const g of goodsList.value) {
    const cat = g.categoryName || '未分类'
    if (!m.has(cat)) m.set(cat, [])
    m.get(cat).push(g)
  }
  return [...m.entries()].map(([categoryName, items]) => ({ categoryName, items }))
})

const qtyMap = computed(() => {
  const map = new Map()
  for (const row of cart) map.set(row.id, Number(row.quantity || 0))
  return map
})

const totalAmount = computed(() =>
  cart.reduce((s, row) => s + Number(row.sellingPrice || 0) * Number(row.quantity || 0), 0),
)

/** 对齐约定应收：可手动改低；与后端 receivableAmount / discount_amount 一致 */
const realAmount = computed(() => {
  const I = totalAmount.value
  const m = form.manualRealAmount
  if (m === null || m === undefined || m === '') return Number(I.toFixed(2))
  const x = Number(m)
  if (!Number.isFinite(x)) return Number(I.toFixed(2))
  return Number(Math.max(0, Math.min(I, x)).toFixed(2))
})

const manualReduction = computed(() => Number((totalAmount.value - realAmount.value).toFixed(2)))

function qtyOf(id) {
  return qtyMap.value.get(id) || 0
}

function initDefaultDates() {
  const i = new Date()
  const repay = new Date(i)
  repay.setDate(repay.getDate() + 180)
  const delivery = new Date(i)
  delivery.setDate(delivery.getDate() + 1)
  form.repayDate = repay.toISOString().slice(0, 10)
  form.deliveryDate = delivery.toISOString().slice(0, 10)
}

function applyGoodsKeywordFilter() {
  const q = keyword.value.trim().toLowerCase()
  let list = cashierCache.goodsAll
  if (q) {
    list = list.filter((g) => {
      const name = String(g.name || '').toLowerCase()
      const initial = String(g.nameInitial ?? g.name_initial ?? '').toLowerCase()
      return name.includes(q) || initial.includes(q)
    })
  }
  goodsList.value = list
}

async function loadGoods() {
  loading.value = true
  try {
    if (!cashierCache.goodsAll.length) {
      await cashierCache.fetchGoodsOnShelf()
    }
    if (cashierCache.goodsError) {
      message.error(cashierCache.goodsError)
    }
    applyGoodsKeywordFilter()
    normalizeCartPromoDefaults()
  } catch (e) {
    message.error(e.message || cashierCache.goodsError || '商品加载失败')
  } finally {
    loading.value = false
  }
}

function goodsPromoDefaultsOk(row) {
  const b = row._promoBuyDefault != null && isPositiveIntBag(row._promoBuyDefault)
  const q = row._promoGiftDefault != null && isPositiveGiftQty(row._promoGiftDefault)
  return b && q
}

function normalizeCartPromoDefaults() {
  for (const row of cart) {
    if (row.promoApply === undefined) row.promoApply = false
    const g = cashierCache.goodsAll.find((x) => x.id === row.id)
    if (!g) continue
    if (row._promoBuyDefault === undefined) {
      row._promoBuyDefault = g.promoBuyQty != null ? intBagQty(g.promoBuyQty) : null
    }
    if (row._promoGiftDefault === undefined) {
      row._promoGiftDefault = g.promoGiftQty != null ? roundGiftQty(g.promoGiftQty) : null
    }
  }
}

/** 用当前上架商品列表刷新购物车行上的商品属性（保留数量与买赠开关/覆盖值） */
function syncCartLinesFromCatalog() {
  for (const row of cart) {
    const g = cashierCache.goodsAll.find((x) => x.id === row.id)
    if (!g) continue
    row.name = g.name
    row.sellingPrice = g.sellingPrice
    row.unit = g.unit
    row.image = g.image
    row.barcode = g.barcode
    row.categoryName = g.categoryName
    row.nameInitial = g.nameInitial ?? g.name_initial
    row.promoEnabled = g.promoEnabled
    row._promoBuyDefault = g.promoBuyQty != null ? intBagQty(g.promoBuyQty) : null
    row._promoGiftDefault = g.promoGiftQty != null ? roundGiftQty(g.promoGiftQty) : null
    row.quantity = intBagQty(row.quantity)
    if (Number(g.promoEnabled) !== 1) {
      row.promoApply = false
      row.promoBuyQty = null
      row.promoGiftQty = null
    } else {
      if (row.promoBuyQty != null) row.promoBuyQty = intBagQty(row.promoBuyQty)
      if (row.promoGiftQty != null) row.promoGiftQty = roundGiftQty(row.promoGiftQty)
    }
  }
}

function addToCart(g) {
  const row = cart.find((x) => x.id === g.id)
  if (row) row.quantity += 1
  else
    cart.push({
      ...g,
      quantity: 1,
      _promoBuyDefault: g.promoBuyQty != null ? intBagQty(g.promoBuyQty) : null,
      _promoGiftDefault: g.promoGiftQty != null ? roundGiftQty(g.promoGiftQty) : null,
      promoBuyQty: null,
      promoGiftQty: null,
      promoApply: false,
    })
}

function removeFromCart(id) {
  const i = cart.findIndex((x) => x.id === id)
  if (i >= 0) cart.splice(i, 1)
}

function clearCart() {
  cart.splice(0, cart.length)
}

async function searchMembers() {
  memberLoading.value = true
  try {
    const data = await post('/member/page', {
      pageNum: 1,
      pageSize: 20,
      phone: memberQuery.phone || undefined,
      name: memberQuery.name || undefined,
    })
    memberRows.value = data.records || []
  } catch (e) {
    message.error(e.message || '查询客户失败')
  } finally {
    memberLoading.value = false
  }
}

async function onPhoneBlur() {
  if (!form.customerPhone?.trim()) {
    memberHistoryRemark.value = ''
    return
  }
  try {
    const data = await post('/member/page', {
      pageNum: 1,
      pageSize: 5,
      phone: form.customerPhone.trim(),
    })
    const recs = data.records || []
    if (recs.length === 1) await applyMember(recs[0])
    else memberHistoryRemark.value = ''
  } catch {
    memberHistoryRemark.value = ''
  }
}

/**
 * 会员 address 存的是「收货地址全文」；界面由省/市/区 + 镇/村 + 详细拼接，
 * 回填详细框时需去掉与当前下拉、镇村文案重复的前缀，避免省市区镇街出现两遍。
 */
function detailOnlyForRegionForm(savedFull) {
  const s = String(savedFull ?? '').trim()
  if (!s) return ''
  const stripIfStarts = (text, prefix) => {
    const p = String(prefix ?? '').trim()
    if (!p || !text.startsWith(p)) return text
    return text.slice(p.length).trim()
  }
  let rest = s
  rest = stripIfStarts(rest, regionPrefixText.value)
  rest = stripIfStarts(rest, form.customerTown)
  rest = stripIfStarts(rest, form.customerVillage)
  return rest.length ? rest : s
}

/**
 * 根据 /region/children 返回的镇、村选项，从 remainder 前缀匹配并写入下拉与详细地址。
 */
async function matchAndSetTownVillageFromRemainder(remainder) {
  let rem = String(remainder ?? '').trim()
  selTownCode.value = ''
  selVillageCode.value = ''
  form.customerTown = ''
  form.customerVillage = ''
  villageOptions.value = []

  const townsSorted = [...townOptions.value].sort((a, b) => String(b.label).length - String(a.label).length)
  for (const t of townsSorted) {
    const lab = String(t.label ?? '').trim()
    if (!lab || !rem.startsWith(lab)) continue
    selTownCode.value = t.value
    await nextTick()
    await refreshVillagesFromTown()
    syncTownLabelFromSelection()
    rem = rem.slice(lab.length).trim()
    break
  }

  const villagers = [...villageOptions.value].sort((a, b) => String(b.label).length - String(a.label).length)
  for (const v of villagers) {
    const lab = String(v.label ?? '').trim()
    if (!lab || !rem.startsWith(lab)) continue
    selVillageCode.value = v.value
    await nextTick()
    syncVillageLabelFromSelection()
    rem = rem.slice(lab.length).trim()
    break
  }

  form.customerDetailAddress = rem
}

async function applyMember(row) {
  selectedMember.value = row
  form.customerPhone = row.phone || ''
  form.customerName = row.name || form.customerName
  form.customerGender = row.gender ?? 0
  memberHistoryRemark.value = String(row.remark ?? '').trim()

  const addr = String(row.address ?? '').trim()
  if (!addr) {
    selTownCode.value = ''
    selVillageCode.value = ''
    form.customerTown = ''
    form.customerVillage = ''
    villageOptions.value = []
    form.customerDetailAddress = ''
    closeMemberSearch()
    return
  }

  const parsed = matchPcdPrefixFromAddress(flatRegions.value, addr)
  if (parsed) {
    syncingRegions.value = true
    try {
      selProvinceName.value = parsed.provinceName
      selCityName.value = parsed.cityName
      selDistrictName.value = parsed.districtName
    } finally {
      syncingRegions.value = false
    }
    await nextTick()
    // 避免 watch(districtCode) 的 100ms 防抖再次 refresh 清空刚回填的镇/村
    if (districtChildrenDebounce) {
      clearTimeout(districtChildrenDebounce)
      districtChildrenDebounce = null
    }
    await refreshTownVillageFromDistrict()
    await matchAndSetTownVillageFromRemainder(parsed.remainder)
  } else {
    selTownCode.value = ''
    selVillageCode.value = ''
    form.customerTown = ''
    form.customerVillage = ''
    villageOptions.value = []
    form.customerDetailAddress = detailOnlyForRegionForm(addr)
  }

  closeMemberSearch()
}

/** 输入框姓名优先；未填时回落到已选会员（避免仅绑定会员但 reactive 未同步时误判为空） */
function effectiveCustomerName() {
  const typed = String(form.customerName ?? '').trim()
  if (typed) return typed
  const fromMember = selectedMember.value?.name
  return fromMember != null ? String(fromMember).trim() : ''
}

function syncCustomerNameFromMemberIfNeeded() {
  if (String(form.customerName ?? '').trim()) return
  const n = selectedMember.value?.name
  if (n != null && String(n).trim()) form.customerName = String(n).trim()
}

async function onSettle() {
  if (!cart.length) {
    message.warning('请先添加商品')
    return
  }
  syncCustomerNameFromMemberIfNeeded()
  const customerNameOk = effectiveCustomerName()
  const addressOk = fullReceiverAddress.value.trim()
  if (!customerNameOk) {
    message.warning('请填写客户姓名')
    return
  }
  if (!addressOk) {
    message.warning('请填写收货地址（含省市区镇村或详细地址）')
    return
  }

  for (const c of cart) {
    if (!isPositiveIntBag(c.quantity)) {
      message.warning(`商品「${c.name || ''}」购买数量须为不小于 1 的整数（袋）`)
      return
    }
  }

  for (const c of cart) {
    if (Number(c.promoEnabled) !== 1) continue
    if (!c.promoApply) continue
    if (!goodsPromoDefaultsOk(c)) {
      const b = c.promoBuyQty != null && isPositiveIntBag(c.promoBuyQty)
      const g = c.promoGiftQty != null && isPositiveGiftQty(c.promoGiftQty)
      if (!b || !g) {
        message.warning(
          `商品「${c.name || ''}」未配置默认买满/赠送件数，参与买满送时请填写「买满件数」（整数）与「赠送数量」（可小数）`,
        )
        return
      }
    }
  }

  loading.value = true
  try {
    const pricingNote =
      manualReduction.value > 0.009
        ? `约定应收 ${realAmount.value.toFixed(2)} 元（商品总额 ${totalAmount.value.toFixed(2)}）`
        : undefined
    const res = await post('/order/settle', {
      memberId: selectedMember.value?.id ?? undefined,
      memberCardNo: selectedMember.value?.phone || selectedMember.value?.cardNo || undefined,
      payType: 0,
      receivableAmount: realAmount.value,
      items: cart.map((c) => {
        const linePromo = Number(c.promoEnabled) === 1 && !!c.promoApply
        const ob =
          linePromo && c.promoBuyQty != null && isPositiveIntBag(c.promoBuyQty)
            ? intBagQty(c.promoBuyQty)
            : undefined
        const og =
          linePromo && c.promoGiftQty != null && isPositiveGiftQty(c.promoGiftQty)
            ? roundGiftQty(c.promoGiftQty)
            : undefined
        return {
          goodsId: c.id,
          quantity: intBagQty(c.quantity),
          promoEnabled: linePromo,
          promoBuyQty: ob,
          promoGiftQty: og,
        }
      }),
      customerName: effectiveCustomerName() || undefined,
      customerPhone: String(form.customerPhone ?? '').trim() || undefined,
      customerAddress: fullReceiverAddress.value.trim(),
      customerGender: form.customerGender,
      repayDate: form.repayDate || undefined,
      deliveryDate: form.deliveryDate || undefined,
      regionCodes: receiverRegionCodes.value?.trim() || undefined,
      customerRemark: form.customerRemarkNew?.trim() || undefined,
      orderRemark: form.orderRemark?.trim() || undefined,
      pricingNote,
    })
    message.success(`订单已提交，订单号：${res.orderNo}`)
    const customerRmkAdded = String(form.customerRemarkNew ?? '').trim()
    if (customerRmkAdded) {
      const line = `${formatLocalDateTime(new Date())} ${customerRmkAdded}`
      const prev = String(memberHistoryRemark.value ?? '').trim()
      memberHistoryRemark.value = prev ? `${prev}\n${line}` : line
    }
    clearCart()
    selectedMember.value = null
    Object.assign(form, {
      customerName: '',
      customerPhone: '',
      customerGender: 0,
      customerTown: '',
      customerVillage: '',
      customerDetailAddress: '',
      customerRemarkNew: '',
      orderRemark: '',
      manualRealAmount: null,
    })
    syncingRegions.value = true
    try {
      selProvinceName.value = ''
      selCityName.value = ''
      selDistrictName.value = ''
      selTownCode.value = ''
      selVillageCode.value = ''
      townOptions.value = []
      villageOptions.value = []
      ensureDefaultRegionSelection()
    } finally {
      syncingRegions.value = false
    }
    initDefaultDates()
    localStorage.removeItem(DRAFT_KEY)
    await cashierCache.refreshGoodsOnShelf()
    applyGoodsKeywordFilter()
  } catch (e) {
    message.error(e.message || '结算失败')
  } finally {
    loading.value = false
  }
}

const fullReceiverAddress = computed(
  () =>
    `${regionPrefixText.value}${form.customerTown || ''}${form.customerVillage || ''}${form.customerDetailAddress || ''}`,
)

function saveDraft() {
  localStorage.setItem(
    DRAFT_KEY,
    JSON.stringify({
      keyword: keyword.value,
      cart: [...cart],
      memberHistoryRemark: memberHistoryRemark.value,
      settleForm: { ...form },
      regionNames: {
        province: selProvinceName.value,
        city: selCityName.value,
        district: selDistrictName.value,
      },
      selTownCode: selTownCode.value,
      selVillageCode: selVillageCode.value,
      selectedMemberSnap: selectedMember.value
        ? {
            id: selectedMember.value.id,
            name: selectedMember.value.name,
            phone: selectedMember.value.phone,
            cardNo: selectedMember.value.cardNo,
          }
        : null,
    }),
  )
}

async function loadDraft() {
  try {
    const raw = localStorage.getItem(DRAFT_KEY)
    if (!raw) return
    const d = JSON.parse(raw)
    keyword.value = d.keyword || ''
    if (Array.isArray(d.cart)) cart.splice(0, cart.length, ...d.cart)
    if (d.selectedMemberSnap && typeof d.selectedMemberSnap === 'object' && d.selectedMemberSnap.id) {
      selectedMember.value = { ...d.selectedMemberSnap }
    }
    if (typeof d.memberHistoryRemark === 'string') {
      memberHistoryRemark.value = d.memberHistoryRemark
    }
    if (d.settleForm && typeof d.settleForm === 'object') {
      Object.assign(form, d.settleForm)
      delete form.promoEnabled
    }
    if (d.settleForm?.customerAddress && !d.settleForm?.customerDetailAddress) {
      form.customerDetailAddress = d.settleForm.customerAddress
    }
    syncingRegions.value = true
    try {
      if (d.regionNames && typeof d.regionNames === 'object') {
        selProvinceName.value = d.regionNames.province || ''
        selCityName.value = d.regionNames.city || ''
        selDistrictName.value = d.regionNames.district || ''
      } else if (d.selProvince) {
        const { provinceName, cityName, districtName } = namesFromRegionCodes(
          flatRegions.value,
          [d.selProvince, d.selCity, d.selDistrict].filter(Boolean).join(','),
        )
        selProvinceName.value = provinceName
        selCityName.value = cityName
        selDistrictName.value = districtName
      }
      ensureDefaultRegionSelection()
    } finally {
      syncingRegions.value = false
    }
    await nextTick()
    await refreshTownVillageFromDistrict()
    const town = d.selTownCode
    const vill = d.selVillageCode
    if (town && townOptions.value.some((t) => t.value === town)) {
      selTownCode.value = town
      await nextTick()
      await refreshVillagesFromTown()
      if (vill && villageOptions.value.some((v) => v.value === vill)) selVillageCode.value = vill
    }
    syncTownLabelFromSelection()
    syncVillageLabelFromSelection()
    normalizeCartPromoDefaults()
  } catch {
    /* ignore */
  }
}

watch(keyword, () => {
  if (cashierCache.goodsAll.length) applyGoodsKeywordFilter()
})

watch(
  () => ({
    keyword: keyword.value,
    cart: [...cart],
    form: { ...form },
    selProvinceName: selProvinceName.value,
    selCityName: selCityName.value,
    selDistrictName: selDistrictName.value,
    selTownCode: selTownCode.value,
    selVillageCode: selVillageCode.value,
    memberId: selectedMember.value?.id ?? null,
  }),
  saveDraft,
  { deep: true },
)

/** 首次进入已由 onMounted 拉取商品，跳过一次 onActivated 内的重复请求 */
const skipGoodsRefreshOnNextActivate = ref(true)

onActivated(async () => {
  if (skipGoodsRefreshOnNextActivate.value) {
    skipGoodsRefreshOnNextActivate.value = false
    syncCartLinesFromCatalog()
    applyGoodsKeywordFilter()
    normalizeCartPromoDefaults()
    return
  }
  try {
    await cashierCache.refreshGoodsOnShelf()
  } catch {
    /* ignore */
  }
  syncCartLinesFromCatalog()
  applyGoodsKeywordFilter()
  normalizeCartPromoDefaults()
})

initDefaultDates()

onMounted(async () => {
  await loadRegionCatalog()
  await loadGoods()
  await loadDraft()
})
</script>

<template>
  <n-grid :cols="24" :x-gap="16" class="cashier-page">
    <n-gi :span="17" class="cashier-left-col">
      <n-card class="page-card" :bordered="false" title="快捷收银">
        <n-space vertical size="large">
          <n-space align="center">
            <n-input
              v-model:value="keyword"
              style="width: 460px"
              placeholder="搜索商品名称或首字母（如：尿素 / ns）"
              @keyup.enter="loadGoods"
            />
            <n-button @click="loadGoods">搜索</n-button>
          </n-space>

          <n-space vertical>
            <template v-for="block in groupedGoods" :key="block.categoryName">
              <div class="category-title">{{ block.categoryName }}</div>
              <n-grid :cols="4" :x-gap="12" :y-gap="12">
                <n-gi v-for="g in block.items" :key="g.id">
                  <n-card hoverable class="goods-card" @click="addToCart(g)">
                    <div class="goods-img-wrap" @click.stop>
                      <n-image
                        v-if="g.image"
                        :src="resolveMediaUrl(g.image)"
                        class="goods-img"
                        :img-props="{ style: { width: '100%', height: '100%', objectFit: 'cover' } }"
                        object-fit="cover"
                      />
                      <div v-else class="goods-img-placeholder">暂无图片</div>
                    </div>
                    <div class="goods-card-title">{{ g.name }}</div>
                    <div class="money">
                      ￥{{ Number(g.sellingPrice || 0).toFixed(2) }} / {{ g.unit || '-' }}
                    </div>
                    <div class="muted">库存：{{ g.stock }} {{ g.unit || '' }}</div>
                    <div v-if="qtyOf(g.id) > 0" class="goods-selected-badge">已选 {{ qtyOf(g.id) }}</div>
                  </n-card>
                </n-gi>
              </n-grid>
            </template>
          </n-space>
        </n-space>
      </n-card>
    </n-gi>

    <n-gi :span="7" class="cashier-right-col">
      <div class="cashier-right-stack">
        <n-card
          class="page-card cart-card"
          :class="{ 'cart-card--empty': !cart.length }"
          :bordered="false"
          title="购物车"
        >
          <div class="cart-body">
            <n-empty v-if="!cart.length" description="暂无添加商品" />
            <template v-else>
              <div class="cart-toolbar">
                <n-button quaternary type="error" size="small" @click="clearCart">清空购物车</n-button>
              </div>
              <div v-for="row in cart" :key="row.id" class="cart-item">
                <button type="button" class="delete-x-btn" @click="removeFromCart(row.id)">×</button>
                <div class="cart-item-main">
                  <div class="cart-meta">
                    <div class="cart-item-title">{{ row.name }}</div>
                    <div
                      v-if="Number(row.promoEnabled) === 1"
                      class="cart-promo-block"
                      @click.stop
                    >
                      <div class="cart-promo-head">
                        <span class="muted">同款买满送</span>
                        <n-switch v-model:value="row.promoApply" size="small" />
                      </div>
                      <div v-if="row.promoApply" class="cart-promo-fields">
                        <p v-if="goodsPromoDefaultsOk(row)" class="cart-promo-hint muted">
                          已设默认：满 {{ row._promoBuyDefault }} 袋送 {{ row._promoGiftDefault }}（可留空使用默认）
                        </p>
                        <p v-else class="cart-promo-hint muted">未设默认买满/赠送，请填写下列两项</p>
                        <div class="cart-promo-field">
                          <span class="cart-promo-field-label">买满件数</span>
                          <n-input-number
                            v-model:value="row.promoBuyQty"
                            class="cart-promo-input-full"
                            :min="1"
                            :step="1"
                            :precision="0"
                            size="small"
                            :placeholder="goodsPromoDefaultsOk(row) ? '默认' : '必填'"
                            :clearable="goodsPromoDefaultsOk(row)"
                          />
                        </div>
                        <div class="cart-promo-field">
                          <span class="cart-promo-field-label">赠送数量</span>
                          <n-input-number
                            v-model:value="row.promoGiftQty"
                            class="cart-promo-input-full"
                            :min="0.001"
                            :step="0.1"
                            :precision="3"
                            size="small"
                            :placeholder="goodsPromoDefaultsOk(row) ? '默认' : '必填'"
                            :clearable="goodsPromoDefaultsOk(row)"
                          />
                        </div>
                      </div>
                    </div>
                  </div>
                    <n-space align="center" justify="space-between" wrap class="cart-item-qty-row">
                    <n-input-number
                      v-model:value="row.quantity"
                      class="cart-qty-input"
                      :min="1"
                      :step="1"
                      :precision="0"
                      size="small"
                    />
                    <span class="money">
                      ￥{{ (Number(row.quantity) * Number(row.sellingPrice || 0)).toFixed(2) }}
                    </span>
                  </n-space>
                </div>
              </div>
            </template>
          </div>
        </n-card>

        <n-card class="page-card glass customer-card" :bordered="false" title="客户与账期订单">
          <div class="customer-card-body">
            <n-space justify="space-between" style="width: 100%">
              <n-text depth="3">可通过客户电话/姓名查询并回填已有客户信息</n-text>
              <n-button size="small" @click="openMemberSearch">查询已有客户</n-button>
            </n-space>

            <n-alert v-if="regionLoadError" type="warning" style="margin-bottom: 8px" :title="regionLoadError">
              省市区与「地址管理」同源；镇/街道、村/社区由后端区域树懒加载（与地址库一致）。
            </n-alert>

            <!-- 纯原生表单，避免 Naive n-form-item 与原生 select 叠出「双下拉」观感 -->
            <div class="cashier-native-form">
              <div class="cn-row">
                <label class="cn-label" for="cn-phone">客户电话（可选）</label>
                <input
                  id="cn-phone"
                  v-model="form.customerPhone"
                  type="text"
                  class="cn-input"
                  placeholder="选填，可自动匹配会员"
                  autocomplete="tel"
                  @blur="onPhoneBlur"
                />
              </div>
              <div class="cn-row">
                <label class="cn-label" for="cn-name">客户姓名</label>
                <input id="cn-name" v-model="form.customerName" type="text" class="cn-input" placeholder="客户姓名" />
              </div>
              <div class="cn-row">
                <label class="cn-label" for="cn-gender">客户性别</label>
                <select id="cn-gender" v-model.number="form.customerGender" class="cn-select">
                  <option v-for="g in genderOptions" :key="g.value" :value="g.value">{{ g.label }}</option>
                </select>
              </div>
              <div class="cn-row cn-row--top cn-row--address">
                <label class="cn-label">客户地址</label>
                <div class="cn-address-cell">
                  <span v-if="regionsLoading" class="muted cn-loading-hint">地区数据加载中…</span>
                  <div class="cn-region-cascade">
                    <div class="cn-region-line cn-region-line--3">
                      <select v-model="selProvinceName" class="cn-select cn-select--cascade" title="省 / 直辖市">
                        <option value="">省</option>
                        <option v-for="pn in provinceNameList" :key="pn" :value="pn">{{ pn }}</option>
                      </select>
                      <select
                        v-model="selCityName"
                        class="cn-select cn-select--cascade"
                        title="市"
                        :disabled="!selProvinceName"
                      >
                        <option value="">市</option>
                        <option v-for="cn in cityNameList" :key="cn" :value="cn">{{ cn }}</option>
                      </select>
                      <select
                        v-model="selDistrictName"
                        class="cn-select cn-select--cascade"
                        title="区 / 县"
                        :disabled="!selCityName || !districtNameList.length"
                      >
                        <option value="">区/县</option>
                        <option v-for="dn in districtNameList" :key="dn" :value="dn">{{ dn }}</option>
                      </select>
                    </div>
                    <div class="cn-region-line cn-region-line--2">
                      <select
                        id="cn-town-sel"
                        v-model="selTownCode"
                        class="cn-select cn-select--cascade"
                        title="镇/街道"
                        :disabled="!districtCode"
                      >
                        <option value="">{{ districtCode ? '请选择镇/街道' : '请先选区/县' }}</option>
                        <option v-for="o in townOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
                      </select>
                      <select
                        id="cn-village-sel"
                        v-model="selVillageCode"
                        class="cn-select cn-select--cascade"
                        title="村/社区"
                        :disabled="!selTownCode"
                      >
                        <option value="">{{ selTownCode ? '请选择村/社区' : '请先选镇/街道' }}</option>
                        <option v-for="o in villageOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
                      </select>
                    </div>
                  </div>
                </div>
              </div>
              <div class="cn-row">
                <label class="cn-label" for="cn-detail">详细地址</label>
                <input
                  id="cn-detail"
                  v-model="form.customerDetailAddress"
                  type="text"
                  class="cn-input"
                  placeholder="请输入门牌号等详细地址"
                  autocomplete="street-address"
                />
              </div>
              <div class="cn-row">
                <label class="cn-label" for="cn-repay">还款日期</label>
                <input id="cn-repay" v-model="form.repayDate" type="date" class="cn-input cn-input--date" />
              </div>
              <div class="cn-row">
                <label class="cn-label" for="cn-delivery">送货日期</label>
                <input id="cn-delivery" v-model="form.deliveryDate" type="date" class="cn-input cn-input--date" />
              </div>
              <div class="cn-row cn-row--top">
                <label class="cn-label" for="cn-amt">应收金额</label>
                <div class="cn-amt-wrap">
                  <n-input-number
                    id="cn-amt"
                    v-model:value="form.manualRealAmount"
                    class="cn-input-number-amt"
                    :min="0"
                    :max="totalAmount"
                    :precision="2"
                    :step="0.01"
                    clearable
                    placeholder="默认自动等于总额"
                  />
                  <p class="cn-hint">可手动修改，不填则默认等于总额</p>
                </div>
              </div>
              <div class="cn-row cn-row--top">
                <label class="cn-label" for="cn-hist">历史备注</label>
                <div class="cn-remark-wrap">
                  <textarea
                    id="cn-hist"
                    class="cn-textarea cn-textarea--readonly-hist cn-textarea--hist-box"
                    rows="6"
                    readonly
                    disabled
                    :value="memberHistoryRemark || '无'"
                  />
                  <p class="cn-hint">置灰只读，不可修改</p>
                </div>
              </div>
              <div class="cn-row cn-row--top">
                <label class="cn-label" for="cn-newrmk">新增备注</label>
                <div class="cn-remark-wrap">
                  <input
                    id="cn-newrmk"
                    v-model="form.customerRemarkNew"
                    type="text"
                    class="cn-input cn-input--gray-muted"
                    placeholder="仅填写本次新增备注内容"
                  />
                  <p class="cn-hint">提交后自动按 yyyy-MM-dd HH:mm:ss 追加到历史备注</p>
                </div>
              </div>
              <div class="cn-row cn-row--top">
                <label class="cn-label" for="cn-orderrmk">订单备注</label>
                <div class="cn-remark-wrap">
                  <input
                    id="cn-orderrmk"
                    v-model="form.orderRemark"
                    type="text"
                    class="cn-input"
                    placeholder="请填写订单备注"
                  />
                  <p class="cn-hint">订单附加说明</p>
                </div>
              </div>
            </div>

            <div class="amount-cards">
              <div class="amount-card total-card">
                <div class="amount-label">总金额</div>
                <div class="amount-value">{{ totalAmount.toFixed(2) }}</div>
              </div>
              <div class="amount-card discount-card">
                <div class="amount-label">减免</div>
                <div class="amount-value">{{ manualReduction.toFixed(2) }}</div>
              </div>
              <div class="amount-card real-card">
                <div class="amount-label">应收</div>
                <div class="amount-value">{{ realAmount.toFixed(2) }}</div>
              </div>
            </div>

            <n-button type="primary" block size="medium" :loading="loading" @click="onSettle">
              提交未支付订单
            </n-button>
            <n-text depth="3" style="font-size: 12px">
              与交付版界面一致；当前接口为正式结算（现金），账期/欠款需后端扩展后对接。
            </n-text>
          </div>
        </n-card>
      </div>
    </n-gi>
  </n-grid>

  <dialog ref="memberDialogRef" class="native-member-dialog" @close="() => {}">
    <div class="native-member-dialog__inner">
      <div class="native-member-dialog__head">
        <strong>查询已有客户</strong>
        <button type="button" class="native-member-dialog__x" @click="closeMemberSearch">×</button>
      </div>
      <div class="native-member-dialog__toolbar">
        <n-input v-model:value="memberQuery.phone" placeholder="按电话" style="width: 200px" />
        <n-input v-model:value="memberQuery.name" placeholder="按姓名" style="width: 200px" />
        <n-button type="primary" :loading="memberLoading" @click="searchMembers">查询</n-button>
      </div>
      <div v-if="memberLoading" class="muted" style="padding: 12px">加载中…</div>
      <table v-else class="native-member-table">
        <thead>
          <tr>
            <th>姓名</th>
            <th>电话</th>
            <th>卡号</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in memberRows" :key="row.id">
            <td>{{ row.name }}</td>
            <td>{{ row.phone }}</td>
            <td>{{ row.cardNo }}</td>
            <td>
              <a href="#" class="link-use" @click.prevent="void applyMember(row)">使用该客户</a>
            </td>
          </tr>
          <tr v-if="!memberRows.length">
            <td colspan="4" class="muted" style="text-align: center; padding: 16px">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>
  </dialog>
</template>

<style scoped>
.goods-card {
  position: relative;
  cursor: pointer;
  border-radius: 14px;
  transition: transform 0.08s ease, box-shadow 0.12s ease;
}
.goods-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 26px rgba(15, 23, 42, 0.1);
}
.goods-img {
  width: 100%;
  height: 96px;
  object-fit: cover;
  border-radius: 10px;
}
.goods-img-wrap {
  width: 100%;
  height: 96px;
  margin-bottom: 8px;
}
.goods-img-placeholder {
  width: 100%;
  height: 96px;
  border-radius: 10px;
  border: 1px dashed #d1d5db;
  background: #f9fafb;
  color: #9ca3af;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}
.goods-card-title {
  font-weight: 700;
  margin-bottom: 8px;
}
.goods-selected-badge {
  position: absolute;
  right: 12px;
  bottom: 10px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #dc2626;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.4;
}
/* 左侧仅商品区滚动；右侧购物车+客户区随内容增高，不在此列内再套一层纵向滚动条 */
.cashier-page {
  align-items: start;
}
.cashier-left-col {
  min-width: 0;
  max-height: calc(100vh - 168px);
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 8px;
}
.cashier-right-col {
  min-width: 0;
}
.cashier-right-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: sticky;
  top: 12px;
  align-self: start;
  width: 100%;
}
.cashier-right-stack :deep(.n-card) {
  border-radius: 14px;
}
.cart-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.cart-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 6px;
}
.cart-promo-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 6px;
}
.cart-promo-block {
  width: 100%;
  max-width: 100%;
}
.cart-promo-fields {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
  width: 100%;
}
.cart-promo-hint {
  font-size: 12px;
  line-height: 1.4;
  margin: 0;
}
.cart-promo-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
}
.cart-promo-field-label {
  font-size: 12px;
  color: #64748b;
  flex-shrink: 0;
}
.cart-promo-input-full {
  width: 100% !important;
  min-width: 0;
}
.cart-promo-input-full :deep(.n-input) {
  min-width: 0;
}
.cart-promo-input-full :deep(.n-input__input-el) {
  text-align: left;
  min-width: 4.5rem;
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}
.cart-card :deep(.n-card__content) {
  padding-top: 8px;
}
.cart-card--empty :deep(.n-card__content) {
  min-height: 120px;
}
.customer-card-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.category-title {
  margin: 8px 0 10px;
  font-size: 14px;
  font-weight: 700;
  color: #334155;
}
.cart-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
  padding: 12px 28px 12px 0;
  border-bottom: 1px solid #eef2f7;
}
.cart-item-main {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  min-width: 0;
}
.cart-item-title {
  font-weight: 600;
  line-height: 1.35;
  word-break: break-word;
}
.cart-item-qty-row {
  flex-shrink: 0;
  align-self: stretch;
  width: 100%;
}
.cart-item-qty-row :deep(.n-space) {
  width: 100%;
}
.cart-item-qty-row :deep(.n-space-item:first-child) {
  flex: 1 1 auto;
  min-width: 0;
  max-width: 100%;
}
.cart-meta {
  padding-right: 18px;
  min-width: 0;
  flex: 1;
}
.delete-x-btn {
  position: absolute;
  right: 0;
  top: 6px;
  width: 18px;
  height: 18px;
  border-radius: 999px;
  border: none;
  background: #ef4444;
  color: #fff;
  font-size: 14px;
  line-height: 18px;
  cursor: pointer;
  z-index: 2;
}
.cart-qty-input {
  width: 100%;
  min-width: 132px;
  max-width: 220px;
}
.cart-qty-input :deep(.n-input) {
  min-width: 0;
}
.cart-qty-input :deep(.n-input__input-el) {
  min-width: 5.5rem;
  font-size: 13px;
  font-variant-numeric: tabular-nums;
  text-align: left;
}
/* 收银台右侧：原生表单（与浏览器默认 select/input 一致） */
.cashier-native-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-size: 13px;
  color: #303133;
}
.cn-row {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr);
  align-items: center;
  gap: 8px 10px;
}
.cn-row--top {
  align-items: start;
}
.cn-row--address .cn-label {
  padding-top: 7px;
}
.cn-label {
  color: #606266;
  text-align: right;
  padding-right: 2px;
  flex-shrink: 0;
}
.cn-input,
.cn-textarea {
  width: 100%;
  box-sizing: border-box;
  font: inherit;
  font-size: 13px;
  color: #303133;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 6px 10px;
  background: #fff;
}
.cn-textarea {
  resize: vertical;
  min-height: 52px;
}
.cn-textarea--gray-muted,
.cn-input--gray-muted {
  background: #f4f4f5;
  border-color: #e4e7ed;
  color: #606266;
}
.cn-textarea--readonly-hist:disabled {
  opacity: 1;
  -webkit-text-fill-color: #909399;
  color: #909399;
  background: #f4f4f5;
  cursor: not-allowed;
}
.cn-textarea--hist-box {
  min-height: 140px;
  line-height: 1.5;
}
.cn-select {
  width: 100%;
  box-sizing: border-box;
  font: inherit;
  font-size: 13px;
  color: #303133;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 4px 6px;
  min-height: 32px;
  background: #fff;
  appearance: auto;
  -webkit-appearance: menulist;
}
.cn-region-cascade {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}
.cn-region-line {
  display: grid;
  gap: 5px;
  width: 100%;
}
.cn-region-line--3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}
.cn-region-line--2 {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}
.cn-select--cascade {
  min-width: 0;
  width: 100%;
  font-size: 12px;
  padding: 4px 6px;
  min-height: 30px;
}
.cn-address-cell {
  min-width: 0;
}
.cn-loading-hint {
  display: block;
  margin-bottom: 6px;
}
.cn-hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}
.cn-amt-wrap,
.cn-remark-wrap {
  min-width: 0;
}
.cn-input-number-amt {
  width: 100%;
}
.cn-input-number-amt :deep(.n-input) {
  border-radius: 4px;
}
.cn-input--date {
  min-height: 32px;
}
.amount-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}
.amount-card {
  border-radius: 12px;
  padding: 10px 8px;
  text-align: center;
  border: 2px solid transparent;
  min-width: 0;
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.18);
}
.total-card {
  background: linear-gradient(155deg, #2563eb 0%, #1d4ed8 48%, #1e40af 100%);
  border-color: #172554;
}
.discount-card {
  background: linear-gradient(155deg, #ea580c 0%, #c2410c 48%, #9a3412 100%);
  border-color: #7c2d12;
}
.real-card {
  background: linear-gradient(155deg, #ef4444 0%, #dc2626 48%, #b91c1c 100%);
  border-color: #7f1d1d;
}
.amount-label {
  font-size: 11px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.88);
  margin-bottom: 5px;
}
.amount-value {
  font-size: 17px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: #fff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.22);
}
.muted {
  color: #64748b;
  font-size: 12px;
}
.money {
  font-weight: 700;
  color: #f56c6c;
}

.native-member-dialog {
  border: none;
  border-radius: 8px;
  padding: 0;
  max-width: 760px;
  width: min(92vw, 760px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
}
.native-member-dialog::backdrop {
  background: rgba(0, 0, 0, 0.35);
}
.native-member-dialog__inner {
  padding: 16px 18px 20px;
}
.native-member-dialog__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 16px;
  color: #303133;
}
.native-member-dialog__x {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 4px;
  background: transparent;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  color: #909399;
}
.native-member-dialog__x:hover {
  background: #f5f7fa;
  color: #303133;
}
.native-member-dialog__toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}
.native-member-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.native-member-table th,
.native-member-table td {
  border: 1px solid #ebeef5;
  padding: 8px 10px;
  text-align: left;
}
.native-member-table th {
  background: #f5f7fa;
  font-weight: 600;
  color: #606266;
}
.link-use {
  color: #67c23a;
  cursor: pointer;
  text-decoration: none;
}
.link-use:hover {
  text-decoration: underline;
}
</style>
