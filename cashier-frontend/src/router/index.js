import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    component: () => import('@/layout/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '首页看板' },
      },
      {
        path: 'cashier',
        name: 'cashier',
        component: () => import('@/views/CashierView.vue'),
        meta: { title: '收银台', keepAlive: true },
      },
      {
        path: 'goods',
        name: 'goods',
        component: () => import('@/views/ManagementView.vue'),
        meta: { title: '商品管理', moduleKey: 'goods' },
      },
      {
        path: 'categories',
        name: 'categories',
        component: () => import('@/views/ManagementView.vue'),
        meta: { title: '品类管理', moduleKey: 'categories' },
      },
      {
        path: 'members',
        name: 'members',
        component: () => import('@/views/ManagementView.vue'),
        meta: { title: '客户管理', moduleKey: 'members' },
      },
      {
        path: 'orders',
        name: 'orders',
        component: () => import('@/views/ManagementView.vue'),
        meta: { title: '订单管理', moduleKey: 'orders' },
      },
      {
        path: 'purchases',
        name: 'purchases',
        component: () => import('@/views/ManagementView.vue'),
        meta: { title: '进货管理', moduleKey: 'purchases' },
      },
      {
        path: 'suppliers',
        name: 'suppliers',
        component: () => import('@/views/ManagementView.vue'),
        meta: { title: '供货商管理', moduleKey: 'suppliers' },
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('@/views/ManagementView.vue'),
        meta: { title: '用户管理', moduleKey: 'users' },
      },
      {
        path: 'regions',
        name: 'regions',
        component: () => import('@/views/AddressManagementView.vue'),
        meta: { title: '地址管理' },
      },
    ],
  },
]

const router = createRouter({
  /** 与 `vite.config.js` 的 `base` 一致，避免部署在子路径或非根静态资源时路由与 chunk 加载失败导致白屏 */
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isLoggedIn) return '/login'
  if (to.path === '/login' && auth.isLoggedIn) return '/dashboard'
  return true
})

export default router
