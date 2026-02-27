import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { ElDialog, ElButton, ElIcon } from 'element-plus'
import { InfoFilled, WarningFilled, CircleCloseFilled } from '@element-plus/icons-vue'
import ConfirmDialog from './ConfirmDialog.vue'

describe('ConfirmDialog Component', () => {
  it('should render dialog with title and message', () => {
    const wrapper = mount(ConfirmDialog, {
      props: {
        visible: true,
        title: '确认删除',
        message: '确定要删除这条记录吗？'
      },
      global: {
        components: {
          ElDialog,
          ElButton,
          ElIcon,
          InfoFilled,
          WarningFilled,
          CircleCloseFilled
        }
      }
    })

    expect(wrapper.find('.el-dialog__header').text()).toContain('确认删除')
    expect(wrapper.find('.confirm-message').text()).toContain('确定要删除这条记录吗？')
  })

  it('should emit confirm when confirm button is clicked', async () => {
    const wrapper = mount(ConfirmDialog, {
      props: {
        visible: true,
        title: '确认操作',
        message: '确定要执行此操作吗？'
      },
      global: {
        components: {
          ElDialog,
          ElButton,
          ElIcon,
          InfoFilled,
          WarningFilled,
          CircleCloseFilled
        }
      }
    })

    const confirmButton = wrapper.findAll('.el-button')[1]
    await confirmButton.trigger('click')

    expect(wrapper.emitted('confirm')).toBeTruthy()
    expect(wrapper.emitted('update:visible')).toBeTruthy()
    expect(wrapper.emitted('update:visible')?.[0]).toEqual([false])
  })

  it('should emit cancel when cancel button is clicked', async () => {
    const wrapper = mount(ConfirmDialog, {
      props: {
        visible: true,
        title: '确认操作',
        message: '确定要执行此操作吗？'
      },
      global: {
        components: {
          ElDialog,
          ElButton,
          ElIcon,
          InfoFilled,
          WarningFilled,
          CircleCloseFilled
        }
      }
    })

    const cancelButton = wrapper.findAll('.el-button')[0]
    await cancelButton.trigger('click')

    expect(wrapper.emitted('cancel')).toBeTruthy()
    expect(wrapper.emitted('update:visible')).toBeTruthy()
    expect(wrapper.emitted('update:visible')?.[0]).toEqual([false])
  })

  it('should display warning icon for warning type', () => {
    const wrapper = mount(ConfirmDialog, {
      props: {
        visible: true,
        title: '警告',
        message: '这是一个警告',
        type: 'warning'
      },
      global: {
        components: {
          ElDialog,
          ElButton,
          ElIcon,
          InfoFilled,
          WarningFilled,
          CircleCloseFilled
        }
      }
    })

    expect(wrapper.findComponent(WarningFilled).exists()).toBe(true)
  })

  it('should display danger icon for danger type', () => {
    const wrapper = mount(ConfirmDialog, {
      props: {
        visible: true,
        title: '危险操作',
        message: '这是一个危险操作',
        type: 'danger'
      },
      global: {
        components: {
          ElDialog,
          ElButton,
          ElIcon,
          InfoFilled,
          WarningFilled,
          CircleCloseFilled
        }
      }
    })

    expect(wrapper.findComponent(CircleCloseFilled).exists()).toBe(true)
  })

  it('should display info icon for info type', () => {
    const wrapper = mount(ConfirmDialog, {
      props: {
        visible: true,
        title: '提示',
        message: '这是一个提示',
        type: 'info'
      },
      global: {
        components: {
          ElDialog,
          ElButton,
          ElIcon,
          InfoFilled,
          WarningFilled,
          CircleCloseFilled
        }
      }
    })

    expect(wrapper.findComponent(InfoFilled).exists()).toBe(true)
  })

  it('should use custom confirm and cancel text', () => {
    const wrapper = mount(ConfirmDialog, {
      props: {
        visible: true,
        title: '确认',
        message: '确定吗？',
        confirmText: '是的',
        cancelText: '不了'
      },
      global: {
        components: {
          ElDialog,
          ElButton,
          ElIcon,
          InfoFilled,
          WarningFilled,
          CircleCloseFilled
        }
      }
    })

    const buttons = wrapper.findAll('.el-button')
    expect(buttons[0].text()).toBe('不了')
    expect(buttons[1].text()).toBe('是的')
  })

  it('should apply danger button type for danger type', () => {
    const wrapper = mount(ConfirmDialog, {
      props: {
        visible: true,
        title: '删除',
        message: '确定删除？',
        type: 'danger'
      },
      global: {
        components: {
          ElDialog,
          ElButton,
          ElIcon,
          InfoFilled,
          WarningFilled,
          CircleCloseFilled
        }
      }
    })

    const confirmButton = wrapper.findAll('.el-button')[1]
    expect(confirmButton.attributes('type')).toBe('danger')
  })

  it('should apply warning button type for warning type', () => {
    const wrapper = mount(ConfirmDialog, {
      props: {
        visible: true,
        title: '警告',
        message: '确定继续？',
        type: 'warning'
      },
      global: {
        components: {
          ElDialog,
          ElButton,
          ElIcon,
          InfoFilled,
          WarningFilled,
          CircleCloseFilled
        }
      }
    })

    const confirmButton = wrapper.findAll('.el-button')[1]
    expect(confirmButton.attributes('type')).toBe('warning')
  })

  it('should apply custom width', () => {
    const wrapper = mount(ConfirmDialog, {
      props: {
        visible: true,
        title: '确认',
        message: '确定吗？',
        width: '600px'
      },
      global: {
        components: {
          ElDialog,
          ElButton,
          ElIcon,
          InfoFilled,
          WarningFilled,
          CircleCloseFilled
        }
      }
    })

    const dialog = wrapper.findComponent(ElDialog)
    expect(dialog.props('width')).toBe('600px')
  })

  it('should convert numeric width to px string', () => {
    const wrapper = mount(ConfirmDialog, {
      props: {
        visible: true,
        title: '确认',
        message: '确定吗？',
        width: 600
      },
      global: {
        components: {
          ElDialog,
          ElButton,
          ElIcon,
          InfoFilled,
          WarningFilled,
          CircleCloseFilled
        }
      }
    })

    const dialog = wrapper.findComponent(ElDialog)
    expect(dialog.props('width')).toBe('600px')
  })

  it('should not close dialog on click modal', () => {
    const wrapper = mount(ConfirmDialog, {
      props: {
        visible: true,
        title: '确认',
        message: '确定吗？'
      },
      global: {
        components: {
          ElDialog,
          ElButton,
          ElIcon,
          InfoFilled,
          WarningFilled,
          CircleCloseFilled
        }
      }
    })

    const dialog = wrapper.findComponent(ElDialog)
    expect(dialog.props('closeOnClickModal')).toBe(false)
  })

  it('should handle multiline messages', () => {
    const multilineMessage = '第一行\n第二行\n第三行'
    const wrapper = mount(ConfirmDialog, {
      props: {
        visible: true,
        title: '确认',
        message: multilineMessage
      },
      global: {
        components: {
          ElDialog,
          ElButton,
          ElIcon,
          InfoFilled,
          WarningFilled,
          CircleCloseFilled
        }
      }
    })

    const messageElement = wrapper.find('.confirm-message')
    expect(messageElement.text()).toContain('第一行')
    expect(messageElement.text()).toContain('第二行')
    expect(messageElement.text()).toContain('第三行')
  })
})
