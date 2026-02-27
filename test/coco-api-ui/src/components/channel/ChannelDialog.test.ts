import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { ElDialog, ElForm, ElButton, ElSelect, ElInput, ElSwitch } from 'element-plus'
import ChannelDialog from './ChannelDialog.vue'
import { channelApi } from '@/api/channel'
import type { TgChannel } from '@/types/models'

vi.mock('@/api/channel', () => ({
  channelApi: {
    getTgChannels: vi.fn()
  }
}))

vi.mock('@/stores/channel', () => ({
  useChannelStore: vi.fn(() => ({
    createChannel: vi.fn(),
    updateChannel: vi.fn()
  }))
}))

describe('ChannelDialog Component', () => {
  const mockTgChannels: TgChannel[] = [
    {
      chatId: -1001234567890,
      title: 'Test Channel 1',
      username: 'testchannel1',
      type: 'channel',
      isChannel: true,
      memberCount: 100,
      description: 'Test description'
    },
    {
      chatId: -1001234567891,
      title: 'Test Channel 2',
      username: null,
      type: 'channel',
      isChannel: true,
      memberCount: 200,
      description: null
    }
  ]

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should render dialog with title for create mode', () => {
    const wrapper = mount(ChannelDialog, {
      props: {
        visible: true,
        channel: null
      },
      global: {
        components: {
          ElDialog,
          ElForm,
          ElButton,
          ElSelect,
          ElInput,
          ElSwitch
        }
      }
    })

    expect(wrapper.text()).toContain('新建频道')
  })

  it('should render dialog with title for edit mode', () => {
    const wrapper = mount(ChannelDialog, {
      props: {
        visible: true,
        channel: {
          id: '1',
          channelId: -1001234567890,
          channelUsername: 'testchannel',
          channelTitle: 'Test Channel',
          monitoringStatus: true,
          createTime: '2024-01-01T00:00:00Z',
          updateTime: '2024-01-01T00:00:00Z'
        }
      },
      global: {
        components: {
          ElDialog,
          ElForm,
          ElButton,
          ElSelect,
          ElInput,
          ElSwitch
        }
      }
    })

    expect(wrapper.text()).toContain('编辑频道')
  })

  it('should show quick select dropdown in create mode', () => {
    const wrapper = mount(ChannelDialog, {
      props: {
        visible: true,
        channel: null
      },
      global: {
        components: {
          ElDialog,
          ElForm,
          ElButton,
          ElSelect,
          ElInput,
          ElSwitch
        }
      }
    })

    expect(wrapper.text()).toContain('快速选择')
    expect(wrapper.text()).toContain('从已登录账号的频道列表中选择')
  })

  it('should not show quick select dropdown in edit mode', () => {
    const wrapper = mount(ChannelDialog, {
      props: {
        visible: true,
        channel: {
          id: '1',
          channelId: -1001234567890,
          channelUsername: 'testchannel',
          channelTitle: 'Test Channel',
          monitoringStatus: true,
          createTime: '2024-01-01T00:00:00Z',
          updateTime: '2024-01-01T00:00:00Z'
        }
      },
      global: {
        components: {
          ElDialog,
          ElForm,
          ElButton,
          ElSelect,
          ElInput,
          ElSwitch
        }
      }
    })

    expect(wrapper.text()).not.toContain('快速选择')
  })

  it('should load Telegram channels when select dropdown is opened', async () => {
    vi.mocked(channelApi.getTgChannels).mockResolvedValue({
      records: mockTgChannels,
      total: 2,
      size: 100,
      current: 1,
      pages: 1
    })

    const wrapper = mount(ChannelDialog, {
      props: {
        visible: true,
        channel: null
      },
      global: {
        components: {
          ElDialog,
          ElForm,
          ElButton,
          ElSelect,
          ElInput,
          ElSwitch
        }
      }
    })

    const select = wrapper.findComponent(ElSelect)
    await select.vm.$emit('visible-change', true)

    expect(channelApi.getTgChannels).toHaveBeenCalledWith({
      current: 1,
      size: 100,
      forceRefresh: false
    })
  })

  it('should emit success event after successful creation', async () => {
    const wrapper = mount(ChannelDialog, {
      props: {
        visible: true,
        channel: null
      },
      global: {
        components: {
          ElDialog,
          ElForm,
          ElButton,
          ElSelect,
          ElInput,
          ElSwitch
        },
        stubs: {
          ElFormItem: false
        }
      }
    })

    expect(wrapper.emitted('success')).toBeFalsy()
  })

  it('should emit update:visible when cancel button is clicked', async () => {
    const wrapper = mount(ChannelDialog, {
      props: {
        visible: true,
        channel: null
      },
      global: {
        components: {
          ElDialog,
          ElForm,
          ElButton,
          ElSelect,
          ElInput,
          ElSwitch
        }
      }
    })

    const cancelButton = wrapper.findAll('.el-button')[0]
    await cancelButton.trigger('click')

    expect(wrapper.emitted('update:visible')).toBeTruthy()
    expect(wrapper.emitted('update:visible')?.[0]).toEqual([false])
  })
})
