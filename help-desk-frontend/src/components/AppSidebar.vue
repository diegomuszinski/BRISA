<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useTicketStore } from '@/stores/ticketStore'
import logoBrisa from '@/assets/brisa.png'
import {
  InboxArrowDownIcon,
  WrenchScrewdriverIcon,
  CheckCircleIcon,
  ChartPieIcon,
  DocumentTextIcon,
  PlusCircleIcon,
  ArrowLeftOnRectangleIcon,
  TvIcon,
  TicketIcon,
  FaceSmileIcon,
  CheckBadgeIcon,
  Cog6ToothIcon,
  UserGroupIcon,
  BuildingOfficeIcon,
} from '@heroicons/vue/24/outline'

const router = useRouter()
const ticketStore = useTicketStore()

// --- ESTADO DO MODAL DE SAÍDA ---
const showLogoutModal = ref(false)

const userRoleDisplay = computed(() => {
  const role = ticketStore.currentUser.role
  if (role === 'admin') return 'Administrador'
  if (role === 'user') return 'Usuário'
  if (role === 'technician') return 'Técnico'
  if (role === 'manager') return 'Gestor'
  return 'Não autenticado'
})

// Função chamada ao clicar no botão "Sair" (Abre o Modal)
function requestLogout() {
  showLogoutModal.value = true
}

// Confirma a saída
function confirmLogout() {
  showLogoutModal.value = false
  ticketStore.logout()
  router.push('/login')
}

// Cancela a saída
function cancelLogout() {
  showLogoutModal.value = false
}
</script>

<template>
  <aside class="sidebar">
    <div class="sidebar-header">
      <h2 class="sidebar-title">Help Desk</h2>
      <img :src="logoBrisa" alt="Logo Brisa" class="company-logo" />
      <div v-if="ticketStore.currentUser.role" class="user-profile">
        <strong>{{ userRoleDisplay }}</strong>
        <span>{{ ticketStore.currentUser.name }}</span>
      </div>
    </div>

    <nav class="main-nav">
      <template v-if="['user', 'admin', 'manager'].includes(ticketStore.currentUser.role || '')">
        <RouterLink to="/abrir-chamado" class="nav-link">
          <PlusCircleIcon class="nav-icon" />
          <span>Abrir Chamado</span>
        </RouterLink>
      </template>

      <template v-if="ticketStore.currentUser.role === 'user'">
        <RouterLink to="/meus-chamados" class="nav-link">
          <TicketIcon class="nav-icon" />
          <span>Meus Chamados</span>
        </RouterLink>
        <RouterLink to="/meus-chamados/fechados" class="nav-link">
          <CheckBadgeIcon class="nav-icon" />
          <span>Chamados Fechados</span>
        </RouterLink>
      </template>

      <template
        v-if="['admin', 'technician', 'manager'].includes(ticketStore.currentUser.role || '')"
      >
        <RouterLink to="/fila/abertos" class="nav-link">
          <InboxArrowDownIcon class="nav-icon" />
          <span>Fila de Entrada</span>
        </RouterLink>
        <RouterLink to="/fila/em-andamento" class="nav-link">
          <WrenchScrewdriverIcon class="nav-icon" />
          <span>Em Atendimento</span>
        </RouterLink>
        <RouterLink to="/fila/fechados" class="nav-link">
          <CheckCircleIcon class="nav-icon" />
          <span>Fechados</span>
        </RouterLink>
      </template>

      <template v-if="['admin', 'manager'].includes(ticketStore.currentUser.role || '')">
        <RouterLink to="/painel-gestao" class="nav-link">
          <TvIcon class="nav-icon" />
          <span>Painel de Gestão</span>
        </RouterLink>
        <RouterLink to="/dashboards" class="nav-link">
          <ChartPieIcon class="nav-icon" />
          <span>Dashboards</span>
        </RouterLink>
        <RouterLink to="/relatorios" class="nav-link">
          <DocumentTextIcon class="nav-icon" />
          <span>Relatórios</span>
        </RouterLink>
        <RouterLink to="/relatorios/satisfacao" class="nav-link">
          <FaceSmileIcon class="nav-icon" />
          <span>Pesq. de Satisfação</span>
        </RouterLink>
      </template>

      <template v-if="ticketStore.currentUser.role === 'admin'">
        <RouterLink to="/gerenciamento" class="nav-link">
          <Cog6ToothIcon class="nav-icon" />
          <span>Gerenciamento</span>
        </RouterLink>
        <RouterLink to="/admin/usuarios" class="nav-link">
          <UserGroupIcon class="nav-icon" />
          <span>Usuários</span>
        </RouterLink>
        <RouterLink to="/admin/equipes" class="nav-link">
          <BuildingOfficeIcon class="nav-icon" />
          <span>Equipes</span>
        </RouterLink>
      </template>
    </nav>

    <a href="#" @click.prevent="requestLogout" class="nav-link logout-link">
      <ArrowLeftOnRectangleIcon class="nav-icon" />
      <span>Sair</span>
    </a>

    <div v-if="showLogoutModal" class="modal-overlay">
      <div class="modal-content fade-in-modal">
        <div class="modal-icon-wrapper">
          <ArrowLeftOnRectangleIcon class="modal-icon" />
        </div>
        <h3>Deseja realmente sair?</h3>
        <p>Você será desconectado do sistema.</p>

        <div class="modal-actions">
          <button @click="cancelLogout" class="btn-cancel">Cancelar</button>
          <button @click="confirmLogout" class="btn-confirm">Sair</button>
        </div>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 250px;
  background-color: var(--brisa-blue-primary);
  color: #fff;
  display: flex;
  flex-direction: column;
  height: 100vh;
}
.sidebar-header {
  padding: 1.5rem;
  text-align: center;
  border-bottom: 1px solid var(--brisa-blue-secondary);
  flex-shrink: 0;
}
.sidebar-header h2 {
  margin: 0;
  font-size: 1.5rem;
}
.company-logo {
  width: 80px;
  margin: 1rem auto;
}
.user-profile {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
  margin-top: 1rem;
}
.user-profile strong {
  font-size: 1rem;
  font-weight: 500;
}
.user-profile span {
  font-size: 0.8rem;
  color: #ccc;
}
.main-nav {
  flex-grow: 1;
  overflow-y: auto;
  padding-top: 1rem;
}
.nav-link {
  color: #fff;
  text-decoration: none;
  padding: 1rem 1.5rem;
  transition: background-color 0.3s;
  border-left: 4px solid transparent;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.nav-icon {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
}
.nav-link:hover {
  background-color: var(--brisa-blue-secondary);
}
.nav-link.router-link-exact-active {
  background-color: var(--brisa-blue-secondary);
  font-weight: bold;
  border-left: 4px solid #3498db;
}
.logout-link {
  flex-shrink: 0;
  border-top: 1px solid var(--brisa-blue-secondary);
}

/* --- ESTILOS DO MODAL DE SAÍDA --- */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.6);
  /* Fundo escuro semitransparente */
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  backdrop-filter: blur(3px);
  /* Efeito de desfoque no fundo */
}

