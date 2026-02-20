<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useTicketStore } from '@/stores/ticketStore'
import api from '@/services/api'
import { useRouter } from 'vue-router'

const ticketStore = useTicketStore()
const router = useRouter()
const stats = ref({
  abertos: 0,
  emAndamento: 0,
  fechados: 0,
  total: 0,
  slaViolado: 0,
  chamadosPorAnalista: {} as Record<string, number>,
  // Adicionado para receber a lista
  chamadosSlaViolado: [] as any[],
})
const teams = ref<{ id: number; nomeEquipe: string }[]>([])
const selectedTeam = ref<number | null>(null)

const isAdmin = computed(() => {
  const role = (ticketStore.currentUser.role || '').toLowerCase()
  return role === 'admin' || role === 'administrador'
})

onMounted(async () => {
  if (isAdmin.value) await fetchTeams()
  await fetchStats()
})

async function fetchTeams() {
  try {
    const res = await api.get('/api/equipes')
    teams.value = res.data
  } catch (e) {
    console.error(e)
  }
}

async function fetchStats() {
  try {
    const params = selectedTeam.value ? { equipeId: selectedTeam.value } : {}
    const res = await api.get('/api/dashboard/stats', { params })
    stats.value = res.data
  } catch (e) {
    console.error(e)
  }
}

function openTicket(id: number) {
  router.push(`/chamado/${id}`)
}
</script>

<template>
  <div class="painel-container fade-in">
    <header class="panel-header">
      <div>
        <h1>Painel de Gestão em Tempo Real</h1>
      </div>

      <div v-if="isAdmin" class="team-filter">
        <label>Filtrar Equipe:</label>
        <select v-model="selectedTeam" @change="fetchStats">
          <option :value="null">Todas as Equipes</option>
          <option v-for="team in teams" :key="team.id" :value="team.id">
            {{ team.nomeEquipe }}
          </option>
        </select>
      </div>
    </header>

    <div class="kpi-grid">
      <div class="kpi-card danger-gradient">
        <div class="icon-box">⚠️</div>
        <div class="kpi-info">
          <h3>Aguardando Atendimento</h3>
          <span class="number">{{ stats.abertos }}</span>
          <span class="desc">Chamados na fila</span>
        </div>
      </div>

      <div class="kpi-card critical-gradient">
        <div class="icon-box">🔥</div>
        <div class="kpi-info">
          <h3>SLA Violado</h3>
          <span class="number">{{ stats.slaViolado }}</span>
          <span class="desc">Com atraso no atendimento</span>
        </div>
      </div>

      <div class="kpi-card info-gradient">
        <div class="icon-box">⚡</div>
        <div class="kpi-info">
          <h3>Em Atendimento</h3>
          <span class="number">{{ stats.emAndamento }}</span>
          <span class="desc">Sendo trabalhados agora</span>
        </div>
      </div>

      <div class="kpi-card total-gradient">
        <div class="icon-box">📊</div>
        <div class="kpi-info">
          <h3>Total Geral</h3>
          <span class="number">{{ stats.total }}</span>
          <span class="desc">Volume total de chamados</span>
        </div>
      </div>
    </div>

    <div class="panels-split">
      <div class="panel-section">
        <div class="section-header">
          <h2>Produtividade: Chamados em Aberto por Analista</h2>
        </div>
        <div v-if="Object.keys(stats.chamadosPorAnalista).length === 0" class="empty-state">
          Nenhum chamado atribuído em andamento.
        </div>
        <div v-else class="analysts-grid">
          <div v-for="(qtd, nome) in stats.chamadosPorAnalista" :key="nome" class="analyst-card">
            <div class="analyst-header">
              <span class="name">{{ nome }}</span>
              <span class="badge">{{ qtd }}</span>
            </div>
            <div class="progress-bg">
              <div
                class="progress-bar"
                :style="{ width: Math.min((qtd / 10) * 100, 100) + '%' }"
              ></div>
            </div>
          </div>
        </div>
      </div>

      <div class="panel-section">
        <div class="section-header critical-header">
          <h2>Chamados Acima do SLA</h2>
        </div>
        <div
          v-if="!stats.chamadosSlaViolado || stats.chamadosSlaViolado.length === 0"
          class="empty-state"
        >
          Nenhum chamado com SLA violado.
        </div>
        <div v-else class="sla-list">
          <table class="sla-table">
            <thead>
              <tr>
                <th>Chamado</th>
                <th>Prioridade</th>
                <th>Técnico</th>
                <th>Ação</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="ticket in stats.chamadosSlaViolado" :key="ticket.id">
                <td>{{ ticket.numeroChamado }}</td>

                <td>
                  <span class="tag-prio" :class="ticket.prioridade">{{ ticket.prioridade }}</span>
                </td>

                <td>{{ ticket.tecnicoAtribuido ? ticket.tecnicoAtribuido.nome : '---' }}</td>

                <td>
                  <button class="btn-view" @click="openTicket(ticket.id)">Ver</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.painel-container {
  padding: 2rem;
  background-color: #f8f9fa;
  min-height: 90vh;
}
.fade-in {
  animation: fadeIn 0.5s ease-out;
}
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Header */
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
}
.panel-header h1 {
  margin: 0;
  font-size: 1.6rem;
  color: #2c3e50;
}
.subtitle {
  margin: 0;
  color: #6c757d;
  font-size: 0.9rem;
}
.team-filter select {
  padding: 0.5rem;
  border-radius: 6px;
  border: 1px solid #ced4da;
}

