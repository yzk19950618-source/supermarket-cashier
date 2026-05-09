<script setup>
/**
 * 示例：省市区 + 上传发票/凭证，结算时随 settle 提交。
 * 依赖：naive-ui；请将 postJson/uploadFile 换为你项目中的 request 封装。
 */
import { ref, computed, onMounted } from 'vue'
import { NFormItem, NCascader, NUpload, NImage, NSpace, useMessage } from 'naive-ui'

const message = useMessage()
const regionOptions = ref([])
const regionValue = ref(null)
const receiverAddress = ref('')
const attachmentUrls = ref([])

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

/** 懒加载子节点（若整树已够用可不传 load） */
const uploadAction = '/api/file/upload'

function onUploadFinish({ event }) {
  const xhr = event?.target
  let body
  try {
    body = JSON.parse(xhr?.responseText || '{}')
  } catch {
    body = {}
  }
  if (body.code === 200 && body.data?.url) {
    attachmentUrls.value = [...attachmentUrls.value, body.data.url]
    message.success('上传成功')
  } else {
    message.error(body.message || '上传失败')
  }
}

/** 供父组件调用：拼进 settle 请求体 */
const settleExtras = computed(() => ({
  receiverRegionCodes: Array.isArray(regionValue.value) ? regionValue.value.join(',') : '',
  receiverAddress: receiverAddress.value,
  attachmentUrls: [...attachmentUrls.value]
}))

defineExpose({ settleExtras, loadRegions })
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
      <input v-model="receiverAddress" class="addr-input" type="text" placeholder="街道门牌等" />
    </n-form-item>
    <n-form-item label="发票/凭证">
      <n-upload
        :action="uploadAction"
        :headers="{
          Authorization: `Bearer ${localStorage.getItem('token') || ''}`
        }"
        list-type="image-card"
        accept="image/*"
        @finish="onUploadFinish"
      />
      <n-space :size="8" style="margin-top: 8px; flex-wrap: wrap">
        <n-image
          v-for="(u, i) in attachmentUrls"
          :key="i"
          width="72"
          height="72"
          object-fit="cover"
          :src="u"
          :preview-disabled="false"
        />
      </n-space>
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
