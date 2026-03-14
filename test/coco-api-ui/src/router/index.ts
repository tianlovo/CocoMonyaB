import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppLayout from '@/components/layout/AppLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true }
    },
    {
      path: '/',
      component: AppLayout,
      meta: { requiresAuth: true },
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
          path: 'server-config',
          name: 'server-config',
          component: () => import('@/views/ServerConfigView.vue')
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

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // 公开页面直接放行
  if (to.meta.public) {
    next()
    return
  }

  // 需要认证的页面
  if (to.meta.requiresAuth) {
    const isAuth = await authStore.checkAuth()
    if (isAuth) {
      next()
    } else {
      next('/login')
    }
    return
  }

  next()
})

export default router
