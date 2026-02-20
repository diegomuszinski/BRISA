<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useTicketStore } from '@/stores/ticketStore'
import api from '@/services/api'
// IMPORTANTE: Trocamos os componentes quebrados pelo BarChart que funciona no Admin
import BarChart from '@/components/BarChart.vue'
import { ChartBarIcon, UserGroupIcon, ClockIcon } from '@heroicons/vue/24/outline'
import logoBrisa from '@/assets/brisa.png'
import type { ChartOptions } from 'chart.js'

const ticketStore = useTicketStore()

// --- CONFIGURAÇÃO DO GRÁFICO (IGUAL AO ADMIN) ---
const chartOptions = computed<ChartOptions<'bar'>>(() => {
  return {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: true },
      tooltip: {
        callbacks: {
          label: function (context) {
            let label = context.dataset.label || ''
            if (label) label += ': '
            if (context.parsed.y !== null) label += context.parsed.y
            return label
          },
        },
      },
    },
    scales: {
      y: { beginAtZero: true },
    },
  }
})

// --- CONTROLE DAS ABAS ---
const activeTab = ref('analistas')

// Filtros
const currentYear = new Date().getFullYear()
const currentMonth = new Date().getMonth() + 1
const startYear = 2025
const selectedYear = ref(currentYear < startYear ? startYear : currentYear)
const selectedMonth = ref(currentMonth)
const selectedTeam = ref<number | null>(null)

// --- DADOS DOS GRÁFICOS ---
const dataAnalistas = ref<any>(null)
const dataMensal = ref<any>(null)
const dataCategorias = ref<any>(null)

// Listas auxiliares
const teams = ref<{ id: number; nomeEquipe: string }[]>([])

// Lista de Anos
const years = computed(() => {
  const list = []
  const now = new Date().getFullYear()
  const end = now < startYear ? startYear : now
  for (let i = end; i >= startYear; i--) {
    list.push(i)
  }
  return list
})

const months = [
  { v: 1, l: 'Janeiro' },
  { v: 2, l: 'Fevereiro' },
  { v: 3, l: 'Março' },
  { v: 4, l: 'Abril' },
  { v: 5, l: 'Maio' },
  { v: 6, l: 'Junho' },
  { v: 7, l: 'Julho' },
  { v: 8, l: 'Agosto' },
  { v: 9, l: 'Setembro' },
  { v: 10, l: 'Outubro' },
  { v: 11, l: 'Novembro' },
  { v: 12, l: 'Dezembro' },
]

const isAdmin = computed(() => {
  const role = (ticketStore.currentUser.role || '').toLowerCase()
  return role === 'admin' || role === 'administrador'
})

onMounted(async () => {
  if (isAdmin.value) await fetchTeams()
  await fetchAllReports()
})

watch([selectedYear, selectedMonth, selectedTeam], () => {
  fetchAllReports()
})

async function fetchTeams() {
  try {
    const res = await api.get('/api/equipes')
    teams.value = res.data
  } catch (e) {
    console.error(e)
  }
}

