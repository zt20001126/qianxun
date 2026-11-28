<script setup lang="ts">
/** 按月维护全店共用的节假日计价日期。 */
import { computed, onMounted, ref } from 'vue'
import { roomApi } from '../../api/rooms'
import { monthDateList, shanghaiDate, shiftMonthValue } from '../../utils/operations'

const weekDays = ['一', '二', '三', '四', '五', '六', '日']
const month = ref(shanghaiDate().slice(0, 7))
const saved = ref<string[]>([])
const selected = ref<string[]>([])
const loading = ref(false)
const loaded = ref(false)
const saving = ref(false)
const error = ref('')
const message = ref('')

const dates = computed(() => monthDateList(month.value))
const leadingDays = computed(() => {
  const firstWeekday = new Date(month.value + '-01T00:00:00Z').getUTCDay()
  return (firstWeekday + weekDays.length - 1) % weekDays.length
})
const addedDates = computed(() => selected.value.filter((date) => !saved.value.includes(date)))
const removedDates = computed(() => saved.value.filter((date) => !selected.value.includes(date)))
const dirty = computed(() => addedDates.value.length > 0 || removedDates.value.length > 0)

onMounted(() => { void loadMonth() })

async function loadMonth(): Promise<void> {
  loading.value = true
  loaded.value = false
  error.value = ''
  message.value = ''
  try {
    acceptDates(await roomApi.listHolidayPriceDates(month.value))
    loaded.value = true
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '节假日日期读取失败'
  } finally {
    loading.value = false
  }
}

function acceptDates(value: string[]): void {
  saved.value = value
  selected.value = [...value]
}

function switchMonth(offset: number): void {
  month.value = shiftMonthValue(month.value, offset)
  void loadMonth()
}

function toggleDate(date: string): void {
  message.value = ''
  selected.value = selected.value.includes(date)
    ? selected.value.filter((item) => item !== date)
    : [...selected.value, date]
}

function discard(): void {
  selected.value = [...saved.value]
  error.value = ''
}

async function save(): Promise<void> {
  if (!dirty.value || saving.value) return

  saving.value = true
  error.value = ''
  try {
    // 只提交本次变更，保留其他管理员对同月其他日期的设置。
    const dates = await roomApi.updateHolidayPriceDates({
      month: month.value,
      add_dates: addedDates.value,
      remove_dates: removedDates.value,
    })
    acceptDates(dates)
    message.value = '节假日日期已保存'
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '节假日日期保存失败'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section id="holiday-price-calendar" class="pricing-calendar table-card" aria-labelledby="holiday-price-title">
    <div>
      <h2 id="holiday-price-title">节假日价格日历</h2>
      <p class="page-description">勾选后，全店各房间在这些住宿日期使用各自的节假日价。</p>
    </div>
    <div class="month-navigation">
      <button type="button" class="button button-outline" :disabled="loading || saving || dirty || month <= '1000-01'" @click="switchMonth(-1)">上个月</button>
      <strong aria-live="polite">{{ month.slice(0, 4) }} 年 {{ Number(month.slice(5)) }} 月</strong>
      <button type="button" class="button button-outline" :disabled="loading || saving || dirty || month >= '9999-12'" @click="switchMonth(1)">下个月</button>
    </div>
    <p v-if="error" class="calendar-error" role="alert">{{ error }}</p>
    <p v-if="loading" role="status">正在读取节假日日期…</p>
    <button v-else-if="!loaded" type="button" class="button button-outline" @click="loadMonth">重试</button>
    <template v-else>
      <div class="date-grid">
        <span v-for="day in weekDays" :key="day" class="weekday">周{{ day }}</span>
        <span v-for="index in leadingDays" :key="'blank-' + index" aria-hidden="true"></span>
        <button v-for="date in dates" :key="date" type="button" class="date-button"
          :aria-label="date" :aria-pressed="selected.includes(date)" :disabled="saving" @click="toggleDate(date)">
          {{ Number(date.slice(-2)) }}<span v-if="selected.includes(date)" aria-hidden="true"> ✓</span>
        </button>
      </div>
      <div class="calendar-actions">
        <span role="status">{{ message || (dirty ? '有未保存的日期变更' : '已选 ' + selected.length + ' 天') }}</span>
        <button v-if="dirty" type="button" class="button button-outline" :disabled="saving" @click="discard">放弃更改</button>
        <button type="button" class="button button-primary" :disabled="!dirty || saving" @click="save">
          {{ saving ? '保存中…' : '保存日期' }}
        </button>
      </div>
    </template>
  </section>
</template>

<style scoped>
.pricing-calendar { display: grid; gap: 14px; padding: 18px; }
.pricing-calendar h2 { margin: 0; color: var(--qx-primary-dark); font-size: 18px; }
.pricing-calendar p { margin: 4px 0 0; }
.month-navigation, .calendar-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; }
.date-grid { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); gap: 6px; max-width: 490px; }
.weekday { text-align: center; color: var(--qx-muted); font-size: 12px; }
.date-button { min-height: 36px; border: 1px solid var(--qx-border); border-radius: 6px; color: var(--qx-text); background: var(--qx-card-bg); cursor: pointer; }
.date-button[aria-pressed='true'] { border-color: var(--qx-primary); color: #fff; background: var(--qx-primary); }
.date-button:disabled { cursor: wait; opacity: .7; }
.calendar-error { color: var(--qx-danger); }
</style>
