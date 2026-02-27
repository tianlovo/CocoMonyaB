# 通用组件

本目录包含可复用的通用组件，用于整个应用程序。

## DataTable 组件

通用数据表格组件，支持分页、加载状态、空状态和自定义操作。

### 功能特性

- ✅ 支持分页（10、20、50、100 条/页）
- ✅ 显示当前页码、总页数和总记录数
- ✅ 支持跳转到指定页码
- ✅ 加载状态显示
- ✅ 空状态提示
- ✅ 自定义列渲染（通过插槽）
- ✅ 操作列（编辑、删除等）
- ✅ 响应式设计

### 使用示例

```vue
<template>
  <DataTable
    :data="tableData"
    :columns="columns"
    :loading="loading"
    :pagination="pagination"
    :actions="actions"
    empty-type="empty"
    empty-message="暂无作者数据"
    empty-action-text="创建作者"
    @page-change="handlePageChange"
    @size-change="handleSizeChange"
    @action="handleAction"
    @empty-action="handleCreate"
  >
    <!-- 自定义列渲染 -->
    <template #aliases="{ row }">
      <el-tag v-for="alias in row.aliases" :key="alias" size="small">
        {{ alias }}
      </el-tag>
    </template>
  </DataTable>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import DataTable from '@/components/common/DataTable.vue'

const tableData = ref([
  { id: '1', name: '张三', aliases: ['别名1', '别名2'], createTime: '2024-01-01' }
])

const columns = [
  { prop: 'name', label: '名称', width: 150 },
  { prop: 'aliases', label: '别名', slot: 'aliases' },
  { prop: 'createTime', label: '创建时间', width: 180 }
]

const pagination = ref({
  current: 1,
  size: 10,
  total: 100
})

const actions = [
  { name: 'edit', label: '编辑', type: 'primary' },
  { name: 'delete', label: '删除', type: 'danger' }
]

const loading = ref(false)

const handlePageChange = (page: number) => {
  pagination.value.current = page
  // 重新加载数据
}

const handleSizeChange = (size: number) => {
  pagination.value.size = size
  pagination.value.current = 1
  // 重新加载数据
}

const handleAction = (actionName: string, row: any) => {
  if (actionName === 'edit') {
    // 编辑逻辑
  } else if (actionName === 'delete') {
    // 删除逻辑
  }
}

const handleCreate = () => {
  // 创建新项逻辑
}
</script>
```

### Props

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| data | Array | - | 表格数据 |
| columns | TableColumn[] | - | 列配置 |
| loading | boolean | false | 加载状态 |
| pagination | PaginationConfig | - | 分页配置 |
| actions | TableAction[] | [] | 操作按钮配置 |
| actionsWidth | string \| number | 200 | 操作列宽度 |
| emptyType | 'empty' \| 'no-result' \| 'error' | 'empty' | 空状态类型 |
| emptyMessage | string | '' | 空状态消息 |
| emptyActionText | string | '' | 空状态操作按钮文字 |

### Events

| 事件名 | 参数 | 说明 |
|--------|------|------|
| page-change | (page: number) | 页码变化 |
| size-change | (size: number) | 每页大小变化 |
| row-click | (row: any) | 行点击 |
| action | (actionName: string, row: any, index: number) | 操作按钮点击 |
| empty-action | () | 空状态操作按钮点击 |

### 类型定义

```typescript
interface TableColumn {
  prop: string          // 字段名
  label: string         // 列标题
  width?: string | number    // 列宽度
  minWidth?: string | number // 最小列宽度
  align?: 'left' | 'center' | 'right'  // 对齐方式
  slot?: string         // 自定义插槽名称
}

interface TableAction {
  name: string          // 操作名称
  label: string         // 按钮文字
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
  size?: 'large' | 'default' | 'small'
  icon?: string         // 图标
  disabled?: (row: any) => boolean  // 禁用条件
}

interface PaginationConfig {
  current: number       // 当前页码
  size: number          // 每页大小
  total: number         // 总记录数
}
```

## EmptyState 组件

空状态提示组件，用于显示列表为空、搜索无结果或错误状态。

### 功能特性

- ✅ 多种空状态类型（empty、no-result、error）
- ✅ 自定义提示文案
- ✅ 可选操作按钮
- ✅ 响应式设计
- ✅ Fluent Design 动画效果

### 使用示例

```vue
<template>
  <!-- 空列表状态 -->
  <EmptyState
    type="empty"
    message="暂无作者数据"
    action-text="创建作者"
    @action="handleCreate"
  />

  <!-- 搜索无结果 -->
  <EmptyState
    type="no-result"
    message="未找到匹配的作者"
  />

  <!-- 错误状态 -->
  <EmptyState
    type="error"
    message="加载失败，请重试"
    action-text="重新加载"
    @action="handleRetry"
  />
</template>

<script setup lang="ts">
import EmptyState from '@/components/common/EmptyState.vue'

const handleCreate = () => {
  // 创建新项逻辑
}

const handleRetry = () => {
  // 重试逻辑
}
</script>
```

### Props

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| type | 'empty' \| 'no-result' \| 'error' | 'empty' | 空状态类型 |
| message | string | '' | 提示消息（为空时使用默认消息） |
| actionText | string | '' | 操作按钮文字 |

### Events

| 事件名 | 参数 | 说明 |
|--------|------|------|
| action | () | 操作按钮点击 |

### 默认消息

- `empty`: "暂无数据"
- `no-result`: "无搜索结果"
- `error`: "加载失败，请重试"

## 需求覆盖

这些组件满足以下需求：

- **需求 9.1-9.6**: 分页功能（页码、总数、跳转、每页大小、Element Plus Pagination）
- **需求 13.2**: 列表为空时显示空状态提示和引导操作
- **需求 13.3**: 搜索无结果时显示"无搜索结果"提示
- **需求 13.1**: 数据加载时显示加载状态
- **需求 12.1-12.6**: 响应式设计支持
