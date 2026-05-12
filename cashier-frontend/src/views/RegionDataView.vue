<script setup>
import { onMounted, ref } from 'vue'
import { useMessage } from 'naive-ui'
import { get, post } from '@/utils/request'

const message = useMessage()
const loading = ref(false)
const treeData = ref([])
const exploreParentId = ref('110000')
const childrenPreview = ref(null)
const childrenLoading = ref(false)

function mapTree(nodes) {
  return (nodes || []).map((n) => {
    const raw = n.children
    const hasSub = Array.isArray(raw) && raw.length > 0
    return {
      key: String(n.value ?? n.code ?? n.id ?? ''),
      label: `${n.label ?? n.name ?? ''}（${n.value ?? n.code ?? ''}）`,
      isLeaf: n.isLeaf === true || !hasSub,
      children: hasSub ? mapTree(raw) : undefined,
    }
  })
}

async function loadTree() {
  loading.value = true
  try {
    const data = await post('/region/all')
    treeData.value = mapTree(Array.isArray(data) ? data : [])
  } catch (e) {
    message.error(e.message || '加载区域失败')
  } finally {
    loading.value = false
  }
}

async function loadChildrenPreview() {
  const pid = exploreParentId.value?.trim() ?? ''
  childrenLoading.value = true
  childrenPreview.value = null
  try {
    const list = await get('/region/children', { parentId: pid || undefined })
    childrenPreview.value = list
  } catch (e) {
    message.error(e.message || '查询子节点失败')
  } finally {
    childrenLoading.value = false
  }
}

onMounted(loadTree)
</script>

<template>
  <n-space vertical size="large">
    <n-card class="page-card" :bordered="false" title="省市区数据（只读）">
      <n-alert type="info" title="说明" style="margin-bottom: 16px">
        后端仅提供区域查询：<code>/api/region/all</code>（GET/POST）与
        <code>/api/region/children</code>（GET/POST，参数 parentId）。不提供地区 CRUD。
      </n-alert>
      <n-spin :show="loading">
        <n-tree
          block-line
          expand-on-click
          selectable
          :data="treeData"
          default-expand-all
        />
      </n-spin>
    </n-card>

    <n-card class="page-card" :bordered="false" title="懒加载子节点演示（GET /region/children）">
      <n-space vertical>
        <n-text depth="3">
          填写父级 region code（空表示省级根列表，与「全部区域」一致）。下方展示接口返回的原始子节点数组。
        </n-text>
        <n-space align="center">
          <n-input v-model:value="exploreParentId" placeholder="例如 110000 或留空" style="width: 280px" />
          <n-button type="primary" :loading="childrenLoading" @click="loadChildrenPreview">查询子节点</n-button>
        </n-space>
        <n-card v-if="childrenPreview != null" embedded size="small">
          <pre class="json-preview">{{ JSON.stringify(childrenPreview, null, 2) }}</pre>
        </n-card>
      </n-space>
    </n-card>
  </n-space>
</template>

<style scoped>
.json-preview {
  margin: 0;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
