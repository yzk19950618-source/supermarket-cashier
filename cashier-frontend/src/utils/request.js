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
  })
}
