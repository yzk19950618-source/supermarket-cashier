/**
 * 展示为 yyyy-MM-dd HH:mm:ss（无时区换算，按字符串规范化）
 * @param {unknown} v LocalDateTime JSON、Date、或 yyyy-MM-dd
 */
export function formatDateTime(v) {
  if (v == null || v === '') return '-'
  const s = String(v).trim()
  if (!s) return '-'
  // 纯日期 yyyy-MM-dd
  if (/^\d{4}-\d{2}-\d{2}$/.test(s)) return `${s} 00:00:00`
  // ISO 或 "yyyy-MM-dd HH:mm:ss" / "yyyy-MM-ddTHH:mm:ss"
  let t = s.replace('T', ' ')
  const m = t.match(/^(\d{4}-\d{2}-\d{2})\s+(\d{1,2}:\d{1,2}(?::\d{1,2})?(?:\.\d+)?)/)
  if (m) {
    const date = m[1]
    let time = m[2].split('.')[0]
    const parts = time.split(':')
    const hh = parts[0].padStart(2, '0')
    const mm = (parts[1] || '00').padStart(2, '0')
    const ss = (parts[2] || '00').padStart(2, '0')
    return `${date} ${hh}:${mm}:${ss}`
  }
  if (/^\d{4}-\d{2}-\d{2}/.test(t)) return t.slice(0, 19).replace('T', ' ')
  return t
}
