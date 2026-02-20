<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useTicketStore } from '@/stores/ticketStore'
import { useToast } from 'vue-toastification'
import { PencilSquareIcon, TrashIcon, XMarkIcon } from '@heroicons/vue/24/outline'

const ticketStore = useTicketStore()
const toast = useToast()

const newCategory = ref('')
const newProblem = ref('')
const newProblemPriority = ref('')

// Controle de Edição
const editingCategory = ref<any>(null)
const editingProblem = ref<any>(null)

onMounted(() => {
  ticketStore.fetchFormData()
})

// --- CATEGORIAS ---
async function handleSaveCategory() {
  if (!newCategory.value) return toast.warning('Digite o nome da categoria')
  try {
    if (editingCategory.value) {
      await ticketStore.updateCategory(editingCategory.value.id, newCategory.value)
      toast.success('Categoria atualizada!')
      editingCategory.value = null
    } else {
      await ticketStore.createCategory(newCategory.value)
      toast.success('Categoria criada!')
    }
    newCategory.value = ''
  } catch (e) {
    toast.error('Erro ao salvar categoria')
  }
}

function startEditCategory(cat: any) {
  editingCategory.value = cat
  newCategory.value = cat.nome
}

function cancelEditCategory() {
  editingCategory.value = null
  newCategory.value = ''
}

async function deleteCategory(id: number) {
  if (confirm('Excluir esta categoria?')) {
    try {
      await ticketStore.deleteCategory(id)
      toast.success('Excluído!')
    } catch (e) {
      toast.error('Erro ao excluir')
    }
  }
}

// --- PROBLEMAS ---
async function handleSaveProblem() {
  if (!newProblem.value) return toast.warning('Digite o nome do problema')
  try {
    if (editingProblem.value) {
      await ticketStore.updateProblem(
        editingProblem.value.id,
        newProblem.value,
        newProblemPriority.value,
      )
      toast.success('Problema atualizado!')
      editingProblem.value = null
    } else {
      await ticketStore.createProblem(newProblem.value, newProblemPriority.value)
      toast.success('Problema criado!')
    }
    newProblem.value = ''
    newProblemPriority.value = ''
  } catch (e) {
    toast.error('Erro ao salvar problema')
  }
}

function startEditProblem(prob: any) {
  editingProblem.value = prob
  newProblem.value = prob.nome
  newProblemPriority.value = prob.prioridadePadrao
}

function cancelEditProblem() {
  editingProblem.value = null
  newProblem.value = ''
  newProblemPriority.value = ''
}

async function deleteProblem(id: number) {
  if (confirm('Excluir este problema?')) {
    try {
      await ticketStore.deleteProblem(id)
      toast.success('Excluído!')
    } catch (e) {
      toast.error('Erro ao excluir')
    }
  }
}
</script>

<template>
  <div class="management-container">
    <div class="header">
      <h1>Gerenciamento do Sistema</h1>
    </div>

    <div class="panels">
      <div class="panel">
        <h2>Categorias</h2>
        <div class="form-row">
          <input
            v-model="newCategory"
            :placeholder="editingCategory ? 'Editando...' : 'Nova Categoria'"
          />
          <button class="btn-add" @click="handleSaveCategory">
            {{ editingCategory ? 'Atualizar' : 'Adicionar' }}
          </button>
          <button v-if="editingCategory" class="btn-cancel-icon" @click="cancelEditCategory">
            <XMarkIcon class="icon" />
          </button>
        </div>
        <ul class="list">
          <li v-for="cat in ticketStore.categories" :key="cat.id">
            <span>{{ cat.nome }}</span>
            <div class="actions">
              <PencilSquareIcon class="icon edit" @click="startEditCategory(cat)" />
              <TrashIcon class="icon delete" @click="deleteCategory(cat.id)" />
            </div>
          </li>
          <li v-if="ticketStore.categories.length === 0" style="color: #999">
            Nenhuma categoria cadastrada.
          </li>
        </ul>
      </div>

      <div class="panel">
        <h2>Problemas e SLA</h2>
        <div class="form-stack">
          <input
            v-model="newProblem"
            :placeholder="editingProblem ? 'Editando Problema...' : 'Nome do Problema'"
          />
          <select v-model="newProblemPriority">
            <option value="">Selecione Prioridade (Padrão: Média)</option>
            <option value="Baixa">Baixa</option>
            <option value="Média">Média</option>
            <option value="Elevada">Elevada</option>
            <option value="Crítica">Crítica</option>
          </select>
          <div style="display: flex; gap: 0.5rem">
            <button class="btn-add full" @click="handleSaveProblem">
              {{ editingProblem ? 'Atualizar Problema' : 'Adicionar Problema' }}
            </button>
            <button v-if="editingProblem" class="btn-cancel" @click="cancelEditProblem">
              Cancelar
            </button>
          </div>
        </div>
        <ul class="list">
          <li v-for="prob in ticketStore.problems" :key="prob.id" class="prob-item">
            <div>
              <strong>{{ prob.nome }}</strong>
              <span :class="['badge', prob.prioridadePadrao]">{{ prob.prioridadePadrao }}</span>
            </div>
            <div class="actions">
              <PencilSquareIcon class="icon edit" @click="startEditProblem(prob)" />
              <TrashIcon class="icon delete" @click="deleteProblem(prob.id)" />
            </div>
          </li>
          <li v-if="ticketStore.problems.length === 0" style="color: #999">
            Nenhum problema cadastrado.
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<style scoped>
.management-container {
  padding: 2rem;
  max-width: 1100px;
  margin: 0 auto;
}
.panels {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 2rem;
}
.panel {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}
.form-row {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}
.form-stack {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
  margin-bottom: 1.5rem;
}
input,
select {
  padding: 0.6rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  flex: 1;
}
.btn-add {
  background: #003366;
  color: white;
  border: none;
  padding: 0.6rem 1rem;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
}
.btn-cancel {
  background: #ddd;
  color: #333;
  border: none;
  padding: 0.6rem;
  border-radius: 4px;
  cursor: pointer;
  width: 100%;
}
.btn-cancel-icon {
  background: none;
  border: none;
  cursor: pointer;
}
.list {
  list-style: none;
  padding: 0;
  margin-top: 1rem;
  border-top: 1px solid #eee;
}
.list li {
  padding: 0.8rem 0;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.actions {
  display: flex;
  gap: 0.5rem;
}
.icon {
  width: 18px;
  height: 18px;
  cursor: pointer;
}
.icon.edit {
  color: #0d6efd;
}
.icon.delete {
  color: #dc3545;
}
.badge {
  padding: 0.2rem 0.5rem;
  font-size: 0.75rem;
  border-radius: 4px;
  color: white;
  margin-left: 0.5rem;
}
.badge.Baixa {
  background: #198754;
}
.badge.Média {
  background: #ffc107;
  color: #333;
}
.badge.Elevada {
  background: #dc3545;
}
.badge.Crítica {
  background: #000;
}
.full {
  width: 100%;
}
</style>
