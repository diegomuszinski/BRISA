<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '@/services/api'
import { useToast } from 'vue-toastification'
import { TrashIcon, PencilSquareIcon, XMarkIcon } from '@heroicons/vue/24/outline'

const toast = useToast()

interface BaseItem {
  id: number
  nome: string
}
interface Problema extends BaseItem {
  prioridadePadrao: string
}

const problemas = ref<Problema[]>([])
const categorias = ref<BaseItem[]>([])

// Inputs do Formulário
const formCategoria = ref('')
const formProblema = ref('')
const formPrioridade = ref('Média')

// --- CONTROLE DE EDIÇÃO ---
const editingCategory = ref<BaseItem | null>(null)
const editingProblem = ref<Problema | null>(null)

// --- CONTROLE DOS MODAIS DE EXCLUSÃO ---
const showDeleteProb = ref(false)
const showDeleteCat = ref(false)
const itemToDelete = ref<any>(null)

onMounted(() => {
  fetchProblemas()
  fetchCategorias()
})

async function fetchProblemas() {
  try {
    const res = await api.get('/api/problemas')
    problemas.value = res.data
  } catch (e) {
    console.error(e)
  }
}

async function fetchCategorias() {
  try {
    const res = await api.get('/api/categorias')
    categorias.value = res.data
  } catch (e) {
    console.error(e)
  }
}

// =======================
// LÓGICA DE CATEGORIAS
// =======================
function startEditCategory(cat: BaseItem) {
  editingCategory.value = cat
  formCategoria.value = cat.nome
}

function cancelEditCategory() {
  editingCategory.value = null
  formCategoria.value = ''
}

async function saveCategory() {
  if (!formCategoria.value.trim()) return toast.warning('Nome obrigatório.')

  try {
    if (editingCategory.value) {
      // EDITAR
      await api.put(`/api/categorias/${editingCategory.value.id}`, { nome: formCategoria.value })
      toast.success('Categoria atualizada!')
    } else {
      // CRIAR
      await api.post('/api/categorias', { nome: formCategoria.value })
      toast.success('Categoria criada!')
    }
    cancelEditCategory() // Limpa tudo
    fetchCategorias()
  } catch (e) {
    toast.error('Erro ao salvar categoria.')
  }
}

// =======================
// LÓGICA DE PROBLEMAS
// =======================
function startEditProblem(prob: Problema) {
  editingProblem.value = prob
  formProblema.value = prob.nome
  formPrioridade.value = prob.prioridadePadrao
}

function cancelEditProblem() {
  editingProblem.value = null
  formProblema.value = ''
  formPrioridade.value = 'Média'
}

async function saveProblem() {
  if (!formProblema.value.trim()) return toast.warning('Nome obrigatório.')

  const payload = { nome: formProblema.value, prioridadePadrao: formPrioridade.value }

  try {
    if (editingProblem.value) {
      // EDITAR
      await api.put(`/api/problemas/${editingProblem.value.id}`, payload)
      toast.success('Problema atualizado!')
    } else {
      // CRIAR
      await api.post('/api/problemas', payload)
      toast.success('Problema criado!')
    }
    cancelEditProblem() // Limpa tudo
    fetchProblemas()
  } catch (e) {
    toast.error('Erro ao salvar problema.')
  }
}

// =======================
// LÓGICA DE EXCLUSÃO (MODAIS)
// =======================
function confirmDeleteCat(item: any) {
  itemToDelete.value = item
  showDeleteCat.value = true
}
function confirmDeleteProb(item: any) {
  itemToDelete.value = item
  showDeleteProb.value = true
}

async function deleteItem(type: 'problema' | 'categoria') {
  if (!itemToDelete.value) return
  const endpoint = type === 'problema' ? 'problemas' : 'categorias'

  try {
    await api.delete(`/api/${endpoint}/${itemToDelete.value.id}`)
    toast.success('Excluído com sucesso!')
    if (type === 'problema') fetchProblemas()
    else fetchCategorias()
  } catch (e) {
    toast.error('Erro ao excluir.')
  } finally {
    showDeleteProb.value = false
    showDeleteCat.value = false
    itemToDelete.value = null
  }
}
</script>

