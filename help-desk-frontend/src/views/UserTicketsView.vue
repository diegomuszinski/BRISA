<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useTicketStore } from '@/stores/ticketStore'
import TicketList from '@/components/TicketList.vue'
import { RouterLink } from 'vue-router'

const props = defineProps<{
  status: 'abertos' | 'fechados'
}>()

const ticketStore = useTicketStore()

onMounted(() => {
  // Busca os dados atualizados do usuário
  ticketStore.fetchMyTickets()
})

const viewConfig = computed(() => {
  // SEPARADO: Se a rota pedir fechados, entrega fechados.
  if (props.status === 'fechados') {
    return {
      title: 'Histórico de Chamados (Fechados)',
      tickets: ticketStore.myClosedTickets,
    }
  }
  // SEPARADO: Se não, entrega abertos.
  return {
    title: 'Meus Chamados (Em Aberto)',
    tickets: ticketStore.myOpenTickets,
  }
})
</script>

<template>
  <div class="user-tickets-view">
    <header class="view-header">
      <h1>{{ viewConfig.title }}</h1>
    </header>

    <div class="view-content">
      <div v-if="viewConfig.tickets && viewConfig.tickets.length > 0">
        <TicketList :tickets="viewConfig.tickets" />
      </div>

      <div v-else class="no-tickets">
        <p>Nenhum chamado encontrado nesta lista.</p>

        <RouterLink v-if="props.status === 'abertos'" to="/abrir-chamado" class="btn-link">
          Clique aqui para abrir um novo chamado
        </RouterLink>
      </div>
    </div>
  </div>
</template>

<style scoped>
.user-tickets-view {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}
.view-header {
  margin-bottom: 2rem;
  border-bottom: 1px solid #eee;
  padding-bottom: 1rem;
}
.view-header h1 {
  margin: 0;
  font-size: 1.8rem;
  color: #2c3e50;
}

.view-content {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  min-height: 200px;
}
.no-tickets {
  padding: 4rem 2rem;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}
.btn-link {
  color: #003366;
  text-decoration: underline;
  font-weight: 600;
}
</style>
