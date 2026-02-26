<template>
  <div class="skeleton-loader">
    <div v-if="type === 'table'" class="skeleton-table">
      <div class="skeleton-table-header">
        <div 
          v-for="i in columns" 
          :key="i" 
          class="skeleton-cell skeleton-shimmer"
        ></div>
      </div>
      <div 
        v-for="row in rows" 
        :key="row" 
        class="skeleton-table-row"
      >
        <div 
          v-for="col in columns" 
          :key="col" 
          class="skeleton-cell skeleton-shimmer"
        ></div>
      </div>
    </div>
    
    <div v-else-if="type === 'card'" class="skeleton-card">
      <div class="skeleton-card-header skeleton-shimmer"></div>
      <div class="skeleton-card-body">
        <div 
          v-for="i in lines" 
          :key="i" 
          class="skeleton-line skeleton-shimmer"
          :style="{ width: getLineWidth(i) }"
        ></div>
      </div>
    </div>
    
    <div v-else-if="type === 'list'" class="skeleton-list">
      <div 
        v-for="i in items" 
        :key="i" 
        class="skeleton-list-item"
      >
        <div class="skeleton-avatar skeleton-shimmer"></div>
        <div class="skeleton-content">
          <div class="skeleton-line skeleton-shimmer" style="width: 60%"></div>
          <div class="skeleton-line skeleton-shimmer" style="width: 80%"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  type?: 'table' | 'card' | 'list'
  rows?: number
  columns?: number
  lines?: number
  items?: number
}

withDefaults(defineProps<Props>(), {
  type: 'table',
  rows: 5,
  columns: 4,
  lines: 3,
  items: 3
})

const getLineWidth = (index: number) => {
  const widths = ['100%', '90%', '70%', '85%', '95%']
  return widths[index % widths.length]
}
</script>

<style scoped>
.skeleton-loader {
  padding: var(--spacing-md);
}

/* Shimmer animation */
.skeleton-shimmer {
  background: linear-gradient(
    90deg,
    var(--fluent-bg-alt) 0%,
    var(--fluent-bg-hover) 50%,
    var(--fluent-bg-alt) 100%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

/* Table skeleton */
.skeleton-table {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.skeleton-table-header,
.skeleton-table-row {
  display: flex;
  gap: var(--spacing-sm);
}

.skeleton-table-header .skeleton-cell {
  height: 40px;
  border-radius: var(--radius-sm);
}

.skeleton-table-row .skeleton-cell {
  height: 60px;
  border-radius: var(--radius-sm);
}

.skeleton-cell {
  flex: 1;
}

/* Card skeleton */
.skeleton-card {
  border-radius: var(--radius-md);
  overflow: hidden;
}

.skeleton-card-header {
  height: 60px;
  margin-bottom: var(--spacing-md);
  border-radius: var(--radius-md);
}

.skeleton-card-body {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.skeleton-line {
  height: 16px;
  border-radius: var(--radius-sm);
}

/* List skeleton */
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.skeleton-list-item {
  display: flex;
  gap: var(--spacing-md);
  align-items: center;
}

.skeleton-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  flex-shrink: 0;
}

.skeleton-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

/* Responsive adjustments */
@media (max-width: 767px) {
  .skeleton-loader {
    padding: var(--spacing-sm);
  }
  
  .skeleton-table-row .skeleton-cell {
    height: 50px;
  }
  
  .skeleton-avatar {
    width: 40px;
    height: 40px;
  }
}
</style>
