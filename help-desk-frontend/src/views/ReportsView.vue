<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useTicketStore } from '@/stores/ticketStore'
import api from '@/services/api'
import logoBrisa from '@/assets/brisa.png'
import jsPDF from 'jspdf'
import autoTable from 'jspdf-autotable'

const ticketStore = useTicketStore()

// --- ESTADOS DE CONTROLE ---
const reportType = ref('analistas')
const searchPerformed = ref(false)
const loading = ref(false)

// --- DADOS ---
const aggregatedResults = ref<any[]>([])
const teams = ref<{ id: number; nomeEquipe: string }[]>([])

// --- FILTROS ---
const currentYear = new Date().getFullYear()
const currentMonth = new Date().getMonth() + 1
const filtersAggr = ref({
  year: currentYear,
  month: currentMonth,
  equipeId: null as number | null,
})

// --- LÓGICA DE ANOS (CORRIGIDA) ---
const years = computed(() => {
  const list = []
  // Começa do ano atual do sistema (ex: 2025, 2026...)
  const end = new Date().getFullYear()
  // Data de corte solicitada: 2025 (2024 excluído)
  const start = 2025

  for (let i = end; i >= start; i--) {
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

// Permissão: Apenas Admin vê o filtro de equipes
const isAdmin = computed(() => {
  const role = (ticketStore.currentUser.role || '').toLowerCase()
  return ['admin', 'administrador'].includes(role)
})

// --- INICIALIZAÇÃO ---
onMounted(async () => {
  if (isAdmin.value) {
    try {
      const res = await api.get('/api/equipes')
      teams.value = res.data
    } catch (e) {
      console.error('Erro ao carregar equipes', e)
    }
  }
})

// --- AÇÃO DE PESQUISA ---
async function search() {
  searchPerformed.value = true
  loading.value = true
  aggregatedResults.value = []

  try {
    const params: any = {
      year: filtersAggr.value.year,
      equipeId: filtersAggr.value.equipeId,
    }

    if (reportType.value !== 'mensal') {
      params.month = filtersAggr.value.month
    }

    let endpoint = ''
    if (reportType.value === 'analistas') endpoint = '/api/reports/analistas'
    else if (reportType.value === 'categorias') endpoint = '/api/reports/categorias'
    else if (reportType.value === 'mensal') endpoint = '/api/reports/mensal'

    const res = await api.get(endpoint, { params })
    aggregatedResults.value = res.data
  } catch (error) {
    console.error('Erro na busca:', error)
    alert('Erro ao buscar dados do relatório.')
  } finally {
    loading.value = false
  }
}

function clearFields() {
  searchPerformed.value = false
  aggregatedResults.value = []
}

// --- TÍTULO DO RELATÓRIO ---
const currentReportTitle = computed(() => {
  if (reportType.value === 'analistas') return 'Quantidade de chamados atendidos por analista'
  if (reportType.value === 'categorias') return 'Tempo médio de atendimento por tipo de chamado'
  if (reportType.value === 'mensal') return 'Total de chamados / mês'
  return 'Relatório'
})

// --- 1. EXPORTAR PDF ---
function exportPdf() {
  if (aggregatedResults.value.length === 0) return alert('Não há dados para exportar.')

  try {
    const doc = new jsPDF()

    try {
      const imgWidth = 30
      const imgHeight = 15
      doc.addImage(logoBrisa, 'PNG', 14, 10, imgWidth, imgHeight)
    } catch (err) {
      console.warn('Logo ignorada no PDF')
    }

    doc.setFontSize(14)
    doc.text(currentReportTitle.value, 105, 20, { align: 'center' })
    doc.setFontSize(10)
    doc.text(`Emissão: ${new Date().toLocaleDateString()}`, 195, 20, { align: 'right' })
    doc.setLineWidth(0.5)
    doc.line(14, 28, 196, 28)

    let head = []
    let body = []

    if (reportType.value === 'analistas') {
      head = [['Analista', 'Chamados Atendidos']]
      body = aggregatedResults.value.map((r) => [r.nomeAnalista, r.totalChamados])
    } else if (reportType.value === 'categorias') {
      head = [['Categoria/Problema', 'Tempo Médio (Horas)']]
      body = aggregatedResults.value.map((r) => [r.categoria, Number(r.tempoMedioHoras).toFixed(2)])
    } else if (reportType.value === 'mensal') {
      head = [['Mês', 'Total de Chamados']]
      body = aggregatedResults.value.map((r) => {
        const mName = months.find((m) => m.v === r.mes)?.l || r.mes
        return [mName, r.totalChamados]
      })
    }

    autoTable(doc, {
      startY: 35,
      head: head,
      body: body,
      theme: 'grid',
      headStyles: { fillColor: [0, 51, 102], textColor: 255 },
      styles: { fontSize: 10, cellPadding: 3 },
    })

    doc.save(`Relatorio_${reportType.value}.pdf`)
  } catch (e) {
    console.error('Erro PDF:', e)
    alert('Erro ao gerar PDF.')
  }
}

// --- 2. EXPORTAR EXCEL (CORRIGIDO: COMPACTO E LIMPO) ---
function exportExcel() {
  if (aggregatedResults.value.length === 0) return alert('Não há dados para exportar.')

  // Estilo minimalista para Excel (Arial 10pt, Bordas Pretas Finas, Sem quebra de linha excessiva)
  const template = `
<html xmlns:o="urn:schemas-microsoft-com:office:office"
      xmlns:x="urn:schemas-microsoft-com:office:excel"
      xmlns="http://www.w3.org/TR/REC-html40">
<head>
<meta http-equiv="content-type" content="text/plain; charset=UTF-8"/>
<style>
  body, table { font-family: Arial, sans-serif; font-size: 10pt; }
  table { border-collapse: collapse; }
  th {
    background-color: #003366;
    color: white;
    border: 1px solid #000000;
    padding: 4px;
    text-align: left;
    white-space: nowrap;
  }
  td {
    border: 1px solid #000000;
    padding: 4px;
    text-align: left;
    vertical-align: center;
  }
</style>
</head>
<body>
<table>{table}</table>
</body>
</html>`

  const base64 = (s: string) => window.btoa(unescape(encodeURIComponent(s)))
  const format = (s: string, c: any) => s.replace(/{(\w+)}/g, (m, p) => c[p])

  let tableContent = '<thead><tr>'

  if (reportType.value === 'analistas') {
    tableContent += '<th>Analista</th><th>Qtd. Chamados</th>'
  } else if (reportType.value === 'categorias') {
    tableContent += '<th>Categoria</th><th>Tempo Médio (Horas)</th>'
  } else if (reportType.value === 'mensal') {
    tableContent += '<th>Mês</th><th>Total de Chamados</th>'
  }
  tableContent += '</tr></thead><tbody>'

  aggregatedResults.value.forEach((row) => {
    tableContent += '<tr>'
    if (reportType.value === 'analistas') {
      tableContent += `<td>${row.nomeAnalista}</td><td>${row.totalChamados}</td>`
    } else if (reportType.value === 'categorias') {
      tableContent += `<td>${row.categoria}</td><td>${Number(row.tempoMedioHoras).toFixed(2).replace('.', ',')}</td>`
    } else if (reportType.value === 'mensal') {
      const mName = months.find((m) => m.v === row.mes)?.l || row.mes
      tableContent += `<td>${mName}</td><td>${row.totalChamados}</td>`
    }
    tableContent += '</tr>'
  })
  tableContent += '</tbody>'

  const ctx = { table: tableContent }
  const link = document.createElement('a')
  link.href = 'data:application/vnd.ms-excel;base64,' + base64(format(template, ctx))
  link.download = `Relatorio_${reportType.value}.xls`
  link.click()
}

// --- 3. EXPORTAR CSV ---
function exportCsv() {
  if (aggregatedResults.value.length === 0) return alert('Não há dados para exportar.')

  let csvContent = ''

  if (reportType.value === 'analistas') csvContent += 'Analista;Qtd. Chamados\n'
  else if (reportType.value === 'categorias') csvContent += 'Categoria;Tempo Medio (Horas)\n'
  else if (reportType.value === 'mensal') csvContent += 'Mes;Total de Chamados\n'

  aggregatedResults.value.forEach((row) => {
    if (reportType.value === 'analistas') {
      csvContent += `${row.nomeAnalista};${row.totalChamados}\n`
    } else if (reportType.value === 'categorias') {
      csvContent += `${row.categoria};${Number(row.tempoMedioHoras).toFixed(2).replace('.', ',')}\n`
    } else if (reportType.value === 'mensal') {
      const mName = months.find((m) => m.v === row.mes)?.l || row.mes
      csvContent += `${mName};${row.totalChamados}\n`
    }
  })

  const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `Relatorio_${reportType.value}.csv`
  link.click()
}
</script>

<template>
  <div class="reports-view">
    <header class="view-header">
      <h1>Relatórios e Indicadores</h1>
    </header>

    <div class="report-builder">
      <div class="report-type-selector">
        <label>Tipo de Relatório:</label>
        <select v-model="reportType" @change="searchPerformed = false">
          <option value="analistas">Quantidade de chamados atendidos por analista</option>
          <option value="categorias">Tempo médio de atendimento por tipo de chamado</option>
          <option value="mensal">Total de chamados / mês</option>
        </select>
      </div>

      <hr class="separator" />

      <form @submit.prevent="search" class="filter-form">
        <div class="form-grid simple-grid">
          <div class="form-group" v-if="isAdmin">
            <label>Equipe:</label>
            <select v-model="filtersAggr.equipeId">
              <option :value="null">Todas as Equipes</option>
              <option v-for="t in teams" :key="t.id" :value="t.id">{{ t.nomeEquipe }}</option>
            </select>
          </div>

          <div class="form-group">
            <label>Ano:</label>
            <select v-model="filtersAggr.year">
              <option v-for="y in years" :key="y" :value="y">{{ y }}</option>
            </select>
          </div>

          <div class="form-group" v-if="reportType !== 'mensal'">
            <label>Mês:</label>
            <select v-model="filtersAggr.month">
              <option v-for="m in months" :key="m.v" :value="m.v">{{ m.l }}</option>
            </select>
          </div>
        </div>

        <div class="form-actions">
          <button type="submit" class="btn-primary" :disabled="loading">
            {{ loading ? 'Processando...' : 'Gerar Relatório' }}
          </button>
          <button type="button" @click="clearFields" class="btn-secondary">Limpar</button>
        </div>
      </form>

      <div v-if="searchPerformed" class="results-section">
        <hr />
        <div class="results-header">
          <h3>
            {{ currentReportTitle }}
            <span class="count-badge" v-if="aggregatedResults.length > 0"
              >{{ aggregatedResults.length }} registros</span
            >
          </h3>

          <div class="export-buttons" v-if="aggregatedResults.length > 0">
            <button @click="exportPdf" class="btn-export pdf">📄 Salvar PDF</button>
            <button @click="exportExcel" class="btn-export excel">📊 Salvar Excel (.xls)</button>
            <button @click="exportCsv" class="btn-export csv">📝 Salvar CSV</button>
          </div>
        </div>

        <div v-if="aggregatedResults.length > 0" class="simple-table-container">
          <table class="simple-table">
            <thead>
              <tr>
                <th v-if="reportType === 'analistas'">Analista</th>
                <th v-if="reportType === 'analistas'">Qtd. Chamados</th>

                <th v-if="reportType === 'categorias'">Categoria</th>
                <th v-if="reportType === 'categorias'">Tempo Médio (Horas)</th>

                <th v-if="reportType === 'mensal'">Mês</th>
                <th v-if="reportType === 'mensal'">Total de Chamados</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, i) in aggregatedResults" :key="i">
                <td v-if="reportType === 'analistas'">{{ row.nomeAnalista }}</td>
                <td v-if="reportType === 'analistas'">{{ row.totalChamados }}</td>

                <td v-if="reportType === 'categorias'">{{ row.categoria }}</td>
                <td v-if="reportType === 'categorias'">
                  {{ Number(row.tempoMedioHoras).toFixed(2).replace('.', ',') }} h
                </td>

                <td v-if="reportType === 'mensal'">
                  {{ months.find((m) => m.v === row.mes)?.l || row.mes }}
                </td>
                <td v-if="reportType === 'mensal'">{{ row.totalChamados }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="searchPerformed && aggregatedResults.length === 0" class="no-results">
          <p>Nenhum dado encontrado para os filtros selecionados.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.reports-view {
  padding: 2rem;
}
.view-header h1 {
  margin-top: 0;
  margin-bottom: 1.5rem;
  font-size: 1.8rem;
  color: #2c3e50;
}

.report-builder {
  background-color: #fff;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.report-type-selector {
  margin-bottom: 1.5rem;
  display: flex;
  flex-direction: column;
  max-width: 500px;
}
.report-type-selector label {
  font-weight: bold;
  margin-bottom: 0.5rem;
  color: #003366;
}
.report-type-selector select {
  padding: 0.8rem;
  border: 2px solid #003366;
  border-radius: 6px;
  font-size: 1rem;
  font-weight: 600;
  color: #333;
}

.separator {
  border: 0;
  border-top: 1px solid #eee;
  margin: 2rem 0;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1.5rem;
}
.simple-grid {
  max-width: 800px;
}

.form-group {
  display: flex;
  flex-direction: column;
}
.form-group label {
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #495057;
}
.form-group select,
.form-group input {
  padding: 0.75rem;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 1rem;
}

.form-actions {
  margin-top: 2rem;
  display: flex;
  gap: 1rem;
}
.btn-primary {
  background-color: #003366;
  color: white;
  border: none;
  padding: 0.8rem 2rem;
  border-radius: 4px;
  font-weight: bold;
  cursor: pointer;
  transition: background 0.2s;
}
.btn-primary:hover {
  background-color: #002244;
}
.btn-secondary {
  background-color: #f8f9fa;
  color: #333;
  border: 1px solid #ccc;
  padding: 0.8rem 2rem;
  border-radius: 4px;
  cursor: pointer;
}
.btn-secondary:hover {
  background-color: #e2e6ea;
}

.results-section {
  margin-top: 2rem;
}
.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 1rem;
}
.count-badge {
  background: #e9ecef;
  padding: 0.2rem 0.6rem;
  border-radius: 12px;
  font-size: 0.9rem;
  color: #666;
  margin-left: 0.5rem;
}

.export-buttons {
  display: flex;
  gap: 0.8rem;
}
.btn-export {
  padding: 0.6rem 1.2rem;
  border: none;
  border-radius: 4px;
  font-weight: 600;
  cursor: pointer;
  color: white;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.btn-export.pdf {
  background-color: #dc3545;
}
.btn-export.excel {
  background-color: #198754;
}
.btn-export.csv {
  background-color: #0d6efd;
}
.btn-export:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.simple-table-container {
  overflow-x: auto;
  border: 1px solid #e9ecef;
  border-radius: 8px;
}
.simple-table {
  width: 100%;
  border-collapse: collapse;
}
.simple-table th {
  background: #f8f9fa;
  padding: 1rem;
  text-align: left;
  border-bottom: 2px solid #dee2e6;
  color: #495057;
}
.simple-table td {
  padding: 1rem;
  border-bottom: 1px solid #e9ecef;
  color: #212529;
}
.simple-table tr:hover {
  background-color: #f8f9fa;
}

.no-results {
  text-align: center;
  padding: 3rem;
  color: #6c757d;
  border: 2px dashed #dee2e6;
  border-radius: 8px;
  margin-top: 1rem;
}
</style>
