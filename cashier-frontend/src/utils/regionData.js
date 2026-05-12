/**
 * 与离线包 `CashierView` 一致：将后端 `/region/all` 返回的树扁平化，
 * 再按省→市→区（名称/编码）供原生 <select> 与地址维护表使用。
 * 后端节点字段见 RegionTree：value/code、label/name、children。
 */

function nodeDisplayName(n) {
  return String(n.label ?? n.name ?? n.regionName ?? n.text ?? '').trim()
}

export function flattenRegionTree(nodes, parentCode = '', level = 1, out = []) {
  for (const n of nodes || []) {
    const code = String(n.value ?? n.code ?? n.id ?? n.regionCode ?? '').trim()
    const name = nodeDisplayName(n)
    if (!code) continue
    out.push({
      code,
      name,
      parentCode: parentCode || '',
      level,
      sort: out.length + 1,
    })
    const ch = n.children
    if (Array.isArray(ch) && ch.length) flattenRegionTree(ch, code, level + 1, out)
  }
  return out
}

/**
 * 后端若直接返回 MySQL region 扁平表（无 children 嵌套，含 level / parent_code），
 * 不能再走 flattenRegionTree，否则每一行都会被当成省级。
 */
export function isFlatRegionRecordList(arr) {
  if (!Array.isArray(arr) || arr.length === 0) return false
  if (arr.some((x) => Array.isArray(x?.children) && x.children.length > 0)) return false
  let withLevel = 0
  for (const x of arr) {
    if (x != null && x.level != null && String(x.level) !== '') withLevel++
    if (withLevel >= 5) break
  }
  return withLevel >= Math.min(arr.length, 5)
}

/** 将扁平行政区划记录转为与 flattenRegionTree 一致的结构 */
export function flatRowsFromRegionRecords(rows) {
  const out = []
  let i = 0
  for (const r of rows || []) {
    const code = String(r.code ?? r.value ?? r.id ?? r.regionCode ?? '').trim()
    const name = String(r.name ?? r.label ?? r.regionName ?? '').trim()
    if (!code || !name) continue
    const lvl = Number(r.level)
    const level = Number.isFinite(lvl) && lvl >= 1 ? lvl : 1
    const pc = r.parent_code ?? r.parentCode ?? r.parentId
    const parentCode = pc == null || pc === '' ? '' : String(pc).trim()
    out.push({ code, name, parentCode, level, sort: ++i })
  }
  return out
}

/** 统一入口：接口返回树或扁平表均可 */
export function normalizeRegionFlatRows(raw) {
  const arr = Array.isArray(raw) ? raw : []
  if (arr.length === 0) return []
  if (isFlatRegionRecordList(arr)) return flatRowsFromRegionRecords(arr)
  return flattenRegionTree(arr)
}

/** 离线包：按名称嵌套 y[省][市] = [{ name, code }, ...] 区列表 */
export function buildNameKeyedRegionTree(flatRows) {
  const provinces = flatRows.filter((r) => r.level === 1)
  const cities = flatRows.filter((r) => r.level === 2)
  const districts = flatRows.filter((r) => r.level === 3)
  const y = {}
  for (const p of provinces) {
    if (!y[p.name]) y[p.name] = {}
  }
  for (const c of cities) {
    const p = provinces.find((pr) => String(pr.code) === String(c.parentCode))
    if (!p) continue
    if (!y[p.name]) y[p.name] = {}
    if (!y[p.name][c.name]) y[p.name][c.name] = []
  }
  for (const d of districts) {
    const c = cities.find((ct) => String(ct.code) === String(d.parentCode))
    if (!c) continue
    const p = provinces.find((pr) => String(pr.code) === String(c.parentCode))
    if (!p) continue
    if (!y[p.name]) y[p.name] = {}
    if (!y[p.name][c.name]) y[p.name][c.name] = []
    y[p.name][c.name].push({ name: d.name, code: d.code })
  }
  return { y, provinces, cities, districts }
}

export function receiverCodesFromNames(flatRows, provinceName, cityName, districtName) {
  const { provinces, cities, districts } = buildNameKeyedRegionTree(flatRows)
  const p = provinces.find((r) => r.name === provinceName)
  if (!p) return ''
  const c = cities.find(
    (r) => r.name === cityName && String(r.parentCode) === String(p.code),
  )
  if (!c) return p.code
  const d = districts.find(
    (r) => r.name === districtName && String(r.parentCode) === String(c.code),
  )
  if (!d) return [p.code, c.code].join(',')
  return [p.code, c.code, d.code].join(',')
}

/** 根据省市区名称在扁平表中查找区县编码（用于 /region/children 拉取镇街） */
export function findDistrictCode(flatRows, provinceName, cityName, districtName) {
  const p = flatRows.find((r) => r.level === 1 && r.name === provinceName)
  if (!p) return ''
  const c = flatRows.find(
    (r) =>
      r.level === 2 &&
      r.name === cityName &&
      String(r.parentCode) === String(p.code),
  )
  if (!c) return ''
  const d = flatRows.find(
    (r) =>
      r.level === 3 &&
      r.name === districtName &&
      String(r.parentCode) === String(c.code),
  )
  return d?.code || ''
}

export function namesFromRegionCodes(flatRows, codesStr) {
  const codes = String(codesStr || '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  if (!codes.length) return { provinceName: '', cityName: '', districtName: '' }
  const byCode = new Map(flatRows.map((r) => [r.code, r]))
  const p = byCode.get(codes[0])
  const c = codes[1] ? byCode.get(codes[1]) : null
  const d = codes[2] ? byCode.get(codes[2]) : null
  return {
    provinceName: p?.name || '',
    cityName: c?.name || '',
    districtName: d?.name || '',
  }
}

/**
 * 从「收货地址全文」开头匹配省→市→区名称（名称须与扁平区划表一致；长名称优先避免误匹配）。
 * @returns {{ provinceName: string, cityName: string, districtName: string, remainder: string } | null}
 */
export function matchPcdPrefixFromAddress(flatRows, fullAddress) {
  const addr = String(fullAddress ?? '').trim()
  if (!addr || !Array.isArray(flatRows) || flatRows.length === 0) return null
  const provinces = flatRows.filter((r) => r.level === 1)
  const sortedP = [...provinces].sort((a, b) => b.name.length - a.name.length)
  for (const p of sortedP) {
    if (!addr.startsWith(p.name)) continue
    let rest = addr.slice(p.name.length)
    const cities = flatRows.filter((r) => r.level === 2 && String(r.parentCode) === String(p.code))
    const sortedC = [...cities].sort((a, b) => b.name.length - a.name.length)
    for (const c of sortedC) {
      if (!rest.startsWith(c.name)) continue
      let rest2 = rest.slice(c.name.length)
      const districts = flatRows.filter((r) => r.level === 3 && String(r.parentCode) === String(c.code))
      const sortedD = [...districts].sort((a, b) => b.name.length - a.name.length)
      for (const d of sortedD) {
        if (!rest2.startsWith(d.name)) continue
        const remainder = rest2.slice(d.name.length).trim()
        return {
          provinceName: p.name,
          cityName: c.name,
          districtName: d.name,
          remainder,
        }
      }
    }
  }
  return null
}
