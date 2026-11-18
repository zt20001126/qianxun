import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

/** 管理后台认证路由；进入成功页前向后端核验管理员会话。 */
export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/home' },
    { path: '/login', component: () => import('../views/login/index.vue'), meta: { title: '管理员登录' } },
    { path: '/home', component: () => import('../views/home/index.vue'), meta: { requiresAuth: true, title: '登录成功' } },
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
