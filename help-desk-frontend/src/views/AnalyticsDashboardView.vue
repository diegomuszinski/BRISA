<script setup lang="ts">
import { ref, watch, onMounted, computed } from 'vue'
import BarChart from '@/components/BarChart.vue'
import api from '@/services/api'
import { useTicketStore } from '@/stores/ticketStore'
import type { RelatorioAnalista, RelatorioCategoria, RelatorioMensal } from '@/types'
import type { ChartOptions } from 'chart.js'
import jsPDF from 'jspdf'
import html2canvas from 'html2canvas'
import logoBrisa from '@/assets/brisa.png'

type Tab = 'analyst' | 'category' | 'month'

const ticketStore = useTicketStore()
const activeTab = ref<Tab>('analyst')
const selectedYear = ref<number | 'all'>('all')
const selectedMonth = ref<number | 'all'>('all')

// Filtro de Equipe
const selectedTeam = ref<number>(-1)
const teams = ref<{ id: number; nomeEquipe: string }[]>([])

const reportData = ref<any>(null)
const isLoading = ref(false)
const isExporting = ref(false)
const chartAreaRef = ref<HTMLElement | null>(null)

// Ano atual dinâmico
const currentYear = new Date().getFullYear()
const years = [currentYear, currentYear - 1, currentYear - 2]

const months = [
  { value: 1, name: 'Janeiro' },
  { value: 2, name: 'Fevereiro' },
  { value: 3, name: 'Março' },
  { value: 4, name: 'Abril' },
  { value: 5, name: 'Maio' },
  { value: 6, name: 'Junho' },
  { value: 7, name: 'Julho' },
  { value: 8, name: 'Agosto' },
  { value: 9, name: 'Setembro' },
  { value: 10, name: 'Outubro' },
  { value: 11, name: 'Novembro' },
  { value: 12, name: 'Dezembro' },
]

// Verifica se é Admin
const isAdmin = computed(() => {
  const role = ticketStore.currentUser.role || ''
  return role.toLowerCase() === 'admin'
})

const reportTitle = computed(() => {
  switch (activeTab.value) {
    case 'analyst':
      return 'Chamados Atendidos por Analista'
    case 'category':
      return 'Tempo Médio de Resolução por Categoria'
    case 'month':
      return 'Total de Chamados por Mês'
    default:
      return 'Relatório Gerencial'
  }
})

const chartOptions = computed<ChartOptions<'bar'>>(() => {
  const options: ChartOptions<'bar'> = {
    plugins: {
      tooltip: {
        callbacks: {
          label: function (context) {
            let label = context.dataset.label || ''
            if (label) label += ': '
            if (context.parsed.y !== null) label += context.parsed.y.toFixed(2)
            return label
          },
        },
      },
    },
    scales: {
      y: { beginAtZero: true },
    },
  }
  return options
})

// Busca Equipes (Apenas para Admin)
const fetchTeams = async () => {
  if (!isAdmin.value) return
  try {
    const res = await api.get('/api/equipes')
    teams.value = res.data
  } catch (error) {
    console.error('Erro ao buscar equipes:', error)
  }
}

const fetchData = async () => {
  isLoading.value = true
  reportData.value = null

  const params = new URLSearchParams()
  if (selectedYear.value !== 'all') params.append('year', selectedYear.value.toString())
  if (selectedMonth.value !== 'all' && activeTab.value !== 'month')
    params.append('month', selectedMonth.value.toString())

  // Envia a equipe selecionada (ou -1)
  params.append('equipeId', selectedTeam.value.toString())

  // CORREÇÃO DAS ROTAS PARA BATER COM O BACKEND
  let url = ''
  switch (activeTab.value) {
    case 'analyst':
      url = '/api/reports/analistas'
      break
    case 'category':
      url = '/api/reports/categorias'
      break
    case 'month':
      url = '/api/reports/mensal'
      break
  }

  try {
    const response = await api.get(`${url}?${params.toString()}`)
    const monthNames = months.map((m) => m.name)

    if (activeTab.value === 'analyst') {
      const data = response.data as any[]
      reportData.value = {
        labels: data.map((d) => d.nomeAnalista || d.nome),
        datasets: [
          {
            label: 'Chamados Atendidos',
            backgroundColor: '#42b983',
            data: data.map((d) => d.totalChamados || d.quantidade),
          },
        ],
      }
    } else if (activeTab.value === 'category') {
      const data = response.data as any[]
      reportData.value = {
        labels: data.map((d) => d.categoria),
        datasets: [
          {
            label: 'Tempo Médio (em horas)',
            backgroundColor: '#f87979',
            data: data.map((d) => d.tempoMedioHoras || d.media),
          },
        ],
      }
    } else if (activeTab.value === 'month') {
      const data = response.data as any[]
      const fullYearData = Array(12).fill(0)
      data.forEach((d) => {
        const mes = d.mes || d.mesIndex
        const qtd = d.totalChamados || d.quantidade
        if (mes >= 1 && mes <= 12) {
          fullYearData[mes - 1] = qtd
        }
      })
      reportData.value = {
        labels: monthNames,
        datasets: [{ label: 'Total de Chamados', backgroundColor: '#3498db', data: fullYearData }],
      }
    }
  } catch (error) {
    console.error('Falha ao buscar dados:', error)
  } finally {
    isLoading.value = false
  }
}