/* Cards */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}
.kpi-card {
  padding: 1.5rem;
  border-radius: 12px;
  color: white;
  display: flex;
  align-items: center;
  gap: 1rem;
  box-shadow: 0 8px 15px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s;
}
.kpi-card:hover {
  transform: translateY(-5px);
}
.icon-box {
  font-size: 2rem;
  background: rgba(255, 255, 255, 0.2);
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}
.kpi-info h3 {
  margin: 0;
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  opacity: 0.9;
}
.number {
  display: block;
  font-size: 2.2rem;
  font-weight: 800;
  line-height: 1.1;
}
.desc {
  font-size: 0.75rem;
  opacity: 0.8;
}

.danger-gradient {
  background: linear-gradient(135deg, #f09819 0%, #edde5d 100%);
  color: #fff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}
.critical-gradient {
  background: linear-gradient(135deg, #ff416c 0%, #ff4b2b 100%);
}
.info-gradient {
  background: linear-gradient(135deg, #2193b0 0%, #6dd5ed 100%);
}
.total-gradient {
  background: linear-gradient(135deg, #373b44 0%, #4286f4 100%);
}

/* Split Panels */
.panels-split {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 1.5rem;
}
.panel-section {
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
}
.section-header {
  margin-bottom: 1rem;
  border-bottom: 2px solid #f1f2f6;
  padding-bottom: 0.5rem;
}
.section-header h2 {
  margin: 0;
  font-size: 1.1rem;
  color: #34495e;
}
.critical-header h2 {
  color: #e74c3c;
}

/* Analysts */
.analysts-grid {
  display: grid;
  gap: 1rem;
}
.analyst-card {
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 0.8rem;
}
.analyst-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #495057;
  font-size: 0.9rem;
}
.badge {
  background: #e9ecef;
  padding: 0.1rem 0.5rem;
  border-radius: 10px;
  font-size: 0.8rem;
}
.progress-bg {
  height: 6px;
  background: #f1f2f6;
  border-radius: 3px;
  overflow: hidden;
}
.progress-bar {
  height: 100%;
  background: #3498db;
  border-radius: 3px;
}

/* SLA Table */
.sla-list {
  overflow-x: auto;
}
.sla-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}
.sla-table th {
  text-align: left;
  padding: 0.5rem;
  color: #6c757d;
  font-weight: 600;
  border-bottom: 1px solid #eee;
}
.sla-table td {
  padding: 0.6rem 0.5rem;
  border-bottom: 1px solid #f8f9fa;
  color: #2c3e50;
}
.btn-view {
  background: none;
  border: 1px solid #3498db;
  color: #3498db;
  padding: 0.2rem 0.6rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.8rem;
}
.btn-view:hover {
  background: #3498db;
  color: white;
}
.tag-prio {
  font-size: 0.75rem;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
  color: white;
  background: #999;
}
.tag-prio.Crítica {
  background: #000;
}
.tag-prio.Elevada {
  background: #dc3545;
}
.tag-prio.Média {
  background: #ffc107;
  color: #333;
}
.tag-prio.Baixa {
  background: #198754;
}

.empty-state {
  color: #adb5bd;
  font-style: italic;
  padding: 1rem;
  text-align: center;
}
</style>
