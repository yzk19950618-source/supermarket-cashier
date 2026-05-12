<script setup>
import { computed, h, onMounted, reactive, ref } from 'vue'
import { NButton, NSpace, useMessage } from 'naive-ui'
import { post } from '@/utils/request'
import { normalizeRegionFlatRows } from '@/utils/regionData'
import { useCashierCacheStore } from '@/stores/cashierCache'

const message = useMessage()
const cashierCache = useCashierCacheStore()
const loading = ref(false)
const rawRows = ref([])

const f = reactive({
  name: '',
  parentCode: '',
  level: '',
})

const page = ref(1)
const pageSize = ref(10)

const levelFilterOptions = [
  { label: '请选择层级', value: '' },
  { label: '省级', value: '1' },
  { label: '市级', value: '2' },
  { label: '区县', value: '3' },
]

async function load() {
  loading.value = true
  try {
    const data = await post('/region/all')
    rawRows.value = normalizeRegionFlatRows(Array.isArray(data) ? data : [])
    cashierCache.setRegionCatalogFromNormalizedRows(rawRows.value)
  } catch (e) {
    message.error(e.message || '加载地区数据失败')
    rawRows.value = []
  } finally {
    loading.value = false
  }
}

const filteredRows = computed(() => {
  let list = rawRows.value
  const name = f.name?.trim()
  const pc = f.parentCode?.trim()
  if (name) list = list.filter((r) => r.name.includes(name))
  if (pc) list = list.filter((r) => String(r.parentCode).includes(pc))
  if (f.level !== '' && f.level != null) {
    const lv = Number(f.level)
    if (!Number.isNaN(lv)) list = list.filter((r) => r.level === lv)
  }
  return list
})

const pagedRows = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredRows.value.slice(start, start + pageSize.value)
})

const pageCount = computed(() =>
  Math.max(1, Math.ceil(filteredRows.value.length / pageSize.value)),
)

function search() {
  page.value = 1
}

function resetFilters() {
  f.name = ''
  f.parentCode = ''
  f.level = ''
  page.value = 1
}

function onAdd() {
  message.info('当前后端仅提供地区查询（GET/POST /api/region/all、/api/region/children），未开放地区增删改接口。')
}

function onEdit() {
  message.info('同上，暂无地区编辑接口。')
}

function onDelete() {
  message.info('同上，暂无地区删除接口。')
}

const columns = [
  { title: '编码', key: 'code', width: 120 },
  { title: '名称', key: 'name', ellipsis: { tooltip: true } },
  {
    title: '父级编码',
    key: 'parentCode',
    width: 130,
    render(row) {
      const v = row.parentCode
      return v === '' || v == null ? '-' : v
    },
  },
  { title: '层级', key: 'level', width: 72 },
  { title: '排序', key: 'sort', width: 72 },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    render() {
      return h(
        NSpace,
        { size: 8 },
        {
          default: () => [
            h(
              NButton,
              { size: 'small', text: true, type: 'primary', onClick: onEdit },
              { default: () => '编辑' },
            ),
            h(
              NButton,
              { size: 'small', text: true, type: 'error', onClick: onDelete },
              { default: () => '删除' },
            ),
          ],
        },
      )
    },
  },
]

onMounted(load)
</script>

<template>
  <n-space vertical size="large">
    <n-card class="page-card" :bordered="false" title="地址管理">
      <div class="toolbar toolbar--labeled-two-row">
        <div class="toolbar-filters-row">
          <div class="toolbar-labeled-field">
            <span class="toolbar-field-label">地区名称</span>
            <input v-model="f.name" type="text" class="native-filter toolbar-field-control" placeholder="请输入地区名称" />
          </div>
          <div class="toolbar-labeled-field">
            <span class="toolbar-field-label">父级编码</span>
            <input v-model="f.parentCode" type="text" class="native-filter toolbar-field-control" placeholder="请输入父级编码" />
          </div>
          <div class="toolbar-labeled-field">
            <span class="toolbar-field-label">层级</span>
            <select v-model="f.level" class="native-filter native-filter--level toolbar-field-control">
              <option v-for="opt in levelFilterOptions" :key="String(opt.value)" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>
          </div>
        </div>
        <div class="toolbar-actions-row">
          <n-space>
            <n-button type="primary" @click="search">查询</n-button>
            <n-button @click="resetFilters(); search()">重置</n-button>
            <n-button type="success" @click="onAdd">新增</n-button>
          </n-space>
        </div>
      </div>
      <n-data-table :columns="columns" :data="pagedRows" :loading="loading" size="small" />
      <div class="table-footer">
        <n-pagination
          v-model:page="page"
          v-model:page-size="pageSize"
          :page-count="pageCount"
          show-size-picker
          :page-sizes="[10, 20, 50]"
        />
      </div>
    </n-card>
  </n-space>
</template>

<style scoped>
.native-filter {
  height: 32px;
  padding: 0 10px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
  font-size: 14px;
  min-width: 160px;
  max-width: 220px;
  background: #fff;
  color: #303133;
}
.native-filter:focus {
  outline: none;
  border-color: #67c23a;
  box-shadow: 0 0 0 1px rgba(103, 194, 58, 0.2);
}
.native-filter--level {
  min-width: 140px;
  max-width: 200px;
}
.toolbar-labeled-field .native-filter {
  max-width: none;
  box-sizing: border-box;
}
</style>
