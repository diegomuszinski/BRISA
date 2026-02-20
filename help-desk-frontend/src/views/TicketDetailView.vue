<script setup lang="ts">
import { ref, onMounted, computed, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useTicketStore, calculateSlaDeadline } from '@/stores/ticketStore'
import { formatDateTime } from '@/utils/formatters'
import { useToast } from 'vue-toastification'
import AppModal from '@/components/AppModal.vue'
import AssignTicketModal from '@/components/AssignTicketModal.vue'
import type { Anexo } from '@/types'
import { PencilSquareIcon } from '@heroicons/vue/24/outline'

const ticketStore = useTicketStore()
const route = useRoute()
const toast = useToast()
const ticket = computed(() => ticketStore.activeTicket)

const newComment = ref('')
const isSubmittingComment = ref(false)

const newAttachment = ref<File | null>(null)
const isCloseModalVisible = ref(false)
const solutionText = ref('')
const isReopenModalVisible = ref(false)
const reopenReason = ref('')
const isAssignModalVisible = ref(false)

const isEditCategoryModalVisible = ref(false)
const isEditPriorityModalVisible = ref(false)
const editCategory = ref('')
const editPriority = ref('')

// Helpers para abrir modais
function openEditCategoryModal() {
  if (ticket.value) {
    // Tenta ler do DTO ou do Objeto aninhado
    editCategory.value = ticket.value.category || ticket.value.categoria?.nome || ''
    if (ticketStore.categories.length === 0) ticketStore.fetchFormData()
    isEditCategoryModalVisible.value = true
  }
}

function openEditPriorityModal() {
  if (ticket.value) {
    editPriority.value = ticket.value.priority || ticket.value.prioridade || ''
    if (ticketStore.problems.length === 0) ticketStore.fetchFormData() // Busca prioridades via problemas ou fixo
    isEditPriorityModalVisible.value = true
  }
}

async function handleSaveCategory() {
  if (ticket.value) {
    try {
      await ticketStore.updateTicketClassification(
        ticket.value.id,
        editCategory.value,
        ticket.value.priority || ticket.value.prioridade || 'Média',
      )
      toast.success('Categoria atualizada!')
      isEditCategoryModalVisible.value = false
    } catch (error) {
      toast.error('Erro ao atualizar categoria.')
    }
  }
}

async function handleSavePriority() {
  if (ticket.value) {
    try {
      await ticketStore.updateTicketClassification(
        ticket.value.id,
        ticket.value.category || ticket.value.categoria?.nome || '',
        editPriority.value,
      )
      toast.success('Prioridade atualizada!')
      isEditPriorityModalVisible.value = false
    } catch (error) {
      toast.error('Erro ao atualizar prioridade.')
    }
  }
}

async function handleDownload(anexo: Anexo) {
  toast.info(`Iniciando download de: ${anexo.nomeArquivo}`)
  try {
    await ticketStore.downloadAnexo(anexo)
  } catch (error) {
    toast.error('Falha ao baixar o anexo.')
  }
}

function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  if (target.files && target.files.length > 0) newAttachment.value = target.files[0]
}

async function uploadFile() {
  if (!newAttachment.value || !ticket.value) return
  try {
    await ticketStore.uploadAttachment(ticket.value.id, newAttachment.value)
    toast.success('Arquivo anexado com sucesso!')
    newAttachment.value = null
    const input = document.getElementById('new-file-input') as HTMLInputElement
    if (input) input.value = ''
  } catch (error) {
    toast.error('Erro ao enviar arquivo.')
  }
}

// Contagem regressiva SLA
const MAX_CHARS = 1000
const countdown = ref('')
let countdownInterval: number | undefined = undefined

function startCountdown() {
  if (countdownInterval) clearInterval(countdownInterval)

  countdownInterval = window.setInterval(() => {
    if (!ticket.value) return

    // Se já estiver fechado
    if (['Encerrado', 'Fechado', 'Resolvido', 'Cancelado'].includes(ticket.value.status)) {
      const status = ticket.value.status || 'Finalizado'
      countdown.value = status.charAt(0).toUpperCase() + status.slice(1)
      if (countdownInterval) clearInterval(countdownInterval)
      return
    }

    // Calcula SLA
    // Tenta pegar data de abertura de openedAt ou dataAbertura
    const dataAbertura = ticket.value.openedAt || ticket.value.dataAbertura
    const prioridade = ticket.value.priority || ticket.value.prioridade

    if (!dataAbertura) {
      countdown.value = '...'
      return
    }

    const now = new Date().getTime()
    const deadline = calculateSlaDeadline(dataAbertura, prioridade).getTime()
    const diff = deadline - now

    if (diff <= 0) {
      countdown.value = `Atrasado`
      return
    }

    const days = Math.floor(diff / (1000 * 60 * 60 * 24))
    const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
    countdown.value = `${days}d ${hours}h ${minutes}m`
  }, 1000)
}

