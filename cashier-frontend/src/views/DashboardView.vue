<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useMessage } from 'naive-ui'
import { post } from '@/utils/request'

const message = useMessage()
const loading = ref(false)
const dashboard = ref({})
const ranking = ref([])
const reminders = ref([])

const trendRef = ref(null)
let echartsLib
let trendChart

async function initCharts() {
  if (!echartsLib) echartsLib = await import('echarts')
  const echarts = echartsLib.default ?? echartsLib
  if (trendRef.value && !trendChart) trendChart = echarts.init(trendRef.value)
}

function setTrend(opt = { dates: [], amounts: [] }) {
  const amounts = (opt.amounts || []).map((a) => Number(a))
  trendChart?.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: opt.dates || [] },
    yAxis: { type: 'value' },
    series: [
      {
        type: 'line',
        smooth: true,
        areaStyle: {},
        data: amounts,
      },
    ],
  })
}

function resize() {
  trendChart?.resize()
}

async function load() {
  loading.value = true
  try {
    const [dash, trend, rank, repay] = await Promise.all([
      post('/statistics/dashboard'),
      post('/statistics/salesTrend', { days: 7 }),
      post('/statistics/salesRanking', { top: 10 }),
      post('/statistics/repaymentReminder', { days: 30 }),
    ])
    dashboard.value = dash || {}
    ranking.value = rank || []
    reminders.value = repay || []
    setTrend({
      dates: trend?.dates || [],
      amounts: trend?.amounts || [],
    })
  } catch (e) {
    message.error(e.message || '加载看板失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await initCharts()
  await load()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  trendChart?.dispose()
  trendChart = null
})
</script>

<template>
  <n-space vertical size="large">
    <n-grid :cols="3" :x-gap="16">
      <n-gi>
        <n-card class="page-card glass" :bordered="false">
          <n-statistic label="今日订单" :value="dashboard.todayOrderCount ?? 0" />
        </n-card>
      </n-gi>
      <n-gi>
        <n-card class="page-card glass" :bordered="false">
          <n-statistic label="今日总营业额" :value="Number(dashboard.todayTotalTurnover || 0)" prefix="￥" />
        </n-card>
      </n-gi>
      <n-gi>
        <n-card class="page-card glass" :bordered="false">
          <n-statistic label="今日新增待收款" :value="Number(dashboard.todayPendingCollection || 0)" prefix="￥" />
        </n-card>
      </n-gi>
    </n-grid>

    <n-grid :cols="4" :x-gap="16">
      <n-gi>
        <n-card class="page-card glass" :bordered="false">
          <n-statistic label="本年订单数" :value="dashboard.yearOrderCount ?? 0" />
        </n-card>
      </n-gi>
      <n-gi>
        <n-card class="page-card glass" :bordered="false">
          <n-statistic label="本年总已收款金额" :value="Number(dashboard.yearTotalCollectedAmount || 0)" prefix="￥" />
        </n-card>
      </n-gi>
      <n-gi>
        <n-card class="page-card glass" :bordered="false">
          <n-statistic label="本年总未收款金额" :value="Number(dashboard.yearTotalUncollectedAmount || 0)" prefix="￥" />
        </n-card>
      </n-gi>
      <n-gi>
        <n-card class="page-card glass" :bordered="false">
          <n-statistic label="本年总金额" :value="Number(dashboard.yearTotalAmount || 0)" prefix="￥" />
        </n-card>
      </n-gi>
    </n-grid>

    <n-card class="page-card" :bordered="false" title="销售趋势（近7天）">
      <div ref="trendRef" style="height: 320px" />
    </n-card>

    <n-grid :cols="2" :x-gap="16">
      <n-gi>
        <n-card class="page-card" :bordered="false" title="商品销量 TOP 10">
          <n-data-table
            :loading="loading"
            size="small"
            :pagination="false"
            :columns="[
              { title: '商品', key: 'goodsName' },
              { title: '销量', key: 'totalQuantity' },
              {
                title: '销售额',
                key: 'totalAmount',
                render: (r) => `￥${Number(r.totalAmount || 0).toFixed(2)}`,
              },
            ]"
            :data="ranking"
          />
        </n-card>
      </n-gi>
      <n-gi>
        <n-card class="page-card" :bordered="false" title="收款提醒（30天内）">
          <n-data-table
            :loading="loading"
            size="small"
            :pagination="false"
            :columns="[
              { title: '客户', key: 'customerName' },
              { title: '订单号', key: 'orderNo' },
              { title: '还款日期', key: 'repayDate' },
              { title: '剩余天数', key: 'daysLeft' },
              {
                title: '应收',
                key: 'realAmount',
                render: (r) => `${Number(r.realAmount || 0).toFixed(2)}`,
              },
            ]"
            :data="reminders"
          />
        </n-card>
      </n-gi>
    </n-grid>
  </n-space>
</template>

