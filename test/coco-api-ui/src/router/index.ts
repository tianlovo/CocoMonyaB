import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/authors'
    },
    {
      path: '/authors',
      name: 'authors',
      component: () => import('@/views/AuthorView.vue')
    },
    {
      path: '/works',
      name: 'works',
      component: () => import('@/views/WorkView.vue')
    },
    {
      path: '/characters',
      name: 'characters',
      component: () => import('@/views/CharacterView.vue')
    },
    {
      path: '/config',
      name: 'config',
      component: () => import('@/views/ConfigView.vue')
    }
  ]
})

export default router
