import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '@/components/layout/AppLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: AppLayout,
      children: [
        {
          path: '',
          redirect: '/config/authors'
        },
        {
          path: 'message-tracking',
          name: 'message-tracking',
          component: () => import('@/views/MessageTrackingView.vue')
        },
        {
          path: 'system-info',
          name: 'system-info',
          component: () => import('@/views/SystemInfoView.vue')
        },
        {
          path: 'channel',
          component: () => import('@/views/ChannelLayout.vue'),
          children: [
            {
              path: '',
              redirect: '/channel/management'
            },
            {
              path: 'management',
              name: 'channel-management',
              component: () => import('@/views/ChannelManagementView.vue')
            },
            {
              path: 'telegram',
              name: 'channel-telegram',
              component: () => import('@/views/TelegramChannelView.vue')
            }
          ]
        },
        {
          path: 'config',
          component: () => import('@/views/ConfigLayout.vue'),
          children: [
            {
              path: '',
              redirect: '/config/authors'
            },
            {
              path: 'authors',
              name: 'config-authors',
              component: () => import('@/views/AuthorView.vue')
            },
            {
              path: 'works',
              name: 'config-works',
              component: () => import('@/views/WorkView.vue')
            },
            {
              path: 'characters',
              name: 'config-characters',
              component: () => import('@/views/CharacterView.vue')
            },
            {
              path: 'filter',
              name: 'config-filter',
              component: () => import('@/views/ConfigView.vue')
            }
          ]
        }
      ]
    }
  ]
})

export default router
