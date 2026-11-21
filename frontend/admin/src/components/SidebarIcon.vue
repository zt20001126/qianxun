<script setup lang="ts">
type SidebarIconName =
  | 'overview'
  | 'calendar'
  | 'room'
  | 'orders'
  | 'members'
  | 'portraits'
  | 'points'
  | 'payments'
  | 'refunds'
  | 'room-blocks'
  | 'channels'
  | 'creators'
  | 'expenses'
  | 'reports'

defineProps<{ name: SidebarIconName }>()
</script>

<template>
  <svg
    class="sidebar-icon"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.7"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    focusable="false"
  >
    <template v-if="name === 'overview'">
      <rect x="3.5" y="3.5" width="7" height="7" rx="1" />
      <rect x="13.5" y="3.5" width="7" height="7" rx="1" />
      <rect x="3.5" y="13.5" width="7" height="7" rx="1" />
      <rect x="13.5" y="13.5" width="7" height="7" rx="1" />
    </template>
    <template v-else-if="name === 'calendar'">
      <rect x="3.5" y="5" width="17" height="16" rx="2" />
      <path d="M7.5 3v4M16.5 3v4M3.5 10h17M8 14h.01M12 14h.01M16 14h.01M8 17.5h.01M12 17.5h.01" />
    </template>
    <template v-else-if="name === 'room'">
      <path d="M3.5 19.5v-8a2 2 0 0 1 2-2h13a2 2 0 0 1 2 2v8" />
      <path d="M3.5 15h17M7 9.5V7a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v2.5M3.5 19.5v1M20.5 19.5v1" />
    </template>
    <template v-else-if="name === 'orders'">
      <path d="M8 4H6.5a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2h11a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2H16" />
      <rect x="8" y="2.5" width="8" height="4" rx="1.5" />
      <path d="M8 11h8M8 15h8M8 18h5" />
    </template>
    <template v-else-if="name === 'members'">
      <circle cx="9" cy="8" r="3.2" />
      <path d="M3.5 20v-1a5.5 5.5 0 0 1 11 0v1M16 5.5a3 3 0 0 1 0 5.8M17 14a4.5 4.5 0 0 1 3.5 4.4V20" />
    </template>
    <template v-else-if="name === 'portraits'">
      <circle cx="9" cy="8" r="3" />
      <path d="M3.5 20v-1a5.5 5.5 0 0 1 11 0v1M17 5h4M17 9h4M17 13h4M17 17h4" />
    </template>
    <template v-else-if="name === 'points'">
      <path d="m12 3 2.7 5.3 5.8.8-4.2 4.1 1 5.8-5.3-2.8-5.3 2.8 1-5.8-4.2-4.1 5.8-.8z" />
      <path d="m8 17.8-1 3.7 5-2.3 5 2.3-1-3.7" />
    </template>
    <template v-else-if="name === 'payments'">
      <rect x="3" y="5" width="18" height="14" rx="2" />
      <path d="M3 9.5h18M7 14h4M7 16.5h2" />
    </template>
    <template v-else-if="name === 'refunds'">
      <path d="M4 9a8 8 0 0 1 13.7-4.7L20 6.5M20 3v3.5h-3.5M20 15a8 8 0 0 1-13.7 4.7L4 17.5M4 21v-3.5h3.5" />
    </template>
    <template v-else-if="name === 'room-blocks'">
      <rect x="4" y="10" width="16" height="11" rx="2" />
      <path d="M8 10V7a4 4 0 0 1 8 0v3M12 14v3" />
    </template>
    <template v-else-if="name === 'channels'">
      <circle cx="6" cy="12" r="2.5" />
      <circle cx="18" cy="5" r="2.5" />
      <circle cx="18" cy="19" r="2.5" />
      <path d="m8.2 10.8 7.5-4.5M8.2 13.2l7.5 4.5" />
    </template>
    <template v-else-if="name === 'creators'">
      <circle cx="9" cy="8" r="3.2" />
      <path d="M3.5 20v-1a5.5 5.5 0 0 1 11 0v1M18 3.5l.9 2.1 2.1.9-2.1.9-.9 2.1-.9-2.1-2.1-.9 2.1-.9z" />
    </template>
    <template v-else-if="name === 'expenses'">
      <path d="M6 3.5h12v17l-2-1.3-2 1.3-2-1.3-2 1.3-2-1.3-2 1.3z" />
      <path d="M14.5 8.5a2.5 2.5 0 1 0-2.5 2.5 2.5 2.5 0 1 1-2.5 2.5M12 7v8" />
    </template>
    <template v-else-if="name === 'reports'">
      <path d="M4 20V4M4 20h17" />
      <path d="M8 16v-4M12 16V8M16 16v-7M20 16v-11" />
    </template>
  </svg>
</template>
