<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useToast } from 'vue-toastification'
import { PencilSquareIcon, TrashIcon, XMarkIcon } from '@heroicons/vue/24/outline'
import api from '@/services/api'

const toast = useToast()

const equipes = ref<{ id: number; nomeEquipe: string }[]>([])
const nomeEquipe = ref('')
const isEditing = ref(false)
const editingId = ref<number | null>(null)

// Controles do Modal
const showDeleteModal = ref(false)
const equipeToDelete = ref<any>(null)

onMounted(async () => {
  await fetchEquipes()
})

async function fetchEquipes() {
  try {
    const res = await api.get('/api/equipes')
    equipes.value = res.data
  } catch (error) {
    console.error('Erro ao buscar equipes', error)
  }
}

function handleEdit(eq: { id: number; nomeEquipe: string }) {
  isEditing.value = true
  editingId.value = eq.id
  nomeEquipe.value = eq.nomeEquipe
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function cancelEdit() {
  isEditing.value = false
  editingId.value = null
  nomeEquipe.value = ''
}

// --- MODAL LÓGICA ---
function confirmDelete(eq: any) {
  equipeToDelete.value = eq
  showDeleteModal.value = true
}

async function executeDelete() {
  if (!equipeToDelete.value) return
  try {
    await api.delete(`/api/equipes/${equipeToDelete.value.id}`)
    toast.success('Equipe excluída!')
    await fetchEquipes()
  } catch (error) {
    toast.error('Erro ao excluir equipe. Verifique se há usuários vinculados.')
  } finally {
    showDeleteModal.value = false
    equipeToDelete.value = null
  }
}
// -------------------

async function handleSave() {
  if (!nomeEquipe.value.trim()) {
    toast.warning('Digite o nome da equipe.')
    return
  }

  try {
    if (isEditing.value && editingId.value) {
      await api.put(`/api/equipes/${editingId.value}`, { nomeEquipe: nomeEquipe.value })
      toast.success('Equipe atualizada!')
    } else {
      await api.post('/api/equipes', { nomeEquipe: nomeEquipe.value })
      toast.success('Equipe cadastrada!')
    }
    cancelEdit()
    await fetchEquipes()
  } catch (error: any) {
    let msg = error.response?.data || 'Erro ao salvar equipe.'
    toast.error(msg)
  }
}
</script>

<template>
  <div class="admin-view">
    <header class="view-header">
      <h1>Gerenciamento de Equipes</h1>
      <p>Cadastre as áreas responsáveis (ex: Suporte N1, Infraestrutura).</p>
    </header>

    <div class="content-container">
      <div class="card form-card">
        <h2>{{ isEditing ? 'Editar Equipe' : 'Nova Equipe' }}</h2>
        <form @submit.prevent="handleSave">
          <div class="form-group">
            <label>Nome da Área/Equipe</label>
            <div class="input-wrapper">
              <input type="text" v-model="nomeEquipe" placeholder="Ex: Suporte Técnico" required />
              <button v-if="isEditing" type="button" class="btn-icon-cancel" @click="cancelEdit">
                <XMarkIcon class="icon" />
              </button>
            </div>
          </div>
          <button type="submit" class="btn-save">
            {{ isEditing ? 'Atualizar' : 'Cadastrar' }}
          </button>
        </form>
      </div>

      <div class="card list-card">
        <h2>Equipes Cadastradas</h2>
        <table>
          <thead>
            <tr>
              <th>Nome</th>
              <th style="width: 100px; text-align: right">Ações</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="eq in equipes" :key="eq.id">
              <td>{{ eq.nomeEquipe }}</td>
              <td class="actions-cell">
                <button @click="handleEdit(eq)" class="icon-btn edit" title="Editar">
                  <PencilSquareIcon class="icon" />
                </button>
                <button @click="confirmDelete(eq)" class="icon-btn delete" title="Excluir">
                  <TrashIcon class="icon" />
                </button>
              </td>
            </tr>
            <tr v-if="equipes.length === 0">
              <td colspan="2" class="empty-msg">Nenhuma equipe cadastrada.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="showDeleteModal" class="modal-overlay">
      <div class="modal-content">
        <h3>Excluir Equipe</h3>
        <p>
          Tem certeza que deseja excluir <strong>{{ equipeToDelete?.nomeEquipe }}</strong
          >?
        </p>
        <div class="modal-actions">
          <button class="btn-cancel" @click="showDeleteModal = false">Cancelar</button>
          <button class="btn-danger" @click="executeDelete">Excluir</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-view {
  padding: 2rem;
  max-width: 1000px;
  margin: 0 auto;
}
.view-header {
  margin-bottom: 2rem;
}
.view-header h1 {
  color: #2c3e50;
  font-size: 1.8rem;
  margin: 0;
}
.view-header p {
  color: #666;
  margin-top: 0.5rem;
}

.content-container {
  display: grid;
  grid-template-columns: 1fr 1.5fr;
  gap: 2rem;
  align-items: start;
}
.card {
  background: #fff;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
}
.card h2 {
  margin-top: 0;
  border-bottom: 1px solid #eee;
  padding-bottom: 1rem;
  margin-bottom: 1rem;
  font-size: 1.2rem;
  color: #2c3e50;
}

.form-group label {
  display: block;
  font-weight: 500;
  margin-bottom: 0.5rem;
  color: #34495e;
}
.input-wrapper {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.form-group input {
  width: 100%;
  padding: 0.8rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 0.95rem;
}

.btn-save {
  width: 100%;
  padding: 0.8rem;
  background-color: #003366;
  color: white;
  border: none;
  border-radius: 6px;
  font-weight: bold;
  cursor: pointer;
  margin-top: 1rem;
  transition: background 0.2s;
}
.btn-save:hover {
  background-color: #002244;
}
.btn-icon-cancel {
  background: none;
  border: none;
  cursor: pointer;
  color: #666;
  padding: 4px;
}

table {
  width: 100%;
  border-collapse: collapse;
}
th,
td {
  padding: 0.8rem;
  text-align: left;
  border-bottom: 1px solid #eee;
}
th {
  font-weight: 600;
  color: #555;
  font-size: 0.9rem;
}
.actions-cell {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}
.icon-btn {
  border: none;
  background: none;
  cursor: pointer;
  padding: 4px;
  transition: transform 0.1s;
}
.icon-btn:hover {
  transform: scale(1.1);
}
.icon-btn.edit {
  color: #0d6efd;
}
.icon-btn.delete {
  color: #dc3545;
}
.icon {
  width: 18px;
  height: 18px;
}
.empty-msg {
  text-align: center;
  color: #999;
  padding: 1.5rem;
  font-style: italic;
}

/* MODAL */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}
.modal-content {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  width: 350px;
  text-align: center;
}
.modal-content h3 {
  color: #dc3545;
  margin-top: 0;
}
.modal-actions {
  display: flex;
  justify-content: center;
  gap: 1rem;
  margin-top: 1.5rem;
}
.btn-cancel {
  background: #eee;
  border: none;
  padding: 0.6rem 1rem;
  border-radius: 4px;
  cursor: pointer;
}
.btn-danger {
  background: #dc3545;
  color: white;
  border: none;
  padding: 0.6rem 1rem;
  border-radius: 4px;
  cursor: pointer;
}
</style>
