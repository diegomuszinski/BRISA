<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { Ticket, TicketDTO } from '@/types'
import { formatDateTime } from '@/utils/formatters'
import { calculateSlaDeadline } from '@/stores/ticketStore' // Importa cálculo
import { ExclamationTriangleIcon } from '@heroicons/vue/24/outline'

const props = defineProps<{
  tickets: (Ticket | TicketDTO)[]
}>()

const router = useRouter()

function goToTicket(id: number) {
  router.push(`/chamado/${id}`)
}

function getPriorityClass(priority?: string | null): string {
  if (!priority) return 'priority-default'
  const key = priority
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
  return `priority-${key}`
}

function isTicketReopened(ticket: any): boolean {
  return !!ticket.isReopened || !!ticket.foiReaberto
}

// Obter nome do Técnico
function getTecnicoName(ticket: any) {
  if (typeof ticket.tecnicoAtribuido === 'object' && ticket.tecnicoAtribuido)
    return ticket.tecnicoAtribuido.nome
  return ticket.assignedTo || ticket.tecnicoAtribuido || '-'
}

// Obter SLA Formatado (Data + Tempo Restante)
function getSlaDisplay(ticket: any) {
  if (['Resolvido', 'Fechado', 'Encerrado', 'Cancelado'].includes(ticket.status)) return '-'

  const start = ticket.openedAt || ticket.dataAbertura
  if (!start) return '-'

  const priority = ticket.priority || ticket.prioridade || 'Média'
  const deadline = calculateSlaDeadline(start, priority)
  const now = new Date()
  const diff = deadline.getTime() - now.getTime()

  const dateStr = formatDateTime(deadline.toISOString())

  if (diff <= 0) return `${dateStr} (ATRASADO)`

  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))

  return `${dateStr} (${days}d ${hours}h ${minutes}m)`
}
</script>

<template>
  <div class="ticket-list">
    <table class="ticket-table">
      <thead>
        <tr>
          <th>Chamado</th>
          <th>Descrição</th>
          <th>Prioridade</th>
          <th>Solicitante</th>
          <th>Técnico</th>
          <th>Prazo SLA</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="ticket in tickets as any[]" :key="ticket.id" @click="goToTicket(ticket.id)">
          <td class="ticket-number">
            <div class="ticket-id-cell">
              <span v-if="isTicketReopened(ticket)" class="reopened-icon-wrapper" title="Reaberto">
                <ExclamationTriangleIcon class="reopened-icon" />
              </span>
              <span>#{{ ticket.numeroChamado || ticket.id }}</span>
            </div>
          </td>
          <td class="desc-cell">{{ ticket.description || ticket.descricao || 'Sem descrição' }}</td>
          <td>
            <span
              :class="['priority-badge', getPriorityClass(ticket.priority || ticket.prioridade)]"
            >
              {{ ticket.priority || ticket.prioridade || 'N/D' }}
            </span>
          </td>
          <td>{{ ticket.solicitante?.nome || ticket.user || ticket.nomeSolicitante || 'N/D' }}</td>
          <td>{{ getTecnicoName(ticket) }}</td>
          <td class="sla-date" :class="{ overdue: getSlaDisplay(ticket).includes('ATRASADO') }">
            {{ getSlaDisplay(ticket) }}
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.ticket-list {
  overflow-x: auto;
}
.ticket-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.95rem;
}
.ticket-table th,
.ticket-table td {
  padding: 1rem;
  text-align: left;
  border-bottom: 1px solid #e9ecef;
}
.ticket-table th {
  color: #6c757d;
  font-weight: 600;
  text-transform: uppercase;
  font-size: 0.8rem;
}
.ticket-table tbody tr {
  cursor: pointer;
  transition: background 0.2s;
}
.ticket-table tbody tr:hover {
  background-color: #f8f9fa;
}
.ticket-id-cell {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.ticket-number {
  font-weight: 700;
  color: #003366;
  white-space: nowrap;
}
.desc-cell {
  max-width: 300px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.reopened-icon {
  width: 1.1rem;
  color: #f59e0b;
}
.priority-badge {
  padding: 0.3rem 0.8rem;
  border-radius: 12px;
  font-weight: 600;
  color: #fff;
  font-size: 0.75rem;
  text-transform: uppercase;
}
.priority-baixa {
  background-color: #198754;
}
.priority-media {
  background-color: #ffc107;
  color: #333;
}
.priority-alta,
.priority-elevada {
  background-color: #dc3545;
}
.priority-critica {
  background-color: #000;
}
.priority-default {
  background-color: #6c757d;
}
.sla-date {
  font-weight: 500;
  font-size: 0.9rem;
}
.overdue {
  color: #dc3545;
  font-weight: bold;
}
</style>
