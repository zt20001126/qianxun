<script setup lang="ts">
/** 可复用的预订人画像表单；证件号只在当前组件内存中用于本地推算。 */
import { onMounted, ref } from 'vue'
import { portraitApi } from '../../api/portraits'
import { bookerPartyTypeLabels, type BookerPartyType, type BookerPortraitInput } from '../../types/portraits'
import type { PortraitTagOption } from '../../types/portraits'
import { shanghaiDate } from '../../utils/operations'

const portrait = defineModel<BookerPortraitInput>({ required: true })
const props = withDefaults(defineProps<{ referenceDate?: string }>(), { referenceDate: '' })
const errorMessage = ref('')
const idNumber = ref('')
const tagInput = ref('')
const portraitTags = ref<PortraitTagOption[]>([])
const loadingTags = ref(false)
const savingTag = ref(false)

const partyTypes = Object.entries(bookerPartyTypeLabels) as [BookerPartyType, string][]

onMounted(() => { void loadTags() })

async function loadTags(): Promise<void> {
  loadingTags.value = true
  try {
    portraitTags.value = await portraitApi.tags()
  } catch (cause) {
    errorMessage.value = cause instanceof Error ? cause.message : '画像标签读取失败'
  } finally {
    loadingTags.value = false
  }
}
const provinceNames: Record<string, string> = {
  '11': '北京市', '12': '天津市', '13': '河北省', '14': '山西省', '15': '内蒙古自治区',
  '21': '辽宁省', '22': '吉林省', '23': '黑龙江省', '31': '上海市', '32': '江苏省',
  '33': '浙江省', '34': '安徽省', '35': '福建省', '36': '江西省', '37': '山东省',
  '41': '河南省', '42': '湖北省', '43': '湖南省', '44': '广东省', '45': '广西壮族自治区',
  '46': '海南省', '50': '重庆市', '51': '四川省', '52': '贵州省', '53': '云南省',
  '54': '西藏自治区', '61': '陕西省', '62': '甘肃省', '63': '青海省', '64': '宁夏回族自治区',
  '65': '新疆维吾尔自治区', '71': '台湾省', '81': '香港特别行政区', '82': '澳门特别行政区',
}
const idWeights = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
const idCheckCodes = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2']

function setAge(value: string): void {
  const age = value.trim() === '' ? null : Number(value)
  portrait.value = { ...portrait.value, age: age == null || !Number.isFinite(age) ? null : age }
}

function setPartyType(value: string): void {
  portrait.value = {
    ...portrait.value,
    party_type: value ? value as BookerPartyType : null,
  }
}

function setInferredRegion(value: string): void {
  portrait.value = { ...portrait.value, inferred_region: value.trim() || null }
}

async function addTag(): Promise<void> {
  const tag = tagInput.value.trim()
  if (!tag) return
  if (tag.length > 40) {
    errorMessage.value = '单个画像标签不能超过 40 个字符'
    return
  }
  const existingOption = portraitTags.value.find((item) => item.name.toLocaleLowerCase() === tag.toLocaleLowerCase())
  if (portrait.value.tags.some((current) => current.toLocaleLowerCase() === tag.toLocaleLowerCase())) {
    errorMessage.value = '该画像标签已添加到当前画像'
    return
  }
  if (portrait.value.tags.length >= 30) {
    errorMessage.value = '画像标签最多选择 30 个'
    return
  }
  savingTag.value = true
  try {
    const option = existingOption || await portraitApi.createTag(tag)
    if (!existingOption) portraitTags.value = [...portraitTags.value, option]
    portrait.value = { ...portrait.value, tags: [...portrait.value.tags, option.name] }
    tagInput.value = ''
    errorMessage.value = ''
  } catch (cause) {
    errorMessage.value = cause instanceof Error ? cause.message : '新增画像标签失败'
  } finally {
    savingTag.value = false
  }
}

function toggleTag(tag: string, checked: boolean): void {
  const nextTags = checked
    ? [...portrait.value.tags, tag]
    : portrait.value.tags.filter((item) => item !== tag)
  portrait.value = { ...portrait.value, tags: nextTags }
}

function inferFromId(): void {
  const value = idNumber.value.trim().toUpperCase()
  errorMessage.value = ''
  if (!value) return
  if (!/^\d{17}[\dX]$/.test(value)) {
    errorMessage.value = '请输入 18 位有效身份证号；号码只在本页临时处理'
    return
  }
  const province = provinceNames[value.slice(0, 2)]
  if (!province) {
    errorMessage.value = '无法从该证件号码推断省级地区'
    return
  }
  const birth = value.slice(6, 14)
  const year = Number(birth.slice(0, 4))
  const month = Number(birth.slice(4, 6))
  const day = Number(birth.slice(6, 8))
  const birthDate = new Date(Date.UTC(year, month - 1, day))
  if (birthDate.toISOString().slice(0, 10).replaceAll('-', '') !== birth) {
    errorMessage.value = '身份证号中的出生日期无效'
    return
  }
  const checkSum = idWeights.reduce((sum, weight, index) => sum + Number(value[index]) * weight, 0)
  if (idCheckCodes[checkSum % 11] !== value[17]) {
    errorMessage.value = '身份证号校验位不正确'
    return
  }
  const reference = props.referenceDate || shanghaiDate()
  const referenceYear = Number(reference.slice(0, 4))
  const referenceMonth = Number(reference.slice(5, 7))
  const referenceDay = Number(reference.slice(8, 10))
  const age = referenceYear - year
    - (referenceMonth < month || (referenceMonth === month && referenceDay < day) ? 1 : 0)
  if (age < 0 || age > 120) {
    errorMessage.value = '身份证号推算出的年龄超出支持范围'
    return
  }
  portrait.value = { ...portrait.value, age, inferred_region: province }
}
</script>

