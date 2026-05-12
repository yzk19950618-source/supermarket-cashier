<script setup>
/**
 * 示例：省市区 + 详细地址，拼进结算请求的 remark（与收银台上线约定一致）。
 * 依赖：naive-ui；请将 postJson 换为你项目中的 request 封装。
 */
import { ref, computed, onMounted } from 'vue'
import { NFormItem, NCascader, NSpace, useMessage } from 'naive-ui'

const message = useMessage()
const regionOptions = ref([])
const regionValue = ref(null)
const receiverDetail = ref('')

async function postJson(url, body) {
  const token = localStorage.getItem('token') || ''
  const res = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(body ?? {})
  })
  const data = await res.json()
  if (data.code !== 200) throw new Error(data.message || 'request failed')
  return data.data
}

async function loadRegions() {
  try {
    const data = await postJson('/api/region/all', {})
    regionOptions.value = Array.isArray(data) ? data : []
  } catch (e) {
    message.error(String(e.message || e))
  }
}

onMounted(loadRegions)

/** 拼进 settle 的 remark（可与收银台其它分段用「；」拼接） */
const settleRemarkSegment = computed(() => {
  const codes = Array.isArray(regionValue.value) ? regionValue.value.join(',') : ''
  const parts = []
  if (codes) parts.push(`区域:${codes}`)
  if (receiverDetail.value?.trim()) parts.push(`收货地址:${receiverDetail.value.trim()}`)
  return parts.join('；')
})

defineExpose({ settleRemarkSegment, loadRegions })
</script>

<template>
  <n-space vertical :size="12">
    <n-form-item label="省市区">
      <n-cascader
        v-model:value="regionValue"
        placeholder="请选择"
        expand-trigger="click"
        :options="regionOptions"
        label-field="label"
        value-field="value"
        children-field="children"
        check-strategy="child"
        filterable
        clearable
        style="width: 100%"
      />
    </n-form-item>
    <n-form-item label="详细地址">
      <input v-model="receiverDetail" class="addr-input" type="text" placeholder="街道门牌等" />
    </n-form-item>
  </n-space>
</template>

<style scoped>
.addr-input {
  width: 100%;
  padding: 6px 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
}
</style>