onMounted(() => {
  const ticketId = Number(route.params.id)
  if (ticketId) {
    ticketStore.fetchTicketById(ticketId).then(() => {
      if (ticket.value) startCountdown()
    })
  }
  // Carrega listas auxiliares se for admin/técnico
  const role = ticketStore.currentUser.role || ''
  if (['admin', 'manager', 'technician'].includes(role)) {
    ticketStore.fetchAnalysts()
    ticketStore.fetchFormData()
  }
})

onUnmounted(() => {
  if (countdownInterval) clearInterval(countdownInterval)
})

async function addComment() {
  if (newComment.value.trim() && ticket.value) {
    isSubmittingComment.value = true
    try {
      await ticketStore.addCommentToTicket(ticket.value.id, newComment.value.trim())
      newComment.value = ''
      toast.success('Observação adicionada!')
    } catch {
      toast.error('Falha ao adicionar observação.')
    } finally {
      isSubmittingComment.value = false
    }
  }
}

async function assignToMe() {
  if (ticket.value) {
    try {
      await ticketStore.assignTicketToSelf(ticket.value.id)
      toast.success('Chamado capturado!')
    } catch {
      toast.error('Falha ao capturar.')
    }
  }
}

async function handleAssignTicket(technicianId: number) {
  if (ticket.value) {
    try {
      await ticketStore.assignTicket(ticket.value.id, technicianId)
      toast.success('Atribuído!')
      isAssignModalVisible.value = false
    } catch {
      toast.error('Erro ao atribuir.')
    }
  }
}

async function handleCloseTicket() {
  if (ticket.value && solutionText.value.trim() !== '') {
    try {
      await ticketStore.closeTicket(ticket.value.id, solutionText.value.trim())
      toast.success('Encerrado!')
      isCloseModalVisible.value = false
    } catch {
      toast.error('Erro ao encerrar.')
    }
  } else {
    toast.error('Informe a solução.')
  }
}

function handleReopenTicket() {
  if (ticket.value) isReopenModalVisible.value = true
}

async function submitReopenTicket() {
  if (ticket.value && reopenReason.value.trim() !== '') {
    try {
      await ticketStore.reopenTicket(ticket.value.id, reopenReason.value.trim())
      toast.success('Reaberto!')
      isReopenModalVisible.value = false
    } catch {
      toast.error('Erro ao reabrir.')
    }
  } else {
    toast.error('Informe o motivo.')
  }
}
</script>