async function fetchAllReports() {
  const params = {
    year: selectedYear.value,
    month: selectedMonth.value,
    equipeId: selectedTeam.value,
  }

  try {
    console.log('Buscando dados...', params)

    const [resAnalista, resMensal, resCat] = await Promise.all([
      api.get('/api/reports/analistas', { params }),
      api.get('/api/reports/mensal', {
        params: { year: selectedYear.value, equipeId: selectedTeam.value },
      }),
      api.get('/api/reports/categorias', { params }),
    ])

    // 1. Analistas
    const rawAnalistas = resAnalista.data || []
    console.log('Analistas Raw:', rawAnalistas)

    dataAnalistas.value = {
      labels: rawAnalistas.map((d: any) => d.nomeAnalista || d.nome || 'Desconhecido'),
      datasets: [
        {
          label: 'Chamados Atendidos',
          backgroundColor: '#42b983', // Verde Brisa
          data: rawAnalistas.map((d: any) => d.totalChamados || d.quantidade || 0),
        },
      ],
    }

    // 2. Categorias
    const rawCat = resCat.data || []
    dataCategorias.value = {
      labels: rawCat.map((d: any) => d.categoria || d.nome),
      datasets: [
        {
          label: 'Tempo Médio (Horas)',
          backgroundColor: '#f87979', // Vermelho
          data: rawCat.map((d: any) => d.tempoMedioHoras || d.media || 0),
        },
      ],
    }

    // 3. Mensal
    const rawMensal = resMensal.data || []
    const fullYearData = Array(12).fill(0)
    rawMensal.forEach((d: any) => {
      const mes = d.mes || d.mesIndex
      const qtd = d.totalChamados || d.quantidade
      if (mes >= 1 && mes <= 12) {
        fullYearData[mes - 1] = qtd
      }
    })

    const monthNames = months.map((m) => m.l)
    dataMensal.value = {
      labels: monthNames,
      datasets: [
        {
          label: 'Total de Chamados',
          backgroundColor: '#3498db', // Azul
          data: fullYearData,
        },
      ],
    }
  } catch (e) {
    console.error('Erro ao carregar gráficos', e)
  }
}

function exportPDF() {
  window.print()
}
</script>

