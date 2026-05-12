<script setup>
import { computed, h, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { post } from '@/utils/request'
import { useAuthStore } from '@/stores/auth'
import { useCashierCacheStore } from '@/stores/cashierCache'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const auth = useAuthStore()
const cashierCache = useCashierCacheStore()
const refreshing = ref(false)
const siderCollapsed = ref(false)
const pwdOpen = ref(false)
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
})

const menuOptions = [
  { label: () => h(RouterLink, { to: '/dashboard' }, () => '首页看板'), key: '/dashboard' },
  { label: () => h(RouterLink, { to: '/cashier' }, () => '收银台'), key: '/cashier' },
  { label: () => h(RouterLink, { to: '/goods' }, () => '商品管理'), key: '/goods' },
  { label: () => h(RouterLink, { to: '/categories' }, () => '品类管理'), key: '/categories' },
  { label: () => h(RouterLink, { to: '/members' }, () => '客户管理'), key: '/members' },
  { label: () => h(RouterLink, { to: '/orders' }, () => '订单管理'), key: '/orders' },
  { label: () => h(RouterLink, { to: '/purchases' }, () => '进货管理'), key: '/purchases' },
  { label: () => h(RouterLink, { to: '/suppliers' }, () => '供货商管理'), key: '/suppliers' },
  { label: () => h(RouterLink, { to: '/users' }, () => '用户管理'), key: '/users' },
  { label: () => h(RouterLink, { to: '/regions' }, () => '地址管理'), key: '/regions' },
]

/** 侧栏宽度：容纳「从河心连心农资账务系统」单行展示 */
const SIDER_WIDTH = 276
const SIDER_COLLAPSED_WIDTH = 72

const activeMenu = computed(() => `/${route.path.split('/')[1] || 'dashboard'}`)

const headerUserLabel = computed(() => {
  const u = auth.user
  if (!u) return '未登录'
  if (u.role === 1) return '系统管理员'
  return u.realName || u.username || '用户'
})

async function refreshUser() {
  if (!auth.token) return
  refreshing.value = true
  try {
    const info = await post('/auth/info')
    auth.setAuth({ token: auth.token, userInfo: info })
  } catch {
    /* ignore */
  } finally {
    refreshing.value = false
  }
}

async function submitPwd() {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    message.warning('请填写原密码与新密码')
    return
  }
  if (pwdForm.newPassword.length < 6 || pwdForm.newPassword.length > 20) {
    message.warning('新密码长度为 6～20 位')
    return
  }
  try {
    await post('/auth/updatePwd', {
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    message.success('密码已修改')
    pwdOpen.value = false
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
  } catch (e) {
    message.error(e.message || '修改失败')
  }
}

async function logout() {
  try {
    await post('/auth/logout')
  } catch {
    /* ignore */
  }
  auth.clearAuth()
  cashierCache.clear()
  message.success('已退出登录')
  await router.push('/login')
}

onMounted(() => {
  refreshUser()
})
</script>

<template>
  <div
    class="layout-shell"
    :class="{ 'layout-shell--collapsed': siderCollapsed }"
    :style="{
      '--layout-sider-width': `${SIDER_WIDTH}px`,
      '--layout-sider-collapsed': `${SIDER_COLLAPSED_WIDTH}px`,
    }"
  >
    <n-layout has-sider class="app-root app-root--fixed-sider">
    <n-layout-sider
      v-model:collapsed="siderCollapsed"
      bordered
      collapse-mode="width"
      :collapsed-width="SIDER_COLLAPSED_WIDTH"
      :width="SIDER_WIDTH"
      show-trigger
      class="sider layout-sider-fixed"
      content-style="display: flex; flex-direction: column;"
    >
      <div class="brand">
        <div class="brand-badge" aria-hidden="true">河</div>
        <div class="brand-text">从河心连心农资账务系统</div>
      </div>
      <n-menu class="side-menu" :value="activeMenu" :options="menuOptions" />
    </n-layout-sider>
    <n-layout class="main-wrap main-offset">
      <n-layout-header bordered class="header">
        <div>
          <div class="title">{{ route.meta.title || '从河心连心农资账务系统' }}</div>
          <div class="muted subtitle">从河心农资 · 门店账务 / 进销存 / 统计分析</div>
        </div>
        <n-space align="center" :size="12">
          <span class="header-user-link">{{ headerUserLabel }}</span>
          <n-button quaternary type="primary" :loading="refreshing" @click="refreshUser">刷新信息</n-button>
          <n-button quaternary @click="pwdOpen = true">修改密码</n-button>
          <n-button type="error" ghost @click="logout">退出</n-button>
        </n-space>
      </n-layout-header>
      <n-layout-content content-style="padding: 20px; overflow: visible; min-height: calc(100vh - 128px);">
        <router-view v-slot="{ Component, route: r }">
          <template v-if="r.meta.keepAlive">
            <keep-alive>
              <component :is="Component" :key="r.path" />
            </keep-alive>
          </template>
          <component :is="Component" v-else :key="r.fullPath" />
        </router-view>
      </n-layout-content>
      <n-layout-footer bordered class="footer">从河心连心农资账务系统</n-layout-footer>
    </n-layout>
  </n-layout>
  </div>

  <n-modal
    v-model:show="pwdOpen"
    preset="card"
    title="修改登录密码"
    style="width: 420px"
    :bordered="false"
    :mask-closable="false"
  >
    <template #action>
      <n-space justify="end">
        <n-button @click="pwdOpen = false">取消</n-button>
        <n-button type="primary" @click="submitPwd">保存</n-button>
      </n-space>
    </template>
    <n-form label-placement="top">
      <n-form-item label="原密码">
        <n-input v-model:value="pwdForm.oldPassword" type="password" show-password-on="click" />
      </n-form-item>
      <n-form-item label="新密码（6～20 位）">
        <n-input v-model:value="pwdForm.newPassword" type="password" show-password-on="click" />
      </n-form-item>
    </n-form>
  </n-modal>