<template>
  <section class="portrait-fields" aria-labelledby="portrait-fields-heading">
    <div class="portrait-fields-heading">
      <div>
        <h4 id="portrait-fields-heading">预订人画像 <span>选填，可在订单完成后继续修改</span></h4>
        <p>身份证号只在当前页面本地推算年龄和省份，不会提交或保存。</p>
      </div>
    </div>

    <div class="portrait-fields-grid">
      <label class="portrait-field">
        <span>年龄</span>
        <input
          type="number"
          min="0"
          max="120"
          inputmode="numeric"
          :value="portrait.age ?? ''"
          placeholder="可单独填写"
          @input="setAge(($event.target as HTMLInputElement).value)"
        />
      </label>
      <label class="portrait-field">
        <span>用户群体</span>
        <select :value="portrait.party_type || ''" @change="setPartyType(($event.target as HTMLSelectElement).value)">
          <option value="">未填写</option>
          <option v-for="[value, label] in partyTypes" :key="value" :value="value">{{ label }}</option>
        </select>
      </label>
      <label class="portrait-field portrait-field-id">
        <span>身份证号（仅本地推算）</span>
        <input
          v-model="idNumber"
          type="password"
          maxlength="18"
          autocomplete="off"
          placeholder="输入后自动填入年龄和省份"
          @input="inferFromId"
        />
      </label>
      <div class="portrait-field">
        <span>地区（证件推断或手工填写）</span>
        <input
          type="text"
          maxlength="120"
          :value="portrait.inferred_region || ''"
          placeholder="例如：浙江省"
          @input="setInferredRegion(($event.target as HTMLInputElement).value)"
        />
      </div>
    </div>

    <div class="portrait-tag-section">
      <span class="portrait-tag-label">画像标签（预置选项，可新增并供所有画像选择）</span>
      <div class="portrait-tag-entry">
        <input id="portrait-tag-input" v-model="tagInput" maxlength="40" placeholder="输入新标签名称" @keydown.enter.prevent="addTag" />
        <button type="button" :disabled="!tagInput.trim() || savingTag" @click="addTag">{{ savingTag ? '添加中…' : '新增并选择' }}</button>
      </div>
      <p v-if="loadingTags" class="portrait-muted">正在读取标签…</p>
      <div v-else-if="portraitTags.length" class="portrait-tag-list">
        <label v-for="tag in portraitTags" :key="tag.id" class="portrait-tag-choice">
          <input
            type="checkbox"
            :checked="portrait.tags.includes(tag.name)"
            :disabled="!portrait.tags.includes(tag.name) && portrait.tags.length >= 30"
            @change="toggleTag(tag.name, ($event.target as HTMLInputElement).checked)"
          />
          {{ tag.name }}
        </label>
      </div>
      <p v-else class="portrait-muted">暂无标签选项，可新增一个。</p>
    </div>
    <p v-if="errorMessage" class="portrait-error" role="alert">{{ errorMessage }}</p>
  </section>
</template>

<style scoped>
.portrait-fields {
  padding: 15px;
  border: 1px solid #e4e8e8;
  border-radius: 10px;
  background: #fbfcfc;
}
.portrait-fields-heading { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.portrait-fields-heading h4 { margin: 0; color: #1f292b; font-size: 14px; font-weight: 600; }
.portrait-fields-heading h4 span { margin-left: 7px; color: #6e777a; font-size: 11px; font-weight: 400; }
.portrait-fields-heading p { margin: 4px 0 0; color: #6e777a; font-size: 11px; }
.portrait-fields-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 11px 14px; }
.portrait-field { display: grid; gap: 5px; color: #5f6a6d; font-size: 11px; }
.portrait-field input, .portrait-field select {
  min-width: 0;
  min-height: 35px;
  padding: 7px 9px;
  border: 1px solid #dce3e3;
  border-radius: 6px;
  color: #202b2d;
  background: #fff;
  font: inherit;
}
.portrait-field-id input { -webkit-text-security: disc; }
.portrait-tag-section { margin-top: 12px; }
.portrait-tag-label { display: block; margin-bottom: 7px; color: #5f6a6d; font-size: 11px; }
.portrait-tag-entry { display: flex; gap: 7px; }
.portrait-tag-entry input { flex: 1; min-width: 0; min-height: 34px; padding: 6px 8px; border: 1px solid #dce3e3; border-radius: 6px; font: inherit; }
.portrait-tag-entry button { padding: 0 12px; border: 1px solid #dce3e3; border-radius: 6px; color: #286b7b; background: #fff; }
.portrait-tag-entry button:disabled { opacity: .5; }
.portrait-tag-list { display: flex; flex-wrap: wrap; gap: 7px; margin-top: 8px; }
.portrait-tag-choice {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 30px;
  padding: 4px 8px;
  border: 1px solid #dce3e3;
  border-radius: 6px;
  color: #334144;
  background: #fff;
  font-size: 11px;
}
.portrait-tag-choice input { margin: 0; accent-color: #286b7b; }
.portrait-muted, .portrait-error { margin: 0; color: #6e777a; font-size: 11px; }
.portrait-error { margin-top: 8px; color: #b83b2a; }
@media (max-width: 580px) {
  .portrait-fields-grid { grid-template-columns: 1fr; }
}
</style>