.modal-content {
  background: white;
  padding: 2rem;
  border-radius: 16px;
  text-align: center;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  width: 90%;
  max-width: 400px;
  color: #333;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.fade-in-modal {
  animation: popIn 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

@keyframes popIn {
  from {
    transform: scale(0.8);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

.modal-icon-wrapper {
  background-color: #fee2e2;
  /* Fundo vermelho claro */
  padding: 1rem;
  border-radius: 50%;
  margin-bottom: 1rem;
}

.modal-icon {
  width: 40px;
  height: 40px;
  color: #dc2626; /* Ícone vermelho */
}

.modal-content h3 {
  margin: 0 0 0.5rem 0;
  color: #1f2937;
  font-size: 1.5rem;
  font-weight: 700;
}

.modal-content p {
  color: #6b7280;
  margin-bottom: 2rem;
  font-size: 1rem;
}

.modal-actions {
  display: flex;
  gap: 1rem;
  width: 100%;
}

.btn-cancel,
.btn-confirm {
  flex: 1;
  padding: 0.8rem;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  font-size: 1rem;
  transition: all 0.2s;
}

.btn-cancel {
  background: #f3f4f6;
  color: #4b5563;
}
.btn-cancel:hover {
  background: #e5e7eb;
}

.btn-confirm {
  background: #dc2626;
  /* Vermelho */
  color: white;
  box-shadow: 0 4px 6px rgba(220, 38, 38, 0.2);
}
.btn-confirm:hover {
  background: #b91c1c;
  transform: translateY(-1px);
}
</style>
