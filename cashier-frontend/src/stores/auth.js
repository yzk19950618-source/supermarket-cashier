import { defineStore } from 'pinia'

const TOKEN_KEY = 'cashier_token'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: '',
    user: null,
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
  },
  actions: {
    setAuth(payload) {
      const raw = payload?.token ?? ''
      const token = typeof raw === 'string' ? raw.replace(/^Bearer\s+/i, '') : ''
      this.token = token
      this.user = payload?.userInfo ?? payload?.user ?? null
      if (token) localStorage.setItem(TOKEN_KEY, token)
      else localStorage.removeItem(TOKEN_KEY)
    },
    clearAuth() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
    },
    initFromStorage() {
      const t = localStorage.getItem(TOKEN_KEY)
      if (t) this.token = t.replace(/^Bearer\s+/i, '')
    },
  },
})