<template>
  <div class="manage-view">
    <h1>Gerenciamento do Sistema</h1>

    <div class="grid-container">
      <div class="card">
        <div class="card-header">
          <h2>Categorias</h2>
          <button v-if="editingCategory" @click="cancelEditCategory" class="btn-cancel-top">
            Cancelar Edição
          </button>
        </div>

        <form @submit.prevent="saveCategory" class="form-row">
          <input
            v-model="formCategoria"
            :placeholder="editingCategory ? 'Editando nome...' : 'Nova Categoria'"
            required
          />
          <button type="submit" class="btn-add">
            {{ editingCategory ? 'Atualizar' : 'Adicionar' }}
          </button>
        </form>

        <ul class="list">
          <li v-for="c in categorias" :key="c.id">
            {{ c.nome }}
            <div class="actions">
              <button @click="startEditCategory(c)" class="btn-icon edit" title="Editar">
                <PencilSquareIcon class="icon" />
              </button>
              <button @click="confirmDeleteCat(c)" class="btn-icon delete" title="Excluir">
                <TrashIcon class="icon" />
              </button>
            </div>
          </li>
        </ul>
      </div>

      <div class="card">
        <div class="card-header">
          <h2>Problemas e SLA</h2>
          <button v-if="editingProblem" @click="cancelEditProblem" class="btn-cancel-top">
            Cancelar Edição
          </button>
        </div>

        <form @submit.prevent="saveProblem" class="form-col">
          <input
            v-model="formProblema"
            :placeholder="editingProblem ? 'Editando problema...' : 'Nome do Problema'"
            required
          />
          <div class="row-inputs">
            <select v-model="formPrioridade">
              <option>Baixa</option>
              <option>Média</option>
              <option>Alta</option>
              <option>Crítica</option>
            </select>
            <button type="submit" class="btn-add">
              {{ editingProblem ? 'Atualizar' : 'Adicionar' }}
            </button>
          </div>
        </form>

        <ul class="list">
          <li v-for="p in problemas" :key="p.id">
            <span>
              <strong>{{ p.nome }}</strong>
              <span :class="['badge', p.prioridadePadrao.toLowerCase()]">{{
                p.prioridadePadrao
              }}</span>
            </span>
            <div class="actions">
              <button @click="startEditProblem(p)" class="btn-icon edit" title="Editar">
                <PencilSquareIcon class="icon" />
              </button>
              <button @click="confirmDeleteProb(p)" class="btn-icon delete" title="Excluir">
                <TrashIcon class="icon" />
              </button>
            </div>
          </li>
        </ul>
      </div>
    </div>

    <div v-if="showDeleteCat" class="modal-overlay">
      <div class="modal-content delete-modal">
        <h3>Excluir Categoria</h3>
        <p>
          Confirma a exclusão de <strong>{{ itemToDelete?.nome }}</strong
          >?
        </p>
        <div class="modal-actions">
          <button class="btn-cancel" @click="showDeleteCat = false">Cancelar</button>
          <button class="btn-danger" @click="deleteItem('categoria')">Sim, Excluir</button>
        </div>
      </div>
    </div>

    <div v-if="showDeleteProb" class="modal-overlay">
      <div class="modal-content delete-modal">
        <h3>Excluir Problema</h3>
        <p>
          Confirma a exclusão de <strong>{{ itemToDelete?.nome }}</strong
          >?
        </p>
        <div class="modal-actions">
          <button class="btn-cancel" @click="showDeleteProb = false">Cancelar</button>
          <button class="btn-danger" @click="deleteItem('problema')">Sim, Excluir</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.manage-view {
  padding: 2rem;
  max-width: 1000px;
  margin: 0 auto;
}
.grid-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2rem;
}
.card {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #eee;
  padding-bottom: 0.5rem;
  margin-bottom: 1rem;
}
h2 {
  margin: 0;
  color: #003366;
  font-size: 1.2rem;
}
.btn-cancel-top {
  background: #eee;
  border: none;
  padding: 0.3rem 0.8rem;
  border-radius: 4px;
  font-size: 0.8rem;
  cursor: pointer;
  color: #666;
}
.btn-cancel-top:hover {
  background: #ddd;
  color: #333;
}

.form-row {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}
.form-col {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1rem;
}
.row-inputs {
  display: flex;
  gap: 0.5rem;
}

input,
select {
  flex: 1;
  padding: 0.6rem;
  border: 1px solid #ccc;
  border-radius: 4px;
}
.btn-add {
  background: #007bff;
  color: white;
  border: none;
  padding: 0.6rem 1rem;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  min-width: 100px;
}
.btn-add:hover {
  background: #0056b3;
}

.list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.8rem;
  border-bottom: 1px solid #eee;
}

.actions {
  display: flex;
  gap: 0.5rem;
}
.btn-icon {
  background: none;
  border: none;
  cursor: pointer;
  padding: 2px;
}
.btn-icon.edit {
  color: #007bff;
}
.btn-icon.delete {
  color: #dc3545;
}
.icon {
  width: 18px;
  height: 18px;
}

.badge {
  margin-left: 0.5rem;
  padding: 0.2rem 0.5rem;
  border-radius: 10px;
  font-size: 0.75rem;
  color: white;
}
.badge.baixa {
  background: #28a745;
}
.badge.média,
.badge.media {
  background: #ffc107;
  color: #333;
}
.badge.alta {
  background: #fd7e14;
}
.badge.crítica,
.badge.critica {
  background: #dc3545;
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
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
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
  font-weight: bold;
}
.btn-danger {
  background: #dc3545;
  color: white;
  border: none;
  padding: 0.6rem 1rem;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
}
</style>
