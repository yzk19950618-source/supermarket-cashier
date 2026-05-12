import { createApp } from 'vue'
import { createPinia } from 'pinia'
import naive from 'naive-ui'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'
import './assets/main.css'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(router)
app.use(naive)
app.config.errorHandler = (err, instance, info) => {
  console.error('[Vue Error]', err, info)
}
const auth = useAuthStore()
auth.initFromStorage()
app.mount('#app')
