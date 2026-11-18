<script setup lang="ts">
/** 管理员登录成功占位页；本阶段不放入客房、订单等后台业务模块。 */
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const router = useRouter()

function logout(): void {
  auth.clear()
  void router.replace('/login')
}
</script>

<template>
  <main class="home-shell">
    <header class="home-header">
      <div class="home-brand"><span class="small-mark">浔</span><span>芊浔民宿</span></div>
      <button class="text-button" type="button" @click="logout">退出登录</button>
    </header>
    <section class="welcome-card">
      <p class="eyebrow">ADMIN SESSION</p>
      <h1>登录成功</h1>
      <p class="welcome-copy">欢迎，{{ auth.admin?.username }}。管理后台认证已完成。</p>
      <div class="status-line"><span class="status-dot"></span>管理员身份已验证</div>
      <p class="phase-note">第一阶段仅完成登录认证，业务管理页面将在后续阶段接入。</p>
    </section>
  </main>
</template>