async function exportToPDF() {
  const chartElement = chartAreaRef.value
  if (!chartElement) return
  isExporting.value = true
  try {
    const canvas = await html2canvas(chartElement, { scale: 2 })
    const imgData = canvas.toDataURL('image/png')
    const doc = new jsPDF('p', 'mm', 'a4')
    const docWidth = doc.internal.pageSize.getWidth()
    const margin = 15
    doc.addImage(logoBrisa, 'PNG', margin, 10, 40, 15)
    doc.text('Relatório Gerencial', docWidth / 2, 20, { align: 'center' })
    doc.addImage(
      imgData,
      'PNG',
      margin,
      50,
      docWidth - margin * 2,
      (canvas.height * (docWidth - margin * 2)) / canvas.width,
    )
    doc.save(`relatorio_${activeTab.value}.pdf`)
  } catch (error) {
    console.error('Erro PDF:', error)
  } finally {
    isExporting.value = false
  }
}

onMounted(() => {
  fetchTeams()
  fetchData()
})
watch([activeTab, selectedYear, selectedMonth, selectedTeam], fetchData)
</script>

<template>
  <div class="reports-view">
    <header class="view-header">
      <h1>Relatórios Gerenciais</h1>
      <div class="filters">
        <div v-if="isAdmin" class="filter-group">
          <label>Equipe:</label>
          <select v-model="selectedTeam">
            <option :value="-1">Todas as Equipes</option>
            <option v-for="team in teams" :key="team.id" :value="team.id">
              {{ team.nomeEquipe }}
            </option>
          </select>
        </div>

        <div class="filter-group">
          <label>Ano:</label>
          <select v-model="selectedYear">
            <option value="all">Todo o Período</option>
            <option v-for="year in years" :key="year" :value="year">{{ year }}</option>
          </select>
        </div>

        <div class="filter-group">
          <label>Mês:</label>
          <select v-model="selectedMonth" :disabled="activeTab === 'month'">
            <option value="all">Todo o Período</option>
            <option v-for="month in months" :key="month.value" :value="month.value">
              {{ month.name }}
            </option>
          </select>
        </div>
      </div>
    </header>

    <div class="tabs">
      <button :class="{ active: activeTab === 'analyst' }" @click="activeTab = 'analyst'">
        Por Analista
      </button>
      <button :class="{ active: activeTab === 'category' }" @click="activeTab = 'category'">
        Por Categoria
      </button>
      <button :class="{ active: activeTab === 'month' }" @click="activeTab = 'month'">
        Por Mês
      </button>
    </div>

    <div class="chart-area" ref="chartAreaRef">
      <div v-if="isLoading" class="loading-state">Carregando dados...</div>
      <div v-else-if="reportData && reportData.datasets[0].data.some((d: any) => d > 0)">
        <BarChart :chart-data="reportData" :chart-options="chartOptions" />
      </div>
      <div v-else class="no-data">Nenhum dado encontrado.</div>
    </div>

    <div class="export-actions">
      <button @click="exportToPDF" :disabled="isExporting" class="export-btn">
        {{ isExporting ? 'Gerando...' : 'Imprimir PDF' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.reports-view {
  padding: 2rem;
}
.view-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  flex-wrap: wrap;
}
.view-header h1 {
  margin: 0;
  font-size: 1.8rem;
  color: #2c3e50;
}
.filters {
  display: flex;
  gap: 1rem;
  align-items: flex-end;
}
.filter-group {
  display: flex;
  flex-direction: column;
}
.filter-group label {
  font-size: 0.85rem;
  margin-bottom: 0.2rem;
  color: #6c757d;
}
.filters select {
  padding: 0.5rem;
  border-radius: 4px;
  border: 1px solid #ccc;
  min-width: 150px;
}
.tabs {
  margin-bottom: 1.5rem;
  border-bottom: 1px solid #dee2e6;
}
.tabs button {
  padding: 0.8rem 1.5rem;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 1rem;
  color: #6c757d;
  border-bottom: 3px solid transparent;
}
.tabs button.active {
  color: var(--brisa-blue-primary);
  border-bottom-color: var(--brisa-blue-primary);
  font-weight: 600;
}
.chart-area {
  background: #fff;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  min-height: 400px;
}
.loading-state,
.no-data {
  text-align: center;
  padding: 4rem;
  color: #6c757d;
  font-size: 1.2rem;
}
.export-actions {
  margin-top: 1.5rem;
}
.export-btn {
  padding: 0.8rem 1.5rem;
  background-color: var(--brisa-blue-primary);
  color: white;
  border: none;
  border-radius: 4px;
  font-weight: 600;
  cursor: pointer;
}
.export-btn:hover {
  background-color: var(--brisa-blue-secondary);
}
</style>