</template>

<style scoped>
.layout-shell {
  min-height: 100vh;
}
.app-root {
  min-height: 100vh;
}
/** 侧栏固定在视口，仅右侧主体参与页面滚动（折叠时为 72px） */
.app-root--fixed-sider :deep(.n-layout-scroll-container) {
  align-items: flex-start !important;
}
.app-root--fixed-sider :deep(.n-layout-sider.layout-sider-fixed) {
  position: fixed !important;
  top: 0 !important;
  left: 0 !important;
  height: 100vh !important;
  max-height: 100vh !important;
  z-index: 2000 !important;
  width: var(--layout-sider-width, 276px) !important;
}
.layout-shell--collapsed .app-root--fixed-sider :deep(.n-layout-sider.layout-sider-fixed) {
  width: var(--layout-sider-collapsed, 72px) !important;
}
/** 占位不挤占版面，间距由右侧 margin-left 承担 */
.app-root--fixed-sider :deep(.n-layout-sider-placeholder) {
  width: 0 !important;
  min-width: 0 !important;
  max-width: 0 !important;
  flex: 0 0 0 !important;
}
.app-root--fixed-sider :deep(.n-layout-sider-scroll-container) {
  max-height: 100vh;
  overflow-y: auto;
}
.main-offset {
  margin-left: var(--layout-sider-width, 276px) !important;
  flex: 1 1 auto !important;
  min-width: 0 !important;
}
.layout-shell--collapsed .main-offset {
  margin-left: var(--layout-sider-collapsed, 72px) !important;
}
:deep(.n-layout),
:deep(.n-layout-scroll-container) {
  min-height: 0;
}
.sider {
  background:
    linear-gradient(165deg, rgba(255, 255, 255, 0.72) 0%, transparent 42%),
    linear-gradient(180deg, #e8f5f0 0%, #dff6ec 36%, #d1fae5 100%) !important;
  box-shadow:
    4px 0 24px rgba(15, 81, 50, 0.07),
    1px 0 0 rgba(255, 255, 255, 0.65) inset;
  border-right: 1px solid rgba(16, 185, 129, 0.12) !important;
}
.brand {
  min-height: 56px;
  display: flex;
  align-items: center;
  padding: 10px 14px;
  gap: 12px;
  border-bottom: 1px solid rgba(16, 118, 75, 0.1);
  flex-shrink: 0;
}
.brand-badge {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 800;
  font-size: 16px;
  letter-spacing: 0.02em;
  background: linear-gradient(145deg, #10b981 0%, #059669 45%, #047857 100%);
  box-shadow:
    0 4px 12px rgba(5, 150, 105, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.25);
  flex-shrink: 0;
}
.brand-text {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.35;
  letter-spacing: 0.01em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.side-menu {
  flex: 1;
  background: transparent !important;
}
:deep(.side-menu .n-menu-item-content) {
  border-radius: 10px;
  margin: 3px 10px;
  transition: background 0.2s ease, color 0.2s ease;
}
:deep(.side-menu .n-menu-item-content--selected) {
  position: relative;
  background: var(--ep-sidebar-active-bg, rgba(103, 194, 58, 0.18)) !important;
  color: var(--ep-sidebar-active-text, #529b2e) !important;
}
:deep(.side-menu .n-menu-item-content--selected::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 3px;
  border-radius: 0 2px 2px 0;
  background: #67c23a;
}
:deep(.side-menu .n-menu-item-content-header a) {
  color: inherit;
  text-decoration: none;
}
.main-wrap {
  background: linear-gradient(180deg, #f1f5f9 0%, #f8fafc 28%, #f1f5f9 100%);
}
.header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.85) !important;
  -webkit-backdrop-filter: blur(12px);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(148, 163, 184, 0.2) !important;
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.8) inset;
}
.title {
  font-size: 17px;
  font-weight: 600;
  color: #0f172a;
  letter-spacing: 0.02em;
}
.subtitle {
  margin-top: 3px;
  font-size: 12px;
  letter-spacing: 0.02em;
}
.header-user-link {
  font-size: 14px;
  color: #409eff;
  cursor: default;
}
.footer {
  padding: 14px 24px;
  text-align: center;
  font-size: 12px;
  color: #64748b;
  letter-spacing: 0.04em;
  background: rgba(255, 255, 255, 0.75) !important;
  border-top: 1px solid rgba(148, 163, 184, 0.18) !important;
}
</style>
