<template>
  <div class="auto-refresh-control">
    <el-switch 
      v-model="enabled" 
      @change="handleToggle"
    />
    <span class="label">自动刷新</span>
    <el-select 
      v-model="interval" 
      :disabled="!enabled"
      @change="handleIntervalChange"
      style="width: 120px"
    >
      <el-option label="5秒" :value="5000" />
      <el-option label="10秒" :value="10000" />
      <el-option label="30秒" :value="30000" />
      <el-option label="60秒" :value="60000" />
    </el-select>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface Emits {
  (e: 'toggle', enabled: boolean): void
  (e: 'interval-change', interval: number): void
}

const emit = defineEmits<Emits>()

const enabled = ref(false)
const interval = ref(10000) // Default to 10 seconds

const handleToggle = (value: boolean) => {
  emit('toggle', value)
}

const handleIntervalChange = (value: number) => {
  emit('interval-change', value)
}
</script>

<style scoped>
.auto-refresh-control {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.label {
  font-size: var(--font-size-md);
  color: var(--fluent-text-primary);
}

/* Responsive adjustments */
@media (max-width: 767px) {
  .auto-refresh-control {
    flex-direction: column;
    align-items: stretch;
    gap: var(--spacing-sm);
  }
  
  .auto-refresh-control .el-select {
    width: 100% !important;
  }
}
</style>