<template>
  <main class="detail-view">
    <div v-if="ticket" class="ticket-card">
      <header class="card-header">
        <div class="header-title">
          <h2>Detalhes do Chamado: #{{ ticket.numeroChamado }}</h2>
          <span class="status-badge">{{ ticket.status }}</span>
        </div>
        <div class="actions">
          <button
            v-if="
              ticket.status === 'Aberto' &&
              ['admin', 'manager'].includes(ticketStore.currentUser.role || '')
            "
            @click="isAssignModalVisible = true"
            class="action-btn assign-manager"
          >
            Atribuir
          </button>

          <button
            v-if="
              ticket.status === 'Aberto' &&
              ['admin', 'manager', 'technician'].includes(ticketStore.currentUser.role || '')
            "
            @click="assignToMe"
            class="action-btn assign"
          >
            Capturar
          </button>

          <button
            v-if="
              ticket.status === 'Em Andamento' &&
              (ticket.assignedTo === ticketStore.currentUser.name ||
                ticket.tecnicoAtribuido?.nome === ticketStore.currentUser.name)
            "
            @click="isCloseModalVisible = true"
            class="action-btn close"
          >
            Encerrar
          </button>

          <button
            v-if="['Resolvido', 'Encerrado', 'Fechado'].includes(ticket.status)"
            @click="handleReopenTicket"
            class="action-btn reopen"
          >
            Reabrir
          </button>
        </div>
      </header>

      <section class="card-body">
        <div class="detail-grid">
          <div class="detail-item">
            <strong>Solicitante:</strong>
            <span>{{ ticket.solicitante?.nome || ticket.user || 'N/D' }}</span>
          </div>

          <div class="detail-item">
            <strong>Aberto em:</strong>
            <span>{{ formatDateTime(ticket.dataAbertura || ticket.openedAt) }}</span>
          </div>

          <div class="detail-item">
            <strong>Atribuído a:</strong>
            <span>{{ ticket.tecnicoAtribuido?.nome || ticket.assignedTo || 'Não atribuído' }}</span>
          </div>

          <div class="detail-item">
            <strong>Categoria:</strong>
            <div class="editable-field">
              <span>{{ ticket.categoria?.nome || ticket.category || 'Sem Categoria' }}</span>

              <button
                v-if="
                  ['admin', 'manager', 'technician'].includes(ticketStore.currentUser.role || '') &&
                  ticket.status !== 'Fechado'
                "
                @click="openEditCategoryModal"
                class="edit-btn"
                title="Editar Categoria"
              >
                <PencilSquareIcon class="icon-sm" />
              </button>
            </div>
          </div>

          <div class="detail-item">
            <strong>Prioridade:</strong>
            <div class="editable-field">
              <span>{{ ticket.prioridade || ticket.priority || 'Média' }}</span>

              <button
                v-if="
                  ['admin', 'manager', 'technician'].includes(ticketStore.currentUser.role || '') &&
                  ticket.status !== 'Fechado'
                "
                @click="openEditPriorityModal"
                class="edit-btn"
                title="Editar Prioridade"
              >
                <PencilSquareIcon class="icon-sm" />
              </button>
            </div>
          </div>

          <div v-if="ticket.status !== 'Fechado'" class="detail-item">
            <strong>Prazo Final (SLA):</strong>
            <span>{{
              formatDateTime(
                calculateSlaDeadline(
                  ticket.dataAbertura || ticket.openedAt,
                  ticket.prioridade || ticket.priority,
                ),
              )
            }}</span>
            <span class="countdown"> ({{ countdown }})</span>
          </div>

          <div v-if="ticket.status === 'Fechado' && ticket.dataFechamento" class="detail-item">
            <strong>Chamado encerrado:</strong>
            <span style="color: #28a745; font-weight: bold">
              {{ formatDateTime(ticket.dataFechamento) }}
            </span>
          </div>
        </div>

        <div class="detail-item description">
          <strong>Descrição do Problema:</strong>
          <p>{{ ticket.descricao || ticket.description }}</p>
        </div>

        <div class="detail-item description" v-if="ticket.solucao || ticket.solution">
          <strong>Solução Aplicada:</strong>
          <p style="background-color: #d1e7dd; border-color: #badbcc">
            {{ ticket.solucao || ticket.solution }}
          </p>
        </div>
      </section>

      <section class="card-section attachments-section">
        <h3>Anexos</h3>
        <ul v-if="ticket.anexos && ticket.anexos.length > 0">
          <li v-for="anexo in ticket.anexos" :key="anexo.id">
            <button @click="handleDownload(anexo)" class="download-link">
              {{ anexo.nomeArquivo }}
            </button>
          </li>
        </ul>
        <p v-else style="color: #999; margin-bottom: 1rem">Nenhum anexo.</p>

        <div
          v-if="!['Resolvido', 'Fechado', 'Encerrado', 'Cancelado'].includes(ticket.status)"
          class="upload-area"
        >
          <label>Anexar novo arquivo:</label>
          <div class="upload-controls">
            <input type="file" id="new-file-input" @change="handleFileChange" />
            <button v-if="newAttachment" @click="uploadFile" class="btn-upload">Enviar</button>
          </div>
        </div>
      </section>

      <section class="card-section history-section">
        <h3>Histórico</h3>
        <div class="comment-list">
          <div
            v-for="(item, index) in ticket.historico || ticket.history"
            :key="index"
            class="comment-item"
          >
            <div class="comment-header">
              <strong class="author">{{ item.nomeUsuario || item.author || 'Sistema' }}</strong>
              <span class="date">{{ formatDateTime(item.dataHora || item.date) }}</span>
            </div>
            <p class="comment-body">{{ item.comentario || item.comment }}</p>
          </div>
        </div>
      </section>

      <section
        v-if="!['Resolvido', 'Fechado', 'Encerrado', 'Cancelado'].includes(ticket.status)"
        class="card-section comment-form-section"
      >
        <h3>Adicionar Observação</h3>
        <form @submit.prevent="addComment">
          <textarea
            v-model="newComment"
            rows="4"
            required
            :maxlength="MAX_CHARS"
            class="observation-input"
            placeholder="Digite aqui novos detalhes ou observações..."
          ></textarea>

          <div class="form-footer">
            <small
              class="char-counter"
              :class="{ 'near-limit': newComment.length > MAX_CHARS * 0.9 }"
            >
              {{ newComment.length }} / {{ MAX_CHARS }}
            </small>

            <button
              type="submit"
              class="btn-add-comment"
              :disabled="isSubmittingComment || !newComment.trim()"
            >
              {{ isSubmittingComment ? 'Enviando...' : 'Adicionar Observação' }}
            </button>
          </div>
        </form>
      </section>
    </div>
    <div v-else class="not-found"><h2>Carregando chamado...</h2></div>

    <AssignTicketModal
      v-if="isAssignModalVisible && ticket"
      :ticket-id="ticket.id"
      @close="isAssignModalVisible = false"
      @assigned="handleAssignTicket"
    />

    <AppModal v-if="isCloseModalVisible" @close="isCloseModalVisible = false">
      <div class="modal-content-wrapper">
        <h3>Encerrar Chamado</h3>
        <p class="modal-label">Descreva a solução:</p>
        <form @submit.prevent="handleCloseTicket">
          <textarea
            v-model="solutionText"
            rows="5"
            required
            :maxlength="MAX_CHARS"
            class="modal-textarea"
            placeholder="Descreva detalhadamente o que foi feito..."
          ></textarea>
          <div class="form-footer">
            <small
              class="char-counter"
              :class="{ 'near-limit': solutionText.length > MAX_CHARS * 0.9 }"
            >
              {{ solutionText.length }} / {{ MAX_CHARS }}
            </small>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn-secondary" @click="isCloseModalVisible = false">
              Cancelar
            </button>
            <button type="submit" class="btn-primary">Confirmar</button>
          </div>
        </form>
      </div>
    </AppModal>

    <AppModal v-if="isReopenModalVisible" @close="isReopenModalVisible = false">
      <div class="modal-content-wrapper">
        <h3>Reabrir Chamado</h3>
        <p class="modal-label">Motivo:</p>
        <form @submit.prevent="submitReopenTicket">
          <textarea
            v-model="reopenReason"
            rows="5"
            required
            :maxlength="MAX_CHARS"
            class="modal-textarea"
            placeholder="Informe o motivo da reabertura..."
          ></textarea>
          <div class="form-footer">
            <small
              class="char-counter"
              :class="{ 'near-limit': reopenReason.length > MAX_CHARS * 0.9 }"
            >
              {{ reopenReason.length }} / {{ MAX_CHARS }}
            </small>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn-secondary" @click="isReopenModalVisible = false">
              Cancelar
            </button>
            <button type="submit" class="btn-primary">Confirmar</button>
          </div>
        </form>
      </div>
    </AppModal>

    <AppModal v-if="isEditCategoryModalVisible" @close="isEditCategoryModalVisible = false">
      <div class="edit-ticket-modal">
        <h3>Alterar Categoria</h3>
        <form @submit.prevent="handleSaveCategory">
          <div style="margin-bottom: 1rem">
            <label>Nova Categoria:</label>
            <select v-model="editCategory" required style="width: 100%">
              <option v-for="c in ticketStore.categories" :key="c.id" :value="c.nome">
                {{ c.nome }}
              </option>
            </select>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn-secondary" @click="isEditCategoryModalVisible = false">
              Cancelar
            </button>
            <button type="submit" class="btn-primary">Salvar</button>
          </div>
        </form>
      </div>
    </AppModal>

    <AppModal v-if="isEditPriorityModalVisible" @close="isEditPriorityModalVisible = false">
      <div class="edit-ticket-modal">
        <h3>Alterar Prioridade</h3>
        <form @submit.prevent="handleSavePriority">
          <div style="margin-bottom: 1rem">
            <label>Nova Prioridade:</label>
            <select v-model="editPriority" required style="width: 100%">
              <option value="Baixa">Baixa</option>
              <option value="Média">Média</option>
              <option value="Elevada">Elevada</option>
              <option value="Crítica">Crítica</option>
            </select>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn-secondary" @click="isEditPriorityModalVisible = false">
              Cancelar
            </button>
            <button type="submit" class="btn-primary">Salvar</button>
          </div>
        </form>
      </div>
    </AppModal>
  </main>
