<script setup lang="ts">
import { computed } from 'vue'
import { Bar } from 'vue-chartjs'
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  BarElement,
  CategoryScale,
  LinearScale,
} from 'chart.js'

ChartJS.register(Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale)

const props = defineProps<{
  reportData: { [analyst: string]: number }
}>()

const chartData = computed(() => ({
  labels: Object.keys(props.reportData),
  datasets: [
    {
      label: 'Chamados Atendidos',
      backgroundColor: '#28a745', // Verde
      data: Object.values(props.reportData),
    },
  ],
}))

const chartOptions = { responsive: true, maintainAspectRatio: false }
</script>
<template>
  <div class="chart-container">
    <Bar :data="chartData" :options="chartOptions" />
  </div>
</template>
<style scoped>
.chart-container {
  height: 400px;
  position: relative;
}
</style>
