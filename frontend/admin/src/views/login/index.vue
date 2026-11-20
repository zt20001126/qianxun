<script setup lang="ts">
/** 管理员登录页面：提供账号密码输入、提交状态和简短错误提示。 */
import { ref } from 'vue'
import AdminNoticeDialog from '../../components/AdminNoticeDialog.vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const username = ref('')
const password = ref('')
const loading = ref(false)
const errorMessage = ref('')
const router = useRouter()
const auth = useAuthStore()

async function submit(): Promise<void> {
  errorMessage.value = ''
  if (!username.value.trim() || !password.value) {
    errorMessage.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  try {
    await auth.login(username.value.trim(), password.value)
    password.value = ''
    await router.replace('/home')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-shell">
    <img class="login-backdrop" src="/assets/nanxun-water-town-login.webp" alt="" aria-hidden="true" />
    <div class="login-backdrop-overlay" aria-hidden="true"></div>
    <section class="login-card" aria-labelledby="login-title">
      <header class="login-brand-header">
        <div class="login-logo-frame">
          <img class="login-logo" src="/assets/qianxun-logo.jpg" alt="芊浔民宿标志" />
        </div>
        <div class="login-brand-name">
          <h1 id="login-title" class="login-title">芊浔民宿</h1>
          <span class="login-location">南浔</span>
        </div>
        <p class="login-subtitle">管理后台</p>
      </header>
      <form class="login-form" @submit.prevent="submit">
        <label class="login-visually-hidden" for="username">管理员账号</label>
        <input
          id="username"
          v-model="username"
          autocomplete="username"
          maxlength="60"
          placeholder="管理员账号"
          :aria-invalid="Boolean(errorMessage)"
        />
        <label class="login-visually-hidden" for="password">登录密码</label>
        <input
          id="password"
          v-model="password"
          type="password"
          autocomplete="current-password"
          maxlength="128"
          placeholder="登录密码"
          :aria-invalid="Boolean(errorMessage)"
        />
        <button class="primary-button" type="submit" :disabled="loading">
          {{ loading ? '正在登录…' : '登录' }}
        </button>
      </form>
    </section>
    <AdminNoticeDialog v-model:message="errorMessage" />
  </main>
</template>
