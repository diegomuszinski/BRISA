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
  reportData: { [month: string]: number }
}>()

const chartData = computed(() => {
  const labels = Object.keys(props.reportData)
  const data = Object.values(props.reportData)

  return {
    labels: labels,
    datasets: [
      {
        label: 'Total de Chamados',
        backgroundColor: '#003366',
        borderColor: '#003366',
        data: data,
      },
    ],
  }
})

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
}
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
