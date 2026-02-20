<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import TicketList from '@/components/TicketList.vue'
import { useTicketStore } from '@/stores/ticketStore'
import { useRoute } from 'vue-router'
import api from '@/services/api'
import type { TicketDTO } from '@/types'

const ticketStore = useTicketStore()
const route = useRoute()

// Estado local
const tickets = ref<TicketDTO[]>([])
const loading = ref(false)

// Filtros
const searchField = ref('descricao') // Padrão corrigido para portugues
const searchTerm = ref('')
const analystFilter = ref<number | 'todos'>('todos')

const searchOptions = [
  { value: 'numero', text: 'Chamado' },
  { value: 'descricao', text: 'Descrição' },
  { value: 'prioridade', text: 'Prioridade' },
  { value: 'solicitante', text: 'Solicitante' },
]

// --- CORREÇÃO 1: Identificar perfil corretamente (role ou perfil) ---
const userRole = computed(() => {
  const u = ticketStore.currentUser as any
  return (u.role || u.perfil || '').toLowerCase()
})

const isGestorOrAdmin = computed(() => {
  const r = userRole.value
  return ['admin', 'administrador', 'manager', 'gestor'].includes(r)
})

const pageTitle = computed(() => {
  const status = route.params.status
  if (status === 'em-andamento') return 'Chamados em Atendimento'
  if (status === 'fechados') return 'Chamados Fechados'
  return 'Fila de Entrada (Aguardando Atendimento)'
})

async function loadTickets() {
  loading.value = true
  try {
    const statusRoute = route.params.status as string
    let statusParam = 'Aberto'

    if (statusRoute === 'em-andamento') statusParam = 'Em Andamento'
    else if (statusRoute === 'fechados') statusParam = 'Fechado'

    const params: any = {
      status: statusParam,
      search: searchTerm.value,
      searchType: searchField.value,
    }

    if (analystFilter.value !== 'todos') {
      params.tecnicoId = analystFilter.value
    }

    const response = await api.get('/api/tickets', { params })
    tickets.value = response.data
  } catch (error) {
    console.error('Erro ao buscar chamados:', error)
    tickets.value = []
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  // --- CORREÇÃO 2: Busca técnicos se for Admin/Gestor ---
  if (isGestorOrAdmin.value) {
    await ticketStore.fetchAnalysts()
  }
  loadTickets()
})

watch([() => route.params.status, analystFilter, searchField], () => {
  loadTickets()
})
</script>

<template>
  <div class="dashboard-view">
    <header class="view-header">
      <h1>{{ pageTitle }}</h1>

      <div class="filters-container">
        <template v-if="isGestorOrAdmin">
          <select v-model="analystFilter" class="filter-select">
            <option value="todos">Todos os Técnicos</option>
            <option v-for="analyst in ticketStore.analysts" :key="analyst.id" :value="analyst.id">
              {{ analyst.nome || analyst.name }}
            </option>
          </select>
        </template>

        <div class="search-bar">
          <select v-model="searchField" class="search-type-select">
            <option v-for="option in searchOptions" :key="option.value" :value="option.value">
              Buscar por {{ option.text }}
            </option>
          </select>

          <input
            type="search"
            v-model="searchTerm"
            @change="loadTickets"
            @keydown.enter="loadTickets"
            placeholder="Digite e pressione Enter..."
          />
          <button class="btn-search" @click="loadTickets">Buscar</button>
        </div>
      </div>
    </header>

    <div class="view-content">
      <div v-if="loading" class="loading-state">Carregando chamados...</div>
      <div v-else-if="tickets.length === 0" class="empty-state">Nenhum chamado encontrado.</div>
      <TicketList v-else :tickets="tickets" />
    </div>
  </div>
</template>

<style scoped>
.dashboard-view {
  padding: 2rem;
}
.view-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  flex-wrap: wrap;
  gap: 1rem;
}
.view-header h1 {
  margin: 0;
  font-size: 1.8rem;
  color: #2c3e50;
}
.filters-container {
  display: flex;
  gap: 1rem;
  align-items: center;
  flex-wrap: wrap;
}
.search-bar {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}
.search-bar input,
.search-bar select,
.filter-select {
  padding: 0.75rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 0.9rem;
  background-color: #fff;
  height: 45px;
}
.search-bar input {
  width: 250px;
}
.btn-search {
  height: 45px;
  padding: 0 1.5rem;
  background-color: #003366;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 600;
}
.btn-search:hover {
  background-color: #002244;
}
.view-content {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  min-height: 200px;
}
.loading-state,
.empty-state {
  padding: 3rem;
  text-align: center;
  color: #666;
  font-size: 1.1rem;
}
</style>