<template>
  <div class="dashboard-analytics">
    <div class="print-header">
      <img :src="logoBrisa" alt="Logo Brisa" />
    </div>

    <header class="controls-header no-print">
      <div class="title-area">
        <h1>Dashboards Gerenciais</h1>
      </div>

      <div class="filters-area">
        <div v-if="isAdmin" class="filter-group">
          <label>Equipe</label>
          <select v-model="selectedTeam">
            <option :value="null">Todas as Equipes</option>
            <option v-for="t in teams" :key="t.id" :value="t.id">{{ t.nomeEquipe }}</option>
          </select>
        </div>

        <div class="filter-group">
          <label>Ano</label>
          <select v-model="selectedYear">
            <option v-for="y in years" :key="y" :value="y">{{ y }}</option>
          </select>
        </div>

        <div class="filter-group">
          <label>Mês</label>
          <select v-model="selectedMonth">
            <option v-for="m in months" :key="m.v" :value="m.v">{{ m.l }}</option>
          </select>
        </div>

        <button class="btn-export" @click="exportPDF">🖨️ Exportar PDF</button>
      </div>
    </header>

    <div class="tabs-container no-print">
      <button
        class="tab-btn"
        :class="{ active: activeTab === 'analistas' }"
        @click="activeTab = 'analistas'"
      >
        <UserGroupIcon class="tab-icon" /> Quantidade de chamados atendidos
      </button>
      <button
        class="tab-btn"
        :class="{ active: activeTab === 'categorias' }"
        @click="activeTab = 'categorias'"
      >
        <ClockIcon class="tab-icon" /> Tempo médio de atendimento
      </button>
      <button
        class="tab-btn"
        :class="{ active: activeTab === 'mensal' }"
        @click="activeTab = 'mensal'"
      >
        <ChartBarIcon class="tab-icon" /> Total de chamados / mês
      </button>
    </div>

    <div class="chart-display-area printable-area">
      <div v-if="activeTab === 'analistas'" class="chart-card fade-in">
        <h3>Quantidade de chamados atendidos por analista ({{ months[selectedMonth - 1].l }})</h3>
        <div class="chart-wrapper big-chart">
          <BarChart
            v-if="dataAnalistas && dataAnalistas.labels && dataAnalistas.labels.length > 0"
            :chart-data="dataAnalistas"
            :chart-options="chartOptions"
          />
          <p v-else class="no-data-msg">
            {{ dataAnalistas ? 'Nenhum dado encontrado para este filtro.' : 'Carregando...' }}
          </p>
        </div>
      </div>

      <div v-if="activeTab === 'categorias'" class="chart-card fade-in">
        <h3>Tempo médio de atendimento por tipo de chamado (Horas)</h3>
        <div class="chart-wrapper big-chart">
          <BarChart
            v-if="dataCategorias"
            :chart-data="dataCategorias"
            :chart-options="chartOptions"
          />
          <p v-else class="no-data-msg">Carregando ou sem dados...</p>
        </div>
      </div>

      <div v-if="activeTab === 'mensal'" class="chart-card fade-in">
        <h3>Total de chamados / mês ({{ selectedYear }})</h3>
        <div class="chart-wrapper big-chart">
          <BarChart v-if="dataMensal" :chart-data="dataMensal" :chart-options="chartOptions" />
          <p v-else class="no-data-msg">Carregando ou sem dados...</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard-analytics {
  padding: 2rem;
  background-color: #f4f6f9;
  min-height: 100vh;
}
.fade-in {
  animation: fadeIn 0.4s ease-in-out;
}
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
.print-header {
  display: none;
}
.controls-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  margin-bottom: 1.5rem;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);
  flex-wrap: wrap;
  gap: 1rem;
}
.title-area h1 {
  margin: 0;
  font-size: 1.6rem;
  color: #2c3e50;
}
.filters-area {
  display: flex;
  gap: 1rem;
  align-items: center;
  flex-wrap: wrap;
}
.filter-group {
  display: flex;
  flex-direction: column;
}
.filter-group label {
  font-size: 0.75rem;
  font-weight: bold;
  color: #555;
  margin-bottom: 0.2rem;
}
.filter-group select {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  min-width: 120px;
}
.btn-export {
  background-color: #34495e;
  color: white;
  border: none;
  padding: 0.6rem 1.2rem;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  margin-top: 1.2rem;
}
.btn-export:hover {
  background-color: #2c3e50;
}
.tabs-container {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.5rem;
  border-bottom: 2px solid #e0e0e0;
  padding-bottom: 0.5rem;
}
.tab-btn {
  background: none;
  border: none;
  font-size: 1rem;
  font-weight: 600;
  color: #7f8c8d;
  padding: 0.8rem 1.5rem;
  cursor: pointer;
  border-bottom: 3px solid transparent;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.tab-btn:hover {
  color: #003366;
  background-color: rgba(0, 51, 102, 0.05);
  border-radius: 8px 8px 0 0;
}
.tab-btn.active {
  color: #003366;
  border-bottom: 3px solid #003366;
}
.tab-icon {
  width: 20px;
  height: 20px;
}
.chart-card {
  background: white;
  padding: 2rem;
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
  min-height: 500px;
}
.chart-card h3 {
  margin-top: 0;
  color: #2c3e50;
  font-size: 1.3rem;
  margin-bottom: 1.5rem;
  border-bottom: 1px solid #eee;
  padding-bottom: 1rem;
}
.chart-wrapper.big-chart {
  height: 450px;
  position: relative;
}
.no-data-msg {
  text-align: center;
  margin-top: 50px;
  color: #999;
  font-size: 1.1rem;
}

@media print {
  .no-print,
  :global(.sidebar),
  :global(nav),
  :global(header) {
    display: none !important;
  }
  :global(body),
  :global(#app) {
    background: white !important;
    margin: 0 !important;
    padding: 0 !important;
    width: 100vw;
    height: 100vh;
    overflow: hidden;
  }
  .dashboard-analytics {
    background: white !important;
    padding: 0 !important;
    width: 100%;
    height: 100%;
    position: relative;
  }
  .print-header {
    display: block !important;
    position: absolute;
    top: 5%;
    left: 50%;
    transform: translateX(-50%);
    text-align: center;
  }
  .print-header img {
    width: 180px;
  }
  .printable-area {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 90%;
    max-width: 1000px;
  }
  .chart-card {
    box-shadow: none !important;
    border: none !important;
    width: 100%;
  }
  .chart-card h3 {
    text-align: center;
    font-size: 1.8rem;
    margin-bottom: 1cm;
    border-bottom: 2px solid #333;
  }
  .chart-wrapper.big-chart {
    height: 500px !important;
  }
}
</style>
