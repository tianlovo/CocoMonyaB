<template>
  <div class="data-table">
    <el-table
      :data="data"
      v-loading="loading"
      stripe
      style="width: 100%"
      @row-click="handleRowClick"
      class="fluent-card"
    >
      <el-table-column
        v-for="column in columns"
        :key="column.prop"
        :prop="column.prop"
        :label="column.label"
        :width="column.width"
        :min-width="column.minWidth"
        :align="column.align || 'left'"
      >
        <template #default="scope">
          <slot
            v-if="column.slot"
            :name="column.slot"
            :row="scope.row"
            :column="column"
            :$index="scope.$index"
          />
          <span v-else>{{ scope.row[column.prop] }}</span>
        </template>
      </el-table-column>

      <el-table-column
        v-if="actions && actions.length > 0"
        label="操作"
        :width="actionsWidth"
        align="center"
        fixed="right"
      >
        <template #default="scope">
          <el-button
            v-for="action in actions"
            :key="action.name"
            :type="action.type || 'primary'"
            :size="action.size || 'small'"
            :icon="action.icon"
            :disabled="action.disabled?.(scope.row)"
            @click.stop="handleAction(action.name, scope.row, scope.$index)"
            link
          >
            {{ action.label }}
          </el-button>
        </template>
      </el-table-column>

      <template #empty>
        <EmptyState
          :type="emptyType"
          :message="emptyMessage"
          :action-text="emptyActionText"
          @action="handleEmptyAction"
        />
      </template>
    </el-table>

    <el-pagination
      v-if="pagination"
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :page-sizes="[10, 20, 50, 100]"
      :total="pagination.total"
      layout="total, sizes, prev, pager, next, jumper"
      class="pagination"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import EmptyState from './EmptyState.vue'

export interface TableColumn {
  prop: string
  label: string
  width?: string | number
  minWidth?: string | number
  align?: 'left' | 'center' | 'right'
  slot?: string
}

export interface TableAction {
  name: string
  label: string
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
  size?: 'large' | 'default' | 'small'
  icon?: string
  disabled?: (row: any) => boolean
}

export interface PaginationConfig {
  current: number
  size: number
  total: number
}

interface Props {
  data: any[]
  columns: TableColumn[]
  loading?: boolean
  pagination?: PaginationConfig
  actions?: TableAction[]
  actionsWidth?: string | number
  emptyType?: 'empty' | 'no-result' | 'error'
  emptyMessage?: string
  emptyActionText?: string
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  actions: () => [],
  actionsWidth: 200,
  emptyType: 'empty',
  emptyMessage: '',
  emptyActionText: ''
})

interface Emits {
  (e: 'page-change', page: number): void
  (e: 'size-change', size: number): void
  (e: 'row-click', row: any): void
  (e: 'action', actionName: string, row: any, index: number): void
  (e: 'empty-action'): void
}

const emit = defineEmits<Emits>()

const currentPage = computed({
  get: () => props.pagination?.current || 1,
  set: (_val) => {
    // Value is updated through emit
  }
})

const pageSize = computed({
  get: () => props.pagination?.size || 10,
  set: (_val) => {
    // Value is updated through emit
  }
})

const handlePageChange = (page: number) => {
  emit('page-change', page)
}

const handleSizeChange = (size: number) => {
  emit('size-change', size)
}

const handleRowClick = (row: any) => {
  emit('row-click', row)
}

const handleAction = (actionName: string, row: any, index: number) => {
  emit('action', actionName, row, index)
}

const handleEmptyAction = () => {
  emit('empty-action')
}
</script>

<style scoped>
.data-table {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding: var(--spacing-md) 0;
}

/* Responsive adjustments */
@media (max-width: 767px) {
  .pagination {
    justify-content: center;
  }
  
  :deep(.el-pagination) {
    flex-wrap: wrap;
    gap: var(--spacing-sm);
  }
}
</style>
