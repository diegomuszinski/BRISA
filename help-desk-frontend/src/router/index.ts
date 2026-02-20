import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import ManagerDashboardView from '../views/ManagerDashboardView.vue'
import CreateTicketView from '../views/CreateTicketView.vue'
import UserTicketsView from '../views/UserTicketsView.vue'
import TicketDetailView from '../views/TicketDetailView.vue'

import ManageSystemView from '../views/ManageSystemView.vue'
// -------------------------------------------------------------------

import AdminUsersView from '../views/AdminUsersView.vue'
import AdminEquipesView from '../views/AdminEquipesView.vue'
import DashboardView from '../views/DashboardView.vue'
import PainelGestaoView from '../views/PainelGestaoView.vue'
import ReportsView from '../views/ReportsView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', name: 'login', component: LoginView },
    { path: '/', redirect: '/painel-gestao' },

    // Telas de Gestão
    {
      path: '/painel-gestao',
      name: 'painel-gestao',
      component: PainelGestaoView,
      meta: { requiresAuth: true },
    },
    {
      path: '/dashboards',
      name: 'dashboards',
      component: ManagerDashboardView,
      meta: { requiresAuth: true },
    },
    { path: '/relatorios', name: 'reports', component: ReportsView, meta: { requiresAuth: true } },
    {
      path: '/relatorios/satisfacao',
      component: ManagerDashboardView,
      meta: { requiresAuth: true },
    },
    {
      path: '/fila/:status',
      name: 'fila-dinamica',
      component: DashboardView,
      meta: { requiresAuth: true },
    },

    // Operacional
    {
      path: '/abrir-chamado',
      name: 'create-ticket',
      component: CreateTicketView,
      meta: { requiresAuth: true },
    },
    {
      path: '/meus-chamados',
      name: 'my-tickets',
      component: UserTicketsView,
      props: { status: 'abertos' },
      meta: { requiresAuth: true },
    },
    {
      path: '/meus-chamados/fechados',
      name: 'my-closed-tickets',
      component: UserTicketsView,
      props: { status: 'fechados' },
      meta: { requiresAuth: true },
    },
    {
      path: '/chamado/:id',
      name: 'ticket-detail',
      component: TicketDetailView,
      meta: { requiresAuth: true },
    },

    // --- ROTA CORRIGIDA ---
    {
      path: '/gerenciamento',
      name: 'management',
      component: ManageSystemView, // Agora aponta para o arquivo com Janela Bonita
      meta: { requiresAuth: true, roles: ['admin', 'manager', 'technician'] },
    },
    // ----------------------

    // Admin
    {
      path: '/admin/usuarios',
      name: 'users',
      component: AdminUsersView,
      meta: { requiresAuth: true, roles: ['admin'] },
    },
    {
      path: '/admin/equipes',
      name: 'equipes',
      component: AdminEquipesView,
      meta: { requiresAuth: true, roles: ['admin'] },
    },
  ],
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
