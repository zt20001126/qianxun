<script setup lang="ts">
/** 后台壳层，集中提供模块导航、管理员身份和退出入口。 */
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SidebarIcon from '../components/SidebarIcon.vue'
import { useAuthStore } from '../stores/auth'
import { formatChineseDate, shanghaiDate } from '../utils/operations'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const mallExpanded = ref(route.path.startsWith('/mall-'))
const isPrototypeShell = computed(() =>
  route.path === '/home' || route.path === '/room-calendar' || route.path === '/orders',
)
const beijingDateLabel = formatChineseDate(shanghaiDate(new Date()))

function logout(): void {
  auth.clear()
  void router.replace('/login')
}

function openExternalOrder(): void {
  void router.push({ path: '/orders', query: { calendar_action: 'create' } })
}
</script>

<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="sidebar-brand">
        <img class="sidebar-mark" src="/assets/qianxun-logo.jpg" alt="芊浔民宿标志" />
        <div><strong>芊浔民宿</strong><small>经营管理后台</small></div>
      </div>
      <nav class="sidebar-nav" aria-label="主导航">
        <RouterLink to="/home" class="sidebar-nav-primary"><SidebarIcon name="overview" />经营总览</RouterLink>
        <RouterLink to="/assistant" class="sidebar-nav-assistant"><SidebarIcon name="assistant" />智能助手</RouterLink>
        <RouterLink to="/room-calendar" class="sidebar-nav-primary"><SidebarIcon name="calendar" />房态日历</RouterLink>
        <RouterLink to="/rooms"><SidebarIcon name="room" />客房管理</RouterLink>
        <div class="mall-navigation">
          <button class="mall-toggle" type="button" :aria-expanded="mallExpanded" @click="mallExpanded = !mallExpanded">
            <SidebarIcon name="orders" />商城管理 <span>{{ mallExpanded ? '▾' : '▸' }}</span>
          </button>
          <div v-if="mallExpanded" class="mall-children">
            <RouterLink to="/mall-products">商品管理</RouterLink>
            <RouterLink to="/mall-orders">商品订单</RouterLink>
          </div>
        </div>
        <RouterLink to="/orders" class="sidebar-nav-primary"><SidebarIcon name="orders" />订单管理</RouterLink>
        <RouterLink to="/invitations" class="sidebar-nav-primary"><SidebarIcon name="members" />邀请记录</RouterLink>
        <RouterLink to="/members" class="sidebar-nav-primary"><SidebarIcon name="members" />会员管理</RouterLink>
        <RouterLink to="/portraits" class="sidebar-nav-primary"><SidebarIcon name="portraits" />顾客画像</RouterLink>
        <RouterLink to="/points"><SidebarIcon name="points" />积分流水</RouterLink>
        <RouterLink to="/payments"><SidebarIcon name="payments" />支付流水</RouterLink>
        <RouterLink to="/refunds"><SidebarIcon name="refunds" />退款流水</RouterLink>
        <RouterLink to="/room-blocks"><SidebarIcon name="room-blocks" />封房管理</RouterLink>
        <RouterLink to="/channels"><SidebarIcon name="channels" />渠道管理</RouterLink>
        <RouterLink to="/creators"><SidebarIcon name="creators" />达人管理</RouterLink>
        <RouterLink to="/expenses"><SidebarIcon name="expenses" />经营支出</RouterLink>
        <RouterLink to="/reports" class="sidebar-nav-primary"><SidebarIcon name="reports" />经营报表</RouterLink>
      </nav>
      <div class="admin-account sidebar-account">
        <img class="account-avatar" src="/assets/qianxun-logo.jpg" alt="芊浔民宿标志" />
        <span class="account-name">{{ auth.admin?.username || 'admin' }}</span>
        <button class="text-button" type="button" @click="logout">退出登录</button>
      </div>
    </aside>

    <div class="admin-main">
      <header class="admin-topbar" :class="{ 'admin-topbar-dashboard': isPrototypeShell }">
        <div class="topbar-heading">
          <div class="topbar-title-line">
            <strong>{{ $route.meta.title || '经营管理' }}</strong>
            <span v-if="isPrototypeShell" class="beijing-date">
              <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>
              北京时间截至：{{ beijingDateLabel }}
            </span>
          </div>
        </div>
        <div v-if="isPrototypeShell" class="topbar-actions">
          <button class="button button-primary" type="button" @click="openExternalOrder">＋ 录入第三方订单</button>
        </div>
      </header>
      <main class="admin-content" :class="{ 'admin-content-dashboard': isPrototypeShell }"><RouterView /></main>
    </div>
  </div>
</template>

<style scoped>
.mall-toggle { display: flex; align-items: center; gap: 12px; width: 100%; min-height: 45px; padding: 0 12px; border: 0; border-radius: 11px; background: transparent; color: var(--qx-primary-dark); font-size: 14px; font-weight: 600; cursor: pointer; text-align: left; }
.mall-toggle span { margin-left: auto; }
.mall-toggle:hover { background: #f3f7f7; }
.mall-children { padding-left: 24px; }
</style>
