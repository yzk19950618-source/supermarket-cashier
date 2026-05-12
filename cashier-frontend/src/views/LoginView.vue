<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { post } from '@/utils/request'
import { useAuthStore } from '@/stores/auth'
import { useCashierCacheStore } from '@/stores/cashierCache'

const router = useRouter()
const message = useMessage()
const auth = useAuthStore()

const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: '123456',
})

async function onSubmit() {
  loading.value = true
  try {
    const data = await post('/auth/login', { ...form })
    auth.setAuth({ token: data.token, userInfo: data.userInfo })
    await useCashierCacheStore().bootstrap()
    message.success('登录成功')
    await router.push('/dashboard')
  } catch (e) {
    message.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-title">从河心连心农资账务系统</div>
      <div class="login-subtitle">支持收银、商品、客户、进货、订单、统计等业务流程</div>
      <n-card class="page-card" :bordered="false">
        <n-form :model="form" label-placement="top" @keyup.enter="onSubmit">
          <n-form-item label="用户名">
            <n-input v-model:value="form.username" placeholder="用户名" />
          </n-form-item>
          <n-form-item label="密码">
            <n-input v-model:value="form.password" type="password" placeholder="密码" />
          </n-form-item>
          <n-button type="primary" block :loading="loading" @click="onSubmit">登录</n-button>
        </n-form>
      </n-card>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background:
    radial-gradient(ellipse 120% 80% at 50% -20%, rgba(16, 185, 129, 0.18), transparent 55%),
    linear-gradient(155deg, #ecfdf5 0%, #e0f2fe 42%, #f8fafc 100%);
}
.login-card {
  width: 100%;
  max-width: 460px;
}
.login-title {
  font-size: 21px;
  font-weight: 800;
  text-align: center;
  margin-bottom: 10px;
  color: #0f172a;
  letter-spacing: 0.03em;
  line-height: 1.35;
  white-space: nowrap;
}
@media (max-width: 520px) {
  .login-title {
    white-space: normal;
  }
}
.login-subtitle {
  text-align: center;
  color: #64748b;
  font-size: 13px;
  margin-bottom: 22px;
  line-height: 1.5;
}
.login-card :deep(.page-card) {
  border-radius: 14px !important;
  box-shadow:
    0 24px 48px rgba(15, 81, 50, 0.08),
    0 4px 16px rgba(15, 23, 42, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.9) !important;
  border: 1px solid rgba(148, 163, 184, 0.22) !important;
}
</style>
