import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/projects' },
    {
      path: '/login',
      component: () => import('../views/LoginView.vue'),
      meta: { guest: true },
    },
    {
      path: '/register',
      component: () => import('../views/RegisterView.vue'),
      meta: { guest: true },
    },
    {
      path: '/verify-email',
      component: () => import('../views/VerifyEmailView.vue'),
      meta: { guest: true },
    },
    {
      path: '/projects',
      component: () => import('../views/ProjectsView.vue'),
      meta: { auth: true },
    },
    {
      path: '/projects/:id',
      component: () => import('../views/BoardView.vue'),
      meta: { auth: true },
    },
    {
      path: '/projects/:id/settings',
      component: () => import('../views/ProjectSettingsView.vue'),
      meta: { auth: true },
    },
    {
      path: '/chat',
      component: () => import('../views/ChatView.vue'),
      meta: { auth: true },
    },
    {
      path: '/chat/:chatId',
      component: () => import('../views/ChatView.vue'),
      meta: { auth: true },
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.auth && !auth.user) return '/login'
  if (to.meta.guest && auth.user) return '/projects'
})

export default router
