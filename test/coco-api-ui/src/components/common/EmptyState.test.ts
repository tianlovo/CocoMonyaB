import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import EmptyState from './EmptyState.vue'
import ElementPlus from 'element-plus'

describe('EmptyState Component', () => {
  it('should display default empty message when no message provided', () => {
    const wrapper = mount(EmptyState, {
      props: {
        type: 'empty'
      },
      global: {
        plugins: [ElementPlus]
      }
    })

    expect(wrapper.text()).toContain('暂无数据')
  })

  it('should display "无搜索结果" for no-result type', () => {
    const wrapper = mount(EmptyState, {
      props: {
        type: 'no-result'
      },
      global: {
        plugins: [ElementPlus]
      }
    })

    expect(wrapper.text()).toContain('无搜索结果')
  })

  it('should display custom message when provided', () => {
    const customMessage = '自定义空状态消息'
    const wrapper = mount(EmptyState, {
      props: {
        type: 'empty',
        message: customMessage
      },
      global: {
        plugins: [ElementPlus]
      }
    })

    expect(wrapper.text()).toContain(customMessage)
  })

  it('should display action button when actionText is provided', () => {
    const wrapper = mount(EmptyState, {
      props: {
        type: 'empty',
        actionText: '创建新项'
      },
      global: {
        plugins: [ElementPlus]
      }
    })

    const button = wrapper.find('.el-button')
    expect(button.exists()).toBe(true)
    expect(button.text()).toBe('创建新项')
  })

  it('should emit action event when action button is clicked', async () => {
    const wrapper = mount(EmptyState, {
      props: {
        type: 'empty',
        actionText: '创建新项'
      },
      global: {
        plugins: [ElementPlus]
      }
    })

    const button = wrapper.find('.el-button')
    await button.trigger('click')

    expect(wrapper.emitted('action')).toBeTruthy()
  })

  it('should not display action button when actionText is not provided', () => {
    const wrapper = mount(EmptyState, {
      props: {
        type: 'empty'
      },
      global: {
        plugins: [ElementPlus]
      }
    })

    const actionDiv = wrapper.find('.empty-state__action')
    expect(actionDiv.exists()).toBe(false)
  })

  it('should display error message for error type', () => {
    const wrapper = mount(EmptyState, {
      props: {
        type: 'error'
      },
      global: {
        plugins: [ElementPlus]
      }
    })

    expect(wrapper.text()).toContain('加载失败，请重试')
  })
})
