<script setup lang="ts">
/** 管理员登录页面：提供账号密码输入、提交状态和简短错误提示。 */
import { ref } from 'vue'
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
    <section class="login-card">
      <div class="brand-mark" aria-hidden="true">浔</div>
      <p class="eyebrow">QIANXUN HOMESTAY</p>
      <h1>芊浔民宿</h1>
      <p class="subtitle">管理后台</p>
      <form class="login-form" @submit.prevent="submit">
        <label for="username">用户名</label>
        <input id="username" v-model="username" autocomplete="username" maxlength="60" placeholder="请输入管理员用户名" />
        <label for="password">密码</label>
        <input id="password" v-model="password" type="password" autocomplete="current-password" maxlength="128" placeholder="请输入密码" />
        <p v-if="errorMessage" class="error-message" role="alert">{{ errorMessage }}</p>
        <button class="primary-button" type="submit" :disabled="loading">
          {{ loading ? '正在登录…' : '登录管理后台' }}
        </button>
      </form>
      <p class="footer-note">仅限授权管理员使用</p>
    </section>
  </main>
</template>
