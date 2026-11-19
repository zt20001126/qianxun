<script setup lang="ts">
/** 后台壳层，集中提供模块导航、管理员身份和退出入口。 */
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

function logout(): void {
  auth.clear()
  void router.replace('/login')
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
        <RouterLink to="/home"><span>总</span>经营总览</RouterLink>
        <RouterLink to="/room-calendar"><span>历</span>房态日历</RouterLink>
        <RouterLink to="/rooms"><span>客</span>客房管理</RouterLink>
        <RouterLink to="/orders"><span>单</span>订单管理</RouterLink>
        <RouterLink to="/members"><span>会</span>会员管理</RouterLink>
        <RouterLink to="/points"><span>积</span>积分流水</RouterLink>
        <RouterLink to="/payments"><span>付</span>支付流水</RouterLink>
        <RouterLink to="/refunds"><span>退</span>退款流水</RouterLink>
        <RouterLink to="/room-blocks"><span>封</span>封房管理</RouterLink>
        <RouterLink to="/channels"><span>渠</span>渠道管理</RouterLink>
        <RouterLink to="/creators"><span>达</span>达人管理</RouterLink>
        <RouterLink to="/expenses"><span>支</span>经营支出</RouterLink>
        <RouterLink to="/reports"><span>报</span>经营报表</RouterLink>
      </nav>
      <div class="admin-account sidebar-account">
        <img class="account-avatar" src="/assets/qianxun-logo.jpg" alt="芊浔民宿标志" />
        <span class="account-name">{{ auth.admin?.username || 'admin' }}</span>
        <button class="text-button" type="button" @click="logout">退出登录</button>
      </div>
    </aside>

    <div class="admin-main">
      <header class="admin-topbar">
        <div class="topbar-heading">
          <p>芊浔民宿 · 南浔</p>
          <strong>{{ $route.meta.title || '经营管理' }}</strong>
        </div>
      </header>
      <main class="admin-content"><RouterView /></main>
    </div>
  </div>
</template>
