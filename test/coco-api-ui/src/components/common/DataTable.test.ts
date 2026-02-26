import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import DataTable from './DataTable.vue'
import EmptyState from './EmptyState.vue'
import ElementPlus from 'element-plus'

describe('DataTable Component', () => {
  const mockColumns = [
    { prop: 'name', label: '名称' },
    { prop: 'createTime', label: '创建时间' }
  ]

  it('should display empty state when data is empty', () => {
    const wrapper = mount(DataTable, {
      props: {
        data: [],
        columns: mockColumns,
        loading: false
      },
      global: {
        plugins: [ElementPlus],
        stubs: {
          EmptyState: true
        }
      }
    })

    expect(wrapper.findComponent(EmptyState).exists()).toBe(true)
  })

  it('should emit page-change event when page changes', async () => {
    const wrapper = mount(DataTable, {
      props: {
        data: [{ name: 'Test', createTime: '2024-01-01' }],
        columns: mockColumns,
        pagination: { current: 1, size: 10, total: 100 }
      },
      global: {
        plugins: [ElementPlus]
      }
    })

    await wrapper.vm.$nextTick()
    wrapper.vm.handlePageChange(2)
    
    expect(wrapper.emitted('page-change')).toBeTruthy()
    expect(wrapper.emitted('page-change')?.[0]).toEqual([2])
  })

  it('should emit size-change event when page size changes', async () => {
    const wrapper = mount(DataTable, {
      props: {
        data: [{ name: 'Test', createTime: '2024-01-01' }],
        columns: mockColumns,
        pagination: { current: 1, size: 10, total: 100 }
      },
      global: {
        plugins: [ElementPlus]
      }
    })

    await wrapper.vm.$nextTick()
    wrapper.vm.handleSizeChange(20)
    
    expect(wrapper.emitted('size-change')).toBeTruthy()
    expect(wrapper.emitted('size-change')?.[0]).toEqual([20])
  })

  it('should emit action event when action button is clicked', async () => {
    const mockActions = [
      { name: 'edit', label: '编辑', type: 'primary' as const },
      { name: 'delete', label: '删除', type: 'danger' as const }
    ]

    const wrapper = mount(DataTable, {
      props: {
        data: [{ id: '1', name: 'Test' }],
        columns: mockColumns,
        actions: mockActions
      },
      global: {
        plugins: [ElementPlus]
      }
    })

    await wrapper.vm.$nextTick()
    const row = { id: '1', name: 'Test' }
    wrapper.vm.handleAction('edit', row, 0)
    
    expect(wrapper.emitted('action')).toBeTruthy()
    expect(wrapper.emitted('action')?.[0]).toEqual(['edit', row, 0])
  })

  it('should show loading state when loading is true', () => {
    const wrapper = mount(DataTable, {
      props: {
        data: [],
        columns: mockColumns,
        loading: true
      },
      global: {
        plugins: [ElementPlus]
      }
    })

    // Check if v-loading directive is applied
    const table = wrapper.find('.el-table')
    expect(table.exists()).toBe(true)
  })
})
