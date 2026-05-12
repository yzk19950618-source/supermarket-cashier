<script setup>
import {
  NConfigProvider,
  NDialogProvider,
  NMessageProvider,
  zhCN,
  dateZhCN,
} from 'naive-ui'
import { RouterView } from 'vue-router'

/** Element Plus 风格主色 #67C23A */
const themeOverrides = {
  common: {
    primaryColor: '#67c23a',
    primaryColorHover: '#5daf34',
    primaryColorPressed: '#529b2e',
    primaryColorSuppl: '#85ce61',
    infoColor: '#409eff',
    successColor: '#67c23a',
    warningColor: '#e6a23c',
    errorColor: '#f56c6c',
    borderRadius: '4px',
    fontSize: '14px',
    heightMedium: '32px',
  },
  Card: {
    borderRadius: '4px',
    paddingMedium: '20px',
    titleFontSizeMedium: '16px',
  },
  Button: {
    borderRadiusMedium: '4px',
    heightMedium: '32px',
  },
  Input: {
    borderRadius: '4px',
    heightMedium: '32px',
  },
  Layout: {
    colorEmbedded: '#f5f7fa',
  },
  Menu: {
    itemColorActive: 'rgba(103, 194, 58, 0.18)',
    itemTextColorActive: '#529b2e',
    itemIconColorActive: '#529b2e',
  },
}
</script>

<template>
  <n-config-provider :locale="zhCN" :date-locale="dateZhCN" :theme-overrides="themeOverrides">
    <n-dialog-provider>
      <n-message-provider>
        <RouterView />
      </n-message-provider>
    </n-dialog-provider>
  </n-config-provider>
</template>
