const ORIGIN = typeof window !== 'undefined' ? window.location.origin : ''

/**
 * 将后端返回的相对路径转为可访问 URL（与静态资源、上传目录同源时使用）。
 */
export function resolveMediaUrl(path) {
  if (path == null || path === '') return ''
  const s = String(path)
  if (/^https?:\/\//i.test(s)) return s
  if (s.startsWith('/')) return `${ORIGIN}${s}`
  return `${ORIGIN}/${s}`
}
