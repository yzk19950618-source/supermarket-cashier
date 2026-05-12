import axios from 'axios'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'

const TOKEN_KEY = 'cashier_token'

const instance = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

instance.interceptors.request.use((config) => {
  const auth = useAuthStore()
  const raw = auth.token || localStorage.getItem(TOKEN_KEY) || ''
  const token = raw.replace(/^Bearer\s+/i, '')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body?.code === 200) return body.data
    if (body?.code === 401 || body?.code === 403) {
      const auth = useAuthStore()
      auth.clearAuth()
      router.push('/login')
    }
    return Promise.reject(new Error(body?.message || '请求失败'))
  },
  (err) => Promise.reject(err),
)

export function post(url, data = {}) {
  return instance.post(url, data)
}

export function get(url, params = {}) {
  return instance.get(url, { params })
}

export const http = instance

/** multipart，字段名 file；返回体已由拦截器解包为 data */
export function uploadFile(url, file) {
  const fd = new FormData()
  fd.append('file', file)
  return instance.post(url, fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000,
  })
}

function authJsonHeaders() {
  const auth = useAuthStore()
  const raw = auth.token || localStorage.getItem(TOKEN_KEY) || ''
  const token = raw.replace(/^Bearer\s+/i, '')
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  return headers
}

/**
 * POST JSON，返回二进制（Excel 等）。非 2xx 或 JSON 错误体时 reject。
 */
export async function postBlob(url, data = {}) {
  const path = url.startsWith('/') ? url : `/${url}`
  const res = await axios.post(`${instance.defaults.baseURL}${path}`, data, {
    responseType: 'blob',
    headers: authJsonHeaders(),
    timeout: 120000,
  })
  if (res.status >= 400) {
    return Promise.reject(new Error(`HTTP ${res.status}`))
  }
  const ct = (res.headers['content-type'] || '').toLowerCase()
  if (ct.includes('application/json')) {
    const text = await res.data.text()
    let body
    try {
      body = JSON.parse(text)
    } catch {
      return Promise.reject(new Error(text || '请求失败'))
    }
    if (body?.code === 401 || body?.code === 403) {
      const auth = useAuthStore()
      auth.clearAuth()
      router.push('/login')
    }
    return Promise.reject(new Error(body?.message || '请求失败'))
  }
  return { blob: res.data, contentDisposition: res.headers['content-disposition'] }
}

export function filenameFromContentDisposition(cd) {
  if (!cd || typeof cd !== 'string') return ''
  const star = cd.match(/filename\*=UTF-8''([^;\s]+)/i)
  if (star) {
    try {
      return decodeURIComponent(star[1].trim())
    } catch {
      return star[1]
    }
  }
  const q = cd.match(/filename="([^"]+)"/i)
  if (q) return q[1]
  const u = cd.match(/filename=([^;\s]+)/i)
  return u ? u[1].trim() : ''
}

export function triggerDownloadBlob(blob, filename) {
  const u = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = u
  a.download = filename || 'download.bin'
  a.click()
  URL.revokeObjectURL(u)
}
