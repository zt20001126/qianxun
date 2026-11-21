import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

/** 管理后台认证路由；进入成功页前向后端核验管理员会话。 */
export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/home' },
    { path: '/login', component: () => import('../views/login/index.vue'), meta: { title: '管理员登录' } },
    {
      path: '/',
      component: () => import('../layouts/AdminLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: 'home', component: () => import('../views/home/index.vue'), meta: { title: '经营总览' } },
        { path: 'room-calendar', component: () => import('../views/calendar/index.vue'), meta: { title: '房态日历' } },
        { path: 'rooms', component: () => import('../views/rooms/index.vue'), meta: { title: '客房管理' } },
        { path: 'orders', component: () => import('../views/orders/index.vue'), meta: { title: '订单管理' } },
        { path: 'members', component: () => import('../views/members/index.vue'), meta: { title: '会员管理' } },
        { path: 'portraits', component: () => import('../views/portraits/index.vue'), meta: { title: '顾客画像' } },
        { path: 'points', component: () => import('../views/points/index.vue'), meta: { title: '积分流水' } },
        { path: 'payments', component: () => import('../views/transactions/payments.vue'), meta: { title: '支付流水' } },
        { path: 'refunds', component: () => import('../views/transactions/refunds.vue'), meta: { title: '退款流水' } },
        { path: 'room-blocks', component: () => import('../views/room-blocks/index.vue'), meta: { title: '封房管理' } },
        { path: 'channels', component: () => import('../views/channels/index.vue'), meta: { title: '渠道管理' } },
        { path: 'creators', component: () => import('../views/creators/index.vue'), meta: { title: '达人管理' } },
        { path: 'expenses', component: () => import('../views/expenses/index.vue'), meta: { title: '经营支出' } },
        { path: 'reports', component: () => import('../views/reports/index.vue'), meta: { title: '经营报表' } },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/login' },
  ],
})

router.beforeEach(async (to) => {
  document.title = `${String(to.meta.title || '管理后台')} · 芊浔民宿`
  const auth = useAuthStore()
  if (to.meta.requiresAuth) {
    if (!auth.token) return '/login'
    if (!auth.admin) {
      try {
        await auth.fetchMe()
      } catch {
        auth.clear()
        return '/login'
      }
    }
  }
  if (to.path === '/login' && auth.token) {
    try {
      if (!auth.admin) await auth.fetchMe()
      return '/home'
    } catch {
      auth.clear()
    }
  }
  return true
})
