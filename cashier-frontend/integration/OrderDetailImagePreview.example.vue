<script setup>
/**
 * 示例：订单详情附件大图预览（Naive n-image-group）。
 * props.urls：相对路径 /uploads/... 或绝对 URL 均可。
 */
import { computed } from 'vue'
import { NSpace, NImage, NText } from 'naive-ui'

const props = defineProps({
  urls: { type: Array, default: () => [] }
})

const list = computed(() => (Array.isArray(props.urls) ? props.urls : []).filter(Boolean))
</script>

<template>
  <div v-if="list.length">
    <n-text depth="3" style="display: block; margin-bottom: 8px">附件预览</n-text>
    <n-image-group>
      <n-space :size="12" align="start" style="flex-wrap: wrap">
        <n-image
          v-for="(src, i) in list"
          :key="i"
          width="120"
          height="120"
          object-fit="cover"
          :src="src"
        />
      </n-space>
    </n-image-group>
  </div>
  <n-text v-else depth="3">无附件</n-text>
</template>
