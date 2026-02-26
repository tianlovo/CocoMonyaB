import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { ElDialog, ElForm, ElButton } from 'element-plus'
import FormDialog from './FormDialog.vue'

describe('FormDialog Component', () => {
  it('should render dialog with title', () => {
    const wrapper = mount(FormDialog, {
      props: {
        visible: true,
        title: '测试表单',
        formData: {}
      },
      global: {
        components: {
          ElDialog,
          ElForm,
          ElButton
        }
      }
    })

    expect(wrapper.find('.el-dialog__header').text()).toContain('测试表单')
  })

  it('should emit update:visible and cancel when cancel button is clicked', async () => {
    const wrapper = mount(FormDialog, {
      props: {
        visible: true,
        title: '测试表单',
        formData: {}
      },
      global: {
        components: {
          ElDialog,
          ElForm,
          ElButton
        }
      }
    })

    const cancelButton = wrapper.findAll('.el-button')[0]
    await cancelButton.trigger('click')

    expect(wrapper.emitted('update:visible')).toBeTruthy()
    expect(wrapper.emitted('update:visible')?.[0]).toEqual([false])
    expect(wrapper.emitted('cancel')).toBeTruthy()
  })

  it('should show loading state on submit button when loading is true', () => {
    const wrapper = mount(FormDialog, {
      props: {
        visible: true,
        title: '测试表单',
        formData: {},
        loading: true
      },
      global: {
        components: {
          ElDialog,
          ElForm,
          ElButton
        }
      }
    })

    const submitButton = wrapper.findAll('.el-button')[1]
    expect(submitButton.attributes('loading')).toBeDefined()
    expect(submitButton.text()).toContain('提交中')
  })

  it('should disable cancel button when loading', () => {
    const wrapper = mount(FormDialog, {
      props: {
        visible: true,
        title: '测试表单',
        formData: {},
        loading: true
      },
      global: {
        components: {
          ElDialog,
          ElForm,
          ElButton
        }
      }
    })

    const cancelButton = wrapper.findAll('.el-button')[0]
    expect(cancelButton.attributes('disabled')).toBeDefined()
  })

  it('should apply custom width', () => {
    const wrapper = mount(FormDialog, {
      props: {
        visible: true,
        title: '测试表单',
        formData: {},
        width: '800px'
      },
      global: {
        components: {
          ElDialog,
          ElForm,
          ElButton
        }
      }
    })

    const dialog = wrapper.findComponent(ElDialog)
    expect(dialog.props('width')).toBe('800px')
  })

  it('should convert numeric width to px string', () => {
    const wrapper = mount(FormDialog, {
      props: {
        visible: true,
        title: '测试表单',
        formData: {},
        width: 800
      },
      global: {
        components: {
          ElDialog,
          ElForm,
          ElButton
        }
      }
    })

    const dialog = wrapper.findComponent(ElDialog)
    expect(dialog.props('width')).toBe('800px')
  })

  it('should pass formData to slot', () => {
    const formData = { name: '测试', age: 25 }
    const wrapper = mount(FormDialog, {
      props: {
        visible: true,
        title: '测试表单',
        formData
      },
      slots: {
        default: `<div class="test-slot">{{ formData.name }}</div>`
      },
      global: {
        components: {
          ElDialog,
          ElForm,
          ElButton
        }
      }
    })

    expect(wrapper.html()).toContain('test-slot')
  })

  it('should disable form when loading', () => {
    const wrapper = mount(FormDialog, {
      props: {
        visible: true,
        title: '测试表单',
        formData: {},
        loading: true
      },
      global: {
        components: {
          ElDialog,
          ElForm,
          ElButton
        }
      }
    })

    const form = wrapper.findComponent(ElForm)
    expect(form.props('disabled')).toBe(true)
  })

  it('should not close dialog on click modal', () => {
    const wrapper = mount(FormDialog, {
      props: {
        visible: true,
        title: '测试表单',
        formData: {}
      },
      global: {
        components: {
          ElDialog,
          ElForm,
          ElButton
        }
      }
    })

    const dialog = wrapper.findComponent(ElDialog)
    expect(dialog.props('closeOnClickModal')).toBe(false)
  })

  it('should prevent escape key when loading', () => {
    const wrapper = mount(FormDialog, {
      props: {
        visible: true,
        title: '测试表单',
        formData: {},
        loading: true
      },
      global: {
        components: {
          ElDialog,
          ElForm,
          ElButton
        }
      }
    })

    const dialog = wrapper.findComponent(ElDialog)
    expect(dialog.props('closeOnPressEscape')).toBe(false)
  })
})