</template>

<style scoped>
.download-link {
  background: none;
  border: none;
  padding: 0;
  color: #003366;
  text-decoration: underline;
  cursor: pointer;
}
.detail-view {
  padding: 2rem;
  background-color: #f4f7f6;
  display: flex;
  justify-content: center;
}
.ticket-card {
  width: 100%;
  max-width: 900px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  background-color: #f8f9fa;
  border-bottom: 1px solid #ddd;
  flex-wrap: wrap;
  gap: 1rem;
}
.header-title h2 {
  margin: 0;
  font-size: 1.5rem;
  color: #2c3e50;
}
.status-badge {
  padding: 0.4rem 0.8rem;
  border-radius: 15px;
  background: #003366;
  color: white;
  font-weight: 500;
}
.card-body {
  padding: 1.5rem;
}
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.5rem;
  margin-bottom: 1.5rem;
}
.detail-item strong {
  display: block;
  margin-bottom: 0.25rem;
  color: #555;
}
.detail-item.description {
  grid-column: 1 / -1;
}
.detail-item.description p {
  margin: 0;
  padding: 1rem;
  background-color: #f9f9f9;
  border: 1px solid #eee;
  border-radius: 4px;
  white-space: pre-wrap;
}
.card-section {
  padding: 1rem 1.5rem;
  border-top: 1px solid #ddd;
}
.comment-list {
  max-height: 400px;
  overflow-y: auto;
}
.comment-item {
  margin-bottom: 1rem;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 1rem;
}
.comment-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.5rem;
  font-size: 0.9rem;
}
.actions {
  display: flex;
  gap: 0.5rem;
}
.action-btn {
  padding: 0.6rem 1rem;
  border: none;
  border-radius: 4px;
  color: white;
  cursor: pointer;
  transition: opacity 0.2s;
}
.action-btn:hover {
  opacity: 0.85;
}
.assign-manager {
  background-color: #ffc107;
  color: #333;
}
.assign {
  background-color: #28a745;
}
.close {
  background-color: #dc3545;
}
.reopen {
  background-color: #17a2b8;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 1.5rem;
}
.modal-actions button {
  padding: 0.8rem 1.5rem;
  border: none;
  border-radius: 4px;
  font-weight: 600;
  cursor: pointer;
}
.btn-primary {
  background-color: #003366;
  color: white;
}
.btn-secondary {
  background-color: #f8f9fa;
  color: #333;
  border: 1px solid #ced4da;
}
.upload-area {
  margin-top: 1rem;
  padding: 1rem;
  background-color: #f8f9fa;
  border: 1px dashed #ccc;
  border-radius: 4px;
}
.upload-controls {
  display: flex;
  align-items: center;
  gap: 1rem;
}
.btn-upload {
  background-color: #198754;
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  cursor: pointer;
}
.editable-field {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.edit-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: #0d6efd;
  padding: 2px;
}
.icon-sm {
  width: 18px;
  height: 18px;
}
.observation-input {
  width: 100%;
  padding: 1rem;
  border: 1px solid #dee2e6;
  border-radius: 6px;
  resize: vertical;
  font-family: inherit;
  font-size: 0.95rem;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}
.observation-input:focus {
  border-color: #007bff;
  box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.1);
  outline: none;
}
.form-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 0.75rem;
}
.char-counter {
  color: #6c757d;
  font-size: 0.85rem;
}
.char-counter.near-limit {
  color: #dc3545;
  font-weight: bold;
}
.btn-add-comment {
  padding: 0.75rem 1.5rem;
  font-size: 0.95rem;
  background-color: #003366;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  transition:
    background-color 0.2s,
    transform 0.1s;
}
.btn-add-comment:hover:not(:disabled) {
  background-color: #002244;
}
.btn-add-comment:disabled {
  background-color: #a0c4ff;
  cursor: not-allowed;
}
.modal-content-wrapper h3 {
  margin-top: 0;
  margin-bottom: 1rem;
  color: #2c3e50;
  font-size: 1.4rem;
}
.modal-label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #555;
}
.modal-textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-family: inherit;
  font-size: 0.95rem;
  resize: vertical;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}
.modal-textarea:focus {
  border-color: #007bff;
  outline: none;
  box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.1);
}
</style>
