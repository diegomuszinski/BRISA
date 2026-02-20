<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useTicketStore } from '@/stores/ticketStore'
import type { NewTicketPayload } from '@/types'
import { useToast } from 'vue-toastification'

const emit = defineEmits(['ticket-created'])
const ticketStore = useTicketStore()
const toast = useToast()

const description = ref('')
const category = ref('')
const selectedProblemId = ref<number | null>(null) // Novo campo para o ID do problema

const selectedFiles = ref<File[]>([])
const isLoading = ref(false)

const MAX_CHARS = 1000
const remainingChars = computed(() => MAX_CHARS - description.value.length)

const fileInputText = computed(() => {
  if (selectedFiles.value.length === 0) return 'Nenhum arquivo selecionado'
  if (selectedFiles.value.length === 1) return selectedFiles.value[0].name
  return `${selectedFiles.value.length} arquivos selecionados`
})

onMounted(() => {
  ticketStore.fetchFormData() // Busca Categorias e PROBLEMAS
})

function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  if (target.files) {
    selectedFiles.value.push(...Array.from(target.files))
  }
  target.value = ''
}

function removeFile(fileIndex: number) {
  selectedFiles.value.splice(fileIndex, 1)
}

async function handleSubmit() {
  if (!description.value.trim()) {
    toast.error('Por favor, descreva o problema.')
    return
  }

  // Validação: Problema é obrigatório (pois define a prioridade)
  if (!selectedProblemId.value) {
    toast.error('Por favor, selecione qual é o Problema.')
    return
  }

  isLoading.value = true

  const ticketData: NewTicketPayload = {
    description: description.value,
    category: category.value || 'Não Informada',
    problemaId: selectedProblemId.value, // Envia o ID do problema para o backend
    // priority: não enviamos mais, o backend define baseado no problema
  }

  const formData = new FormData()
  formData.append('ticket', new Blob([JSON.stringify(ticketData)], { type: 'application/json' }))

  if (selectedFiles.value.length > 0) {
    for (const file of selectedFiles.value) {
      formData.append('anexos', file)
    }
  }

  try {
    const newTicket = await ticketStore.createTicket(formData)
    toast.success(`Chamado ${newTicket.numeroChamado} criado com sucesso!`)
    emit('ticket-created', newTicket.id)
  } catch (error) {
    console.error('Detalhes do erro:', error)
    toast.error('Ocorreu um erro ao criar o chamado.')
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <form @submit.prevent="handleSubmit" class="ticket-form">
    <div class="form-group">
      <label for="problem">Qual é o Problema?</label>
      <select id="problem" v-model="selectedProblemId" required>
        <option :value="null">Selecione o problema...</option>
        <option v-for="prob in ticketStore.problems" :key="prob.id" :value="prob.id">
          {{ prob.nome }} (SLA: {{ prob.prioridadePadrao }})
        </option>
      </select>
    </div>

    <div class="form-group">
      <label for="category">Categoria (Opcional):</label>
      <select id="category" v-model="category">
        <option value="">Selecione uma categoria...</option>
        <option v-for="cat in ticketStore.categories" :key="cat.id" :value="cat.nome">
          {{ cat.nome }}
        </option>
      </select>
    </div>

    <div class="form-group">
      <label for="description">Descrição Detalhada:</label>
      <textarea
        id="description"
        v-model="description"
        required
        rows="5"
        :maxlength="MAX_CHARS"
      ></textarea>
      <small class="char-counter"> {{ remainingChars }} caracteres restantes </small>
    </div>

    <div class="form-group">
      <label>Anexar Arquivo(s):</label>
      <div class="file-input-container">
        <label for="anexo-input" class="file-input-button">Procurar...</label>
        <input
          type="file"
          id="anexo-input"
          class="file-input-hidden"
          @change="handleFileChange"
          accept=".doc,.docx,.xls,.xlsx,.csv,.txt"
          multiple
        />
        <span class="file-input-text">{{ fileInputText }}</span>
      </div>
      <div v-if="selectedFiles.length > 0" class="file-list">
        <ul>
          <li v-for="(file, index) in selectedFiles" :key="index">
            <span>{{ file.name }}</span>
            <button @click.prevent="removeFile(index)" class="remove-file-btn" title="Remover">
              &times;
            </button>
          </li>
        </ul>
      </div>
    </div>

    <div class="form-actions">
      <button type="submit" class="submit-btn" :disabled="isLoading">
        {{ isLoading ? 'Enviando...' : 'Abrir Chamado' }}
      </button>
    </div>
  </form>
</template>

<style scoped>
.ticket-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  max-width: 100%;
}
.form-group {
  display: flex;
  flex-direction: column;
  width: 100%;
}
.form-group label {
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #555;
}
.form-group input,
.form-group textarea,
.form-group select {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 1rem;
  font-family: inherit;
  box-sizing: border-box;
}
.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 1rem;
}
.submit-btn {
  padding: 0.8rem 1.5rem;
  background-color: var(--brisa-blue-primary);
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.3s;
}
.submit-btn:hover {
  background-color: var(--brisa-blue-secondary);
}
.submit-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}
.char-counter {
  text-align: right;
  margin-top: 0.25rem;
  font-size: 0.8rem;
  color: #6c757d;
}
.file-input-container {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  border: 1px solid #ccc;
  border-radius: 4px;
  padding: 0;
  overflow: hidden;
}
.file-input-hidden {
  display: none;
}
.file-input-button {
  background-color: #f0f0f0;
  padding: 0.75rem 1rem;
  cursor: pointer;
  border-right: 1px solid #ccc;
  white-space: nowrap;
  margin: 0;
  font-weight: normal;
  transition: background-color 0.2s;
}
.file-input-button:hover {
  background-color: #e0e0e0;
}
.file-input-text {
  padding: 0.75rem;
  color: #555;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-grow: 1;
}
.file-list {
  margin-top: 0.75rem;
  font-size: 0.9rem;
}
.file-list ul {
  list-style-type: none;
  padding-left: 0;
  margin: 0.5rem 0 0 0;
}
.file-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.4rem 0.6rem;
  background-color: #f8f9fa;
  border-radius: 4px;
  margin-bottom: 0.25rem;
  color: #333;
}
.remove-file-btn {
  background: none;
  border: none;
  color: #dc3545;
  font-size: 1.4rem;
  font-weight: bold;
  cursor: pointer;
  line-height: 1;
  padding: 0 0.2rem;
}
.remove-file-btn:hover {
  color: #a71d2a;
}
</style>
