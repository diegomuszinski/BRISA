<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useTicketStore } from '@/stores/ticketStore'
import { useToast } from 'vue-toastification'

const ticketStore = useTicketStore()
const router = useRouter()
const toast = useToast()

const selectedProblemId = ref<number | null>(null)
const selectedCategoryId = ref<number | null>(null)
const description = ref('')
// Alterado para array de File para permitir acumular
const files = ref<File[]>([])

const isSubmitting = ref(false)

const selectedProblem = computed(() => {
  return ticketStore.problems.find((p) => p.id === selectedProblemId.value)
})

onMounted(async () => {
  await ticketStore.fetchFormData()
})

function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    // Converte FileList para Array e ADICIONA aos existentes (não substitui)
    const newFiles = Array.from(target.files)
    files.value = [...files.value, ...newFiles]

    // Limpa o input para permitir selecionar o mesmo arquivo novamente se quiser
    target.value = ''
  }
}

function removeFile(index: number) {
  files.value.splice(index, 1)
}

async function handleSubmit() {
  if (!selectedProblemId.value) {
    toast.warning('Selecione qual é o problema.')
    return
  }
  if (!description.value.trim()) {
    toast.warning('A descrição detalhada é obrigatória.')
    return
  }

  isSubmitting.value = true

  try {
    const formData = {
      problemaId: selectedProblemId.value,
      category: selectedCategoryId.value,
      description: description.value,
      priority: selectedProblem.value?.prioridadePadrao || 'Média',
      files: files.value, // Envia o array acumulado
    }

    await ticketStore.createTicket(formData)

    toast.success('Chamado aberto com sucesso!')
    router.push('/meus-chamados')
  } catch (error) {
    console.error(error)
    toast.error('Erro ao abrir chamado.')
  } finally {
    isSubmitting.value = false
  }
}

function handleCancel() {
  router.back()
}
</script>

<template>
  <div class="create-ticket-container">
    <div class="header">
      <h1>Abertura de Novo Chamado</h1>
      <p>Preencha os campos abaixo para registrar sua solicitação.</p>
    </div>

    <div class="form-card">
      <form @submit.prevent="handleSubmit">
        <div class="form-group">
          <label>Qual é o Problema? <span class="required">*</span></label>
          <select v-model="selectedProblemId" required>
            <option :value="null" disabled>Selecione um problema...</option>
            <option v-for="p in ticketStore.problems" :key="p.id" :value="p.id">
              {{ p.nome }}
            </option>
          </select>
        </div>

        <div class="form-group">
          <label>Categoria (Opcional):</label>
          <select v-model="selectedCategoryId">
            <option :value="null">Selecione uma categoria</option>
            <option v-for="c in ticketStore.categories" :key="c.id" :value="c.id">
              {{ c.nome }}
            </option>
          </select>
        </div>

        <div class="form-group">
          <label>Descrição Detalhada: <span class="required">*</span></label>
          <textarea
            v-model="description"
            rows="5"
            placeholder="Descreva o que está acontecendo, mensagens de erro, etc..."
            required
          ></textarea>
        </div>

        <div class="form-group">
          <label>Anexos (Prints, Logs):</label>
          <div class="file-input-wrapper">
            <input type="file" multiple @change="handleFileChange" id="fileInput" />
            <label for="fileInput" class="file-label">
              <span>📎 Adicionar Arquivos</span>
            </label>
            <small>Formatos aceitos: Imagens, PDF, Texto. Você pode adicionar vários.</small>
          </div>

          <div v-if="files.length > 0" class="file-list">
            <p>Arquivos anexados ({{ files.length }}):</p>
            <ul>
              <li v-for="(file, index) in files" :key="index">
                <span>{{ file.name }}</span>
                <button type="button" @click="removeFile(index)" class="btn-remove" title="Remover">
                  ×
                </button>
              </li>
            </ul>
          </div>
        </div>

        <div class="form-actions">
          <button type="button" class="btn-cancel" @click="handleCancel" :disabled="isSubmitting">
            Cancelar
          </button>
          <button type="submit" class="btn-submit" :disabled="isSubmitting">
            {{ isSubmitting ? 'Enviando...' : 'Abrir Chamado' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.create-ticket-container {
  padding: 2rem;
  max-width: 800px;
  margin: 0 auto;
}
.header {
  text-align: center;
  margin-bottom: 2rem;
}
.header h1 {
  color: #2c3e50;
  margin-bottom: 0.5rem;
}
.header p {
  color: #7f8c8d;
}
.form-card {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
}
.form-group {
  margin-bottom: 1.5rem;
}
.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #34495e;
}
.required {
  color: #dc3545;
}
select,
textarea {
  width: 100%;
  padding: 0.8rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
  font-family: inherit;
}
select:focus,
textarea:focus {
  outline: 2px solid #003366;
  border-color: transparent;
}

/* Estilo do Input de Arquivo */
input[type='file'] {
  display: none;
} /* Esconde o input padrão feio */
.file-label {
  display: inline-block;
  padding: 0.6rem 1.2rem;
  background-color: #f0f4f8;
  color: #003366;
  border: 1px dashed #003366;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
}
.file-label:hover {
  background-color: #e1e9f0;
}
small {
  color: #999;
  display: block;
  margin-top: 0.3rem;
}

.file-list {
  margin-top: 1rem;
  background: #fafafa;
  padding: 0.8rem;
  border-radius: 4px;
  border: 1px solid #eee;
}
.file-list p {
  margin: 0 0 0.5rem 0;
  font-size: 0.9rem;
  font-weight: bold;
  color: #555;
}
.file-list ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
.file-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.4rem 0;
  border-bottom: 1px solid #eee;
  font-size: 0.9rem;
}
.file-list li:last-child {
  border-bottom: none;
}
.btn-remove {
  background: none;
  border: none;
  color: #dc3545;
  font-size: 1.2rem;
  font-weight: bold;
  cursor: pointer;
  padding: 0 0.5rem;
}
.btn-remove:hover {
  color: #a71d2a;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 2rem;
}
.btn-submit {
  background-color: #003366;
  color: white;
  border: none;
  padding: 0.8rem 1.5rem;
  border-radius: 4px;
  font-weight: bold;
  cursor: pointer;
}
.btn-submit:disabled {
  background-color: #ccc;
}
.btn-cancel {
  background-color: white;
  border: 1px solid #ddd;
  color: #555;
  padding: 0.8rem 1.5rem;
  border-radius: 4px;
  cursor: pointer;
}
</style>
